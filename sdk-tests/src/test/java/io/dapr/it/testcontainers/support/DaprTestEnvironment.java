/*
 * Copyright 2026 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.dapr.it.testcontainers.support;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprContainerConstants;
import io.dapr.testcontainers.DaprPlacementContainer;
import io.dapr.testcontainers.DaprProtocol;
import io.dapr.testcontainers.DaprSchedulerContainer;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.lifecycle.Startable;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.dapr.it.testcontainers.ContainerConstants.JDK_17_TEMURIN_JAMMY;

/** Owns one application, its sidecar, control plane and private network. */
public final class DaprTestEnvironment implements Startable {
  private static final int APP_PORT = 8080;
  private static final String APP_RUNTIME = "/it-runtime";
  private final String identity;
  private final String mainClass;
  private final DaprProtocol protocol;
  private final Network network;
  private final OwnedResources resources;
  private final DaprPlacementContainer placement;
  private final DaprSchedulerContainer scheduler;
  private final GenericContainer<?> app;
  private final DaprContainer dapr;
  private final StringBuffer appOutput = new StringBuffer();

  /** Defines an isolated topology; no container is started during construction. */
  public DaprTestEnvironment(String appId, Class<?> mainClass) {
    this(appId, mainClass, DaprProtocol.HTTP);
  }

  /**
   * Defines an isolated topology with the specified application callback protocol.
   * @param appId Dapr application ID.
   * @param mainClass Application entry point.
   * @param protocol Application callback protocol.
   */
  public DaprTestEnvironment(String appId, Class<?> mainClass, DaprProtocol protocol) {
    this(appId, mainClass.getName(), protocol, Duration.ofMinutes(2));
  }

  DaprTestEnvironment(String appId, String mainClass, Duration appReadinessTimeout) {
    this(appId, mainClass, DaprProtocol.HTTP, appReadinessTimeout);
  }

