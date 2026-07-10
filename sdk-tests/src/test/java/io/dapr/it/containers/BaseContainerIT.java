/*
 * Copyright 2025 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.it.containers;

import io.dapr.actors.client.ActorClient;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.config.Properties;
import io.dapr.config.Property;
import io.dapr.it.AppRun;
import io.dapr.it.DaprPorts;
import io.dapr.it.Stoppable;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import io.dapr.testcontainers.wait.strategy.DaprWait;
import org.junit.jupiter.api.AfterAll;
import org.testcontainers.Testcontainers;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Base class for sdk-tests integration tests that run Dapr inside a
 * Testcontainer rather than via the local {@code dapr run} CLI.
 *
 * <p>Each subclass owns its own {@code private static DaprContainer dapr}
 * (and optionally {@code AppRun app}) field. This class holds no
 * Dapr/App fields itself — it only provides helpers and {@code @AfterAll}
 * cleanup hooks.
 *
 * <p>Lifecycle (per IT class):
 * <ol>
 *   <li>{@code @BeforeAll}: call {@link #startAppAndAttach} (if needed), then build
 *       the DaprContainer via {@link #daprBuilder}, start it, and call
 *       {@link #deferStop(org.testcontainers.containers.GenericContainer)}.</li>
 *   <li>{@code @AfterAll}: inherited cleanup drains deferStop (LIFO) then
 *       deferClose.</li>
 * </ol>
 */
public abstract class BaseContainerIT {

  /** Pinned Dapr runtime image. Matches the testcontainers-dapr library default. */
  protected static final String DAPR_IMAGE = io.dapr.testcontainers.DaprContainerConstants.DAPR_RUNTIME_IMAGE_TAG;

  protected static final String STATE_STORE_NAME = "statestore";
  protected static final String PUBSUB_NAME = "messagebus";
  protected static final String CONFIG_STORE_NAME = "redisconfigstore";
  protected static final String MONGO_QUERY_STATE_STORE_NAME = "mongo-statestore";

  // JUnit Jupiter runs @BeforeAll/@AfterAll single-threaded per class, so no synchronization needed.
  private static final Deque<Stoppable> TO_BE_STOPPED = new LinkedList<>();
  private static final Deque<AutoCloseable> TO_BE_CLOSED = new LinkedList<>();

  // ---------- DaprContainer builder ----------

  /**
   * Returns a pre-configured {@link DaprContainer} wired into the shared
   * Network and Redis. Callers add components and (optionally) an app port
   * before calling {@code .start()}.
   */
  protected static DaprContainer daprBuilder(String appName) {
    SharedTestInfra.redis();   // ensure Redis is up before DaprContainer needs it
    return new DaprContainer(DAPR_IMAGE)
        .withAppName(appName)
        .withNetwork(SharedTestInfra.network())
        .withDaprLogLevel(DaprLogLevel.DEBUG)
        // Stream daprd logs to stdout so CI surfaces app-discovery and component-load
        // errors. Without this, the container's stdout is consumed by Testcontainers
        // and we have no insight when actor registration or component init fails.
        .withLogConsumer(frame -> System.out.print("[daprd] " + frame.getUtf8String()))
        // Reuses the placement sidecar container within this JVM (Testcontainers manages it);
        // orthogonal to SharedTestInfra's Redis `withReuse(true)`.
        .withReusablePlacement(true);
  }

  // ---------- App lifecycle ----------

  /** Pair returned by {@link #startAppAndAttach}. */
  public record DaprAndApp(DaprContainer dapr, AppRun app) {}

