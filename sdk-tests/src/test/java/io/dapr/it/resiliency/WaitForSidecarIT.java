/*
 * Copyright 2024 The Dapr Authors
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

package io.dapr.it.resiliency;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import io.dapr.client.DaprClientBuilder;
import io.dapr.config.Properties;
import io.dapr.it.testcontainers.DaprClientFactory;
import io.dapr.testcontainers.DaprContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static io.dapr.it.testcontainers.ContainerConstants.TOXI_PROXY_IMAGE_TAG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test SDK resiliency.
 */
@Testcontainers
public class WaitForSidecarIT {

  // Use a number large enough to make sure it will respect the entire timeout.
  private static final Duration LATENCY = Duration.ofSeconds(5);

  private static final Network NETWORK = Network.newNetwork();
  private static final String APP_ID = "wait-for-sidecar-it";

  @Container
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
      .withAppName(APP_ID)
      .withNetwork(NETWORK)
      .withNetworkAliases("dapr");

  @Container
  private static final ToxiproxyContainer TOXIPROXY = new ToxiproxyContainer(TOXI_PROXY_IMAGE_TAG)
      .withNetwork(NETWORK);

  private static Proxy proxy;
  private static String notRunningHttpEndpoint;
  private static String notRunningGrpcEndpoint;

  @BeforeAll
  public static void init() throws Exception {
    ToxiproxyClient toxiproxyClient = new ToxiproxyClient(TOXIPROXY.getHost(), TOXIPROXY.getControlPort());
    proxy = toxiproxyClient.createProxy("dapr", "0.0.0.0:8666", "dapr:3500");

    DaprContainer notRunningContainer = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
        .withAppName(APP_ID + "-not-running");
    notRunningContainer.start();
    notRunningHttpEndpoint = notRunningContainer.getHttpEndpoint();
    notRunningGrpcEndpoint = notRunningContainer.getGrpcEndpoint();
    notRunningContainer.stop();
  }

  @BeforeEach
  void beforeEach() throws Exception {
    proxy.toxics().getAll().forEach(toxic -> {
      try {
        toxic.remove();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void waitSucceeds() throws Exception {
    try (var client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build()) {
      client.waitForSidecar(5000).block();
    }
  }

  @Test
  public void waitTimeout() throws Exception {
    int timeoutInMillis = (int)LATENCY.minusMillis(100).toMillis();
    long started = System.currentTimeMillis();
    applyLatencyToxic();

    assertThrows(RuntimeException.class, () -> {
      try (var client = createToxiProxyClientBuilder().build()) {
        client.waitForSidecar(timeoutInMillis).block();
      }
    });

    long duration = System.currentTimeMillis() - started;

    assertThat(duration).isGreaterThanOrEqualTo(timeoutInMillis);
  }

  @Test
  public void waitSlow() throws Exception {
    int timeoutInMillis = (int)LATENCY.plusMillis(100).toMillis();
    long started = System.currentTimeMillis();
    applyLatencyToxic();

    try (var client = createToxiProxyClientBuilder().build()) {
      client.waitForSidecar(timeoutInMillis).block();
    }

    long duration = System.currentTimeMillis() - started;

    assertThat(duration).isGreaterThanOrEqualTo(LATENCY.toMillis());
  }

  @Test
  public void waitNotRunningTimeout() {
    // Does not make this number too smaller since bug does not repro when <= 2.5s.
    // This has to do with a previous bug in the implementation.
    int timeoutMilliseconds = 5000;
    long started = System.currentTimeMillis();

    assertThrows(RuntimeException.class, () -> {
      try (var client = new DaprClientBuilder()
          .withPropertyOverride(Properties.HTTP_ENDPOINT, notRunningHttpEndpoint)
          .withPropertyOverride(Properties.GRPC_ENDPOINT, notRunningGrpcEndpoint)
          .build()) {
        client.waitForSidecar(timeoutMilliseconds).block();
      }
    });

    long duration = System.currentTimeMillis() - started;

    assertThat(duration).isGreaterThanOrEqualTo(timeoutMilliseconds);
  }

  private static DaprClientBuilder createToxiProxyClientBuilder() {
    String endpoint = "http://localhost:" + TOXIPROXY.getMappedPort(8666);
    return new DaprClientBuilder()
        .withPropertyOverride(Properties.HTTP_ENDPOINT, endpoint)
        .withPropertyOverride(Properties.GRPC_ENDPOINT, endpoint);
  }

  private static void applyLatencyToxic() throws Exception {
    proxy.toxics().latency("latency", ToxicDirection.DOWNSTREAM, LATENCY.toMillis()).setJitter(0L);
  }
}
