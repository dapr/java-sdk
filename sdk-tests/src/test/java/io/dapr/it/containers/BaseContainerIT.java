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

  /** Pinned Dapr runtime image. Matches what spring-boot-4-sdk-tests uses. */
  protected static final String DAPR_IMAGE = "daprio/daprd:1.15.6";

  protected static final String STATE_STORE_NAME = "statestore";
  protected static final String PUBSUB_NAME = "messagebus";
  protected static final String CONFIG_STORE_NAME = "redisconfigstore";

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
        .withDaprLogLevel(DaprLogLevel.INFO)
        // Reuses the placement sidecar container within this JVM (Testcontainers manages it);
        // orthogonal to SharedTestInfra's Redis `withReuse(true)`.
        .withReusablePlacement(true);
  }

  // ---------- App lifecycle ----------

  /** Pair returned by {@link #startAppAndAttach}. */
  public record DaprAndApp(DaprContainer dapr, AppRun app) {}

  /**
   * Two-phase startup for ITs that need an app callback. Allocates the app
   * port, exposes it to Testcontainers, lets the caller build and start the
   * DaprContainer (which now knows the appPort + appChannelAddress), then
   * spawns the AppRun subprocess with the DaprContainer's mapped HTTP/gRPC
   * ports. Returns both. Both are registered for {@code @AfterAll} cleanup
   * via {@link #deferStop} (DaprContainer first, AppRun second — stopped LIFO).
   *
   * @param appName       used both as the Dapr app id and the AppRun name
   * @param serviceClass  the class whose {@code main(String[])} the subprocess runs
   * @param protocol      reserved for future use; AppRun currently ignores it
   * @param daprFactory   given the allocated app port, returns a STARTED
   *                      DaprContainer (factory body builds DaprContainer,
   *                      calls {@code .withAppPort(appPort)
   *                      .withAppChannelAddress("host.testcontainers.internal")},
   *                      and calls {@code .start()})
   */
  protected static DaprAndApp startAppAndAttach(
      String appName,
      Class<?> serviceClass,
      AppRun.AppProtocol protocol,
      java.util.function.IntFunction<DaprContainer> daprFactory) throws Exception {
    // Only the app port matters here — Dapr HTTP/gRPC ports will come from
    // the started DaprContainer's getMappedPort. Allocate only what we need.
    DaprPorts ports = DaprPorts.build(true, false, false);
    int appPort = ports.getAppPort();
    Testcontainers.exposeHostPorts(appPort);

    DaprContainer dapr = daprFactory.apply(appPort);
    // dapr is started inside the factory.
    deferStop(dapr);

    AppRun app = new AppRun(
        ports,
        getServiceSuccessMessage(serviceClass),
        serviceClass,
        60_000,
        dapr.getHttpPort(),
        dapr.getGrpcPort());
    app.start();
    deferStop(app);
    return new DaprAndApp(dapr, app);
  }

  /**
   * Best-effort lookup of a {@code public static final String SUCCESS_MESSAGE}
   * on the service class, falling back to {@code "You're up and running!"}.
   * Existing sdk-tests service classes follow this convention.
   */
  private static String getServiceSuccessMessage(Class<?> serviceClass) {
    try {
      Object value = serviceClass.getField("SUCCESS_MESSAGE").get(null);
      if (value instanceof String) {
        return (String) value;
      }
    } catch (NoSuchFieldException | IllegalAccessException ignored) {
      // fall through
    }
    return "You're up and running!";
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