  /**
   * Two-phase startup for ITs that need an app callback. Allocates the app
   * port, exposes it to Testcontainers, starts the AppRun subprocess so it
   * has bound the host port, then lets the caller build the
   * DaprContainer. Binds daprd to the pre-allocated host ports so the
   * already-running app's {@code DAPR_HTTP_PORT} / {@code DAPR_GRPC_PORT}
   * env vars (used by callbacks like {@code registerActorTimer}) point at
   * a reachable daprd. Returns both. Both are registered for
   * {@code @AfterAll} cleanup via {@link #deferStop}.
   *
   * <p>Order matters: starting daprd before the app causes daprd's
   * application-channel probe to succeed against the Testcontainers SSH
   * bridge before the JVM has actually bound the host port. Daprd then
   * fetches {@code /dapr/config}, gets nothing, reports actor types {@code []}
   * to placement, and never re-queries — so actor ITs hang at
   * {@code waitForActorsReady}. Starting the app first avoids the race.
   *
   * @param appName       used both as the Dapr app id and the AppRun name
   * @param serviceClass  the class whose {@code main(String[])} the subprocess runs
   * @param protocol      reserved for future use; AppRun currently ignores it
   * @param daprFactory   given the allocated app port, returns an UNSTARTED
   *                      DaprContainer (factory body builds the container,
   *                      calls {@code .withAppPort(appPort)
   *                      .withAppChannelAddress("host.testcontainers.internal")}
   *                      and other configuration, then returns it WITHOUT
   *                      calling {@code .start()} — BaseContainerIT pins the
   *                      daprd HTTP/gRPC host ports and starts the container).
   */
  protected static DaprAndApp startAppAndAttach(
      String appName,
      Class<?> serviceClass,
      AppRun.AppProtocol protocol,
      java.util.function.IntFunction<DaprContainer> daprFactory) throws Exception {
    DaprPorts ports = DaprPorts.build(true, true, true);
    int appPort = ports.getAppPort();
    int daprHttpPort = ports.getHttpPort();
    int daprGrpcPort = ports.getGrpcPort();

    // Wire the SSH bridge before either side starts so daprd can resolve
    // host.testcontainers.internal:appPort the moment its container boots.
    Testcontainers.exposeHostPorts(appPort);

    // Start the app subprocess BEFORE daprd. AppRun.start() blocks on
    // assertListeningOnPort, so by the time it returns the JVM has bound
    // appPort and /dapr/config will respond with the registered actor types.
    // DAPR_HTTP_PORT/DAPR_GRPC_PORT point at the pre-allocated host ports
    // we will pin daprd to below; this is what app-side callbacks like
    // registerActorTimer use to dial back to the sidecar.
    AppRun app = new AppRun(
        ports,
        // Empty success-message: the legacy "dapr initialized. Status: Running" string is
        // emitted by daprd's stdout, which used to be merged into the subprocess output by
        // the dapr CLI but is now isolated in the Docker container. Pass "" so Command.run()
        // returns on Maven's first stdout line; AppRun.start() then waits for the app to
        // actually bind its port via assertListeningOnPort, which is the real readiness
        // signal in the containerized world.
        "",
        serviceClass,
        60_000,
        daprHttpPort,
        daprGrpcPort);
    app.start();
    deferStop(app);

    DaprContainer dapr = daprFactory.apply(appPort);
    // Pin daprd's host ports so they match the values the AppRun's env was
    // already given. Must be done before .start().
    dapr.setPortBindings(java.util.List.of(
        daprHttpPort + ":3500",
        daprGrpcPort + ":50001"
    ));
    dapr.start();
    deferStop(dapr);

    // Daprd's HTTP healthz/outbound (the wait strategy on DaprContainer) returns 2xx as
    // soon as outbound connections are ready, but its gRPC server can be a beat behind.
    // Tests that use the gRPC channel (method-invoke gRPC, tracing) hit "error reading
    // server preface: EOF" if they call too soon. Prove the gRPC channel is responsive
    // by issuing a waitForSidecar against a fresh DaprClient before returning.
    try (DaprClient client = newDaprClient(dapr)) {
      client.waitForSidecar(30_000).block();
    }
    return new DaprAndApp(dapr, app);
  }

  /**
   * Polls daprd's metadata endpoint until at least one actor is registered. Call from
   * {@code @BeforeAll} of actor ITs after {@link #startAppAndAttach} returns: the app
   * subprocess takes a moment to register its actor types with daprd, and tests will
   * fail with "did not find address for actor" if invoked too early.
   */
  protected static void waitForActorsReady(DaprContainer dapr) {
    DaprWait.forActors().waitUntilReady(dapr);
  }

  /**
   * Restarts the app subprocess on its same pre-allocated port. The daprd
   * container stays up and reconnects to the app via
   * {@code host.testcontainers.internal:appPort}. Because {@link #daprBuilder}
   * configures NO app-health-check, daprd does not deactivate actors during the
   * gap, so in-memory timers survive (matching the legacy
   * {@code @DaprRunConfig(enableAppHealthCheck=false)}). There is intentionally
   * no sleep between stop and start — {@code ActorTimerRecoveryIT} relies on a
   * quick restart.
   */
  protected static void restartApp(AppRun app) throws Exception {
    app.stop();
    app.start();
  }

  /**
   * Restarts the daprd container in place and re-waits for readiness. Placement
   * and scheduler are NOT recreated on the second start (their DaprContainer
   * fields are non-null), so a persisted actor reminder survives. Pinned host
   * ports re-bind, so the app's DAPR_HTTP_PORT/DAPR_GRPC_PORT and any DaprClient
   * remain valid.
   */
  protected static void restartSidecar(DaprContainer dapr) throws Exception {
    dapr.stop();
    dapr.start();
    try (DaprClient client = newDaprClient(dapr)) {
      client.waitForSidecar(30_000).block();
    }
    waitForActorsReady(dapr);
  }