  DaprTestEnvironment(String appId, String mainClass, DaprProtocol protocol, Duration appReadinessTimeout) {
    if (protocol != DaprProtocol.HTTP && protocol != DaprProtocol.GRPC) {
      throw new IllegalArgumentException("Unsupported application protocol: " + protocol);
    }
    identity = appId + "-" + UUID.randomUUID();
    this.mainClass = mainClass;
    this.protocol = protocol;
    Map<String, String> labels = Map.of("io.dapr.it.environment", identity);
    network = Network.builder().createNetworkCmdModifier(cmd -> cmd.withLabels(labels)).build();
    resources = new OwnedResources(network);
    placement = new DaprPlacementContainer(DaprContainerConstants.DAPR_PLACEMENT_IMAGE_TAG)
        .withNetwork(network).withNetworkAliases("placement").withLabels(labels)
        .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));
    scheduler = new DaprSchedulerContainer(DaprContainerConstants.DAPR_SCHEDULER_IMAGE_TAG)
        .withNetwork(network).withNetworkAliases("scheduler").withLabels(labels)
        .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));
    WaitStrategy appWait = protocol == DaprProtocol.GRPC
        ? new GrpcCallbackWaitStrategy(APP_PORT).withStartupTimeout(appReadinessTimeout)
        : Wait.forHttp("/health").forPort(APP_PORT).forStatusCode(200).withStartupTimeout(appReadinessTimeout);
    app = new GenericContainer<>(JDK_17_TEMURIN_JAMMY)
        .withNetwork(network).withNetworkAliases("app").withLabels(labels)
        .withExposedPorts(APP_PORT)
        .waitingFor(appWait)
        .withEnv("DAPR_HTTP_ENDPOINT", Endpoints.http("dapr", 3500).toString())
        .withEnv("DAPR_GRPC_ENDPOINT", "dapr:50001")
        .withEnv("DAPR_SIDECAR_IP", "dapr")
        .withEnv("DAPR_HTTP_PORT", "3500")
        .withEnv("DAPR_GRPC_PORT", "50001");
    dapr = new DaprContainer(DaprContainerConstants.DAPR_RUNTIME_IMAGE_TAG)
        .withNetwork(network).withNetworkAliases("dapr").withLabels(labels)
        .withAccessToHost(false)
        .withAppName(appId).withAppPort(APP_PORT).withAppChannelAddress("app").withAppProtocol(protocol)
        .withPlacementContainer(placement).withSchedulerContainer(scheduler)
        .waitingFor(Wait.forHttp("/v1.0/healthz/outbound").forPort(3500)
            .forStatusCodeMatching(status -> status >= 200 && status <= 399)
            .withStartupTimeout(Duration.ofMinutes(2)));
    attachLogs(placement, "placement");
    attachLogs(scheduler, "scheduler");
    attachLogs(app, "app");
    attachLogs(dapr, "dapr");
  }

  private void attachLogs(GenericContainer<?> container, String alias) {
    container.withLogConsumer(frame -> {
      String text = frame.getUtf8String();
      if ("app".equals(alias)) {
        appOutput.append(text);
      }
      if (!text.isEmpty()) {
        System.out.print("[" + identity + " " + alias + "] " + text);
      }
    });
  }

  private void configureApplication() {
    Path runtime = Path.of("target", "it-runtime").toAbsolutePath().normalize();
    Path lib = runtime.resolve("lib");
    List<String> classpath = new ArrayList<>();
    classpath.add(APP_RUNTIME + "/test-classes");
    classpath.add(APP_RUNTIME + "/classes");
    try (var files = Files.walk(lib)) {
      classpath.addAll(files.filter(path -> path.toString().endsWith(".jar"))
          .sorted().map(path -> APP_RUNTIME + "/lib/" + lib.relativize(path).toString().replace('\\', '/'))
          .toList());
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read staged IT dependencies", e);
    }
    app.withFileSystemBind(runtime.toString(), APP_RUNTIME, BindMode.READ_ONLY);
    app.withCommand("java", "-cp", String.join(":", classpath), mainClass, Integer.toString(APP_PORT));
  }

  @Override
  public void start() {
    try {
      resources.start(placement);
      System.out.printf("[%s] placement id=%s%n", identity, placement.getContainerId());
      resources.start(scheduler);
      System.out.printf("[%s] scheduler id=%s%n", identity, scheduler.getContainerId());
      configureApplication();
      resources.start(app);
      System.out.printf("[%s] app id=%s image=%s protocol=%s address=%s%n", identity, app.getContainerId(),
          JDK_17_TEMURIN_JAMMY, protocol, appAddress());
      resources.start(dapr);
      System.out.printf("[%s] dapr id=%s endpoint=%s grpc=%s:%d%n", identity, dapr.getContainerId(),
          Endpoints.http(dapr.getHost(), dapr.getHttpPort()), dapr.getHost(), dapr.getGrpcPort());
      try (DaprClient client = clientBuilder().build()) {
        client.waitForSidecar(30_000).block(Duration.ofSeconds(35));
        client.getMetadata().retryWhen(Retry.fixedDelay(30, Duration.ofSeconds(1))).block(Duration.ofSeconds(35));
      } catch (Exception e) {
        throw new IllegalStateException("Sidecar client readiness failed", e);
      }
      System.out.printf("[%s] app=%s (%s, %s), dapr=%s (%s), placement=%s, scheduler=%s%n", identity,
          appAddress(), protocol, app.getContainerId(), Endpoints.http(dapr.getHost(), dapr.getHttpPort()),
          dapr.getContainerId(), placement.getContainerId(), scheduler.getContainerId());
    } catch (RuntimeException | Error failure) {
      try {
        resources.stop();
      } catch (RuntimeException | Error cleanup) {
        failure.addSuppressed(cleanup);
      }
      throw failure;
    }
  }

  @Override
  public void stop() {
    resources.stop();
  }

  /** Returns the HTTP application endpoint reachable by the test JVM. */
  public URI appEndpoint() {
    if (protocol != DaprProtocol.HTTP) {
      throw new IllegalStateException("Application is not HTTP: " + protocol);
    }
    return Endpoints.http(app.getHost(), app.getMappedPort(APP_PORT));
  }

  /**
   * Returns the mapped host and port as an IPv6-safe authority.
   * @return Application authority reachable by the test JVM.
   */
  public String appAddress() {
    return Endpoints.http(app.getHost(), app.getMappedPort(APP_PORT)).getRawAuthority();
  }

  /** Returns a client builder configured for the mapped sidecar ports. */
  public DaprClientBuilder clientBuilder() {
    return Endpoints.client(dapr.getHost(), dapr.getHttpPort(), dapr.getGrpcPort());
  }

  /** Label value identifying just this environment's Docker resources. */
  public String identity() {
    return identity;
  }

  String appOutput() {
    return appOutput.toString();
  }

  DaprProtocol appProtocol() {
    return dapr.getAppProtocol();
  }

  String appContainerId() {
    return app.getContainerId();
  }
}