  // ---------- DaprClient / ActorClient factories ----------

  protected static DaprClient newDaprClient(DaprContainer dapr) {
    return newDaprClientBuilder(dapr).build();
  }

  protected static DaprClientBuilder newDaprClientBuilder(DaprContainer dapr) {
    return new DaprClientBuilder().withPropertyOverrides(daprOverrides(dapr));
  }

  protected static ActorClient newActorClient(DaprContainer dapr) {
    ActorClient client = new ActorClient(new Properties(daprOverrides(dapr)), null);
    deferClose(client);
    return client;
  }

  /**
   * ActorClient overload that injects HTTP headers (metadata) on actor calls.
   * Used by ITs that need to override request-level headers like Content-Length.
   */
  protected static ActorClient newActorClient(DaprContainer dapr, Map<String, String> metadata) {
    ActorClient client = new ActorClient(new Properties(daprOverrides(dapr)), metadata, null);
    deferClose(client);
    return client;
  }

  private static Map<Property<?>, String> daprOverrides(DaprContainer dapr) {
    Map<Property<?>, String> overrides = new HashMap<>();
    overrides.put(Properties.HTTP_ENDPOINT, "http://127.0.0.1:" + dapr.getHttpPort());
    overrides.put(Properties.GRPC_ENDPOINT, "127.0.0.1:" + dapr.getGrpcPort());
    overrides.put(Properties.HTTP_PORT, String.valueOf(dapr.getHttpPort()));
    overrides.put(Properties.GRPC_PORT, String.valueOf(dapr.getGrpcPort()));
    return overrides;
  }

  // ---------- Component helpers (Redis) ----------

  protected static Component redisStateStore(String name) {
    return new Component(name, "state.redis", "v1", Map.of(
        "redisHost", SharedTestInfra.redisInternalHost(),
        "redisPassword", "",
        "actorStateStore", "true"
    ));
  }

  protected static Component redisPubSub(String name) {
    return new Component(name, "pubsub.redis", "v1", Map.of(
        "redisHost", SharedTestInfra.redisInternalHost(),
        "redisPassword", "",
        "processingTimeout", "100ms",
        "redeliverInterval", "100ms"
    ));
  }

  protected static Component redisConfigStore(String name) {
    return new Component(name, "configuration.redis", "v1", Map.of(
        "redisHost", SharedTestInfra.redisInternalHost(),
        "redisPassword", ""
    ));
  }

  /**
   * Mongo-backed state store with query API support. Lazily starts the
   * shared Mongo container before returning the component. Used by
   * {@code AbstractStateClientIT#saveAndQueryAndDeleteState}, which exercises
   * the Dapr preview Query State API — Redis doesn't support that API, so a
   * separate store is required.
   */
  protected static Component mongoStateStore(String name) {
    SharedTestInfra.mongo();   // ensure Mongo is up before DaprContainer needs it
    return new Component(name, "state.mongodb", "v1", Map.of(
        "host", SharedTestInfra.mongoInternalHost(),
        "databaseName", "local",
        "collectionName", "testCollection"
    ));
  }

  // ---------- Cleanup ----------

  protected static <T extends AutoCloseable> T deferClose(T object) {
    TO_BE_CLOSED.push(object);
    return object;
  }

  /**
   * Defer-stop a plain {@link Stoppable} (e.g., {@link AppRun}).
   * Use the {@link #deferStop(org.testcontainers.containers.GenericContainer) GenericContainer overload}
   * for Testcontainers — they aren't {@code Stoppable}.
   */
  protected static void deferStop(Stoppable stoppable) {
    TO_BE_STOPPED.push(stoppable);
  }

  /**
   * Adapter so a Testcontainer can be registered alongside AppRuns in the
   * stop queue.
   */
  protected static void deferStop(org.testcontainers.containers.GenericContainer<?> container) {
    TO_BE_STOPPED.push(() -> container.stop());
  }

  @AfterAll
  protected static void cleanUp() throws Exception {
    while (!TO_BE_STOPPED.isEmpty()) {
      try {
        TO_BE_STOPPED.pop().stop();
      } catch (Exception e) {
        // best-effort
        e.printStackTrace();
      }
    }
    while (!TO_BE_CLOSED.isEmpty()) {
      try {
        TO_BE_CLOSED.pop().close();
      } catch (Exception e) {
        // best-effort
        e.printStackTrace();
      }
    }
  }
}
