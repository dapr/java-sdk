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
 * limitations under the License.
*/

package io.dapr.it.resiliency;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import eu.rekawek.toxiproxy.model.toxic.Latency;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.config.Properties;
import io.dapr.it.containers.BaseContainerIT;
import io.dapr.testcontainers.DaprContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test SDK resiliency around {@code waitForSidecar}.
 */
public class WaitForSidecarIT extends BaseContainerIT {

  // Use a number large enough to make sure it will respect the entire timeout.
  private static final Duration LATENCY = Duration.ofSeconds(5);

  private static DaprContainer dapr;

  private static ToxiproxyContainer toxiproxy;

  private static Proxy proxy;

  private static DaprContainer daprNotRunning;

  private static int daprNotRunningHttpPort;

  @BeforeAll
  public static void init() throws IOException {
    dapr = daprBuilder("wait-for-sidecar-it")
        .withNetworkAliases("dapr");
    dapr.start();
    deferStop(dapr);

    toxiproxy = newToxiproxy();
    ToxiproxyClient toxiproxyClient = new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort());
    proxy = toxiproxyClient.createProxy("dapr", "0.0.0.0:8666", "dapr:3500");

    // A second sidecar that is started and then stopped, so a client pointed at its
    // (now-dead) mapped port cannot possibly get a response. Deterministic stand-in
    // for a "Dapr is not running" target.
    daprNotRunning = daprBuilder("wait-for-sidecar-it-not-running")
        .withNetworkAliases("dapr-not-running");
    daprNotRunning.start();
    daprNotRunningHttpPort = daprNotRunning.getHttpPort();
    daprNotRunning.stop();
  }

  @Test
  public void waitSucceeds() throws Exception {
    try (DaprClient client = newDaprClient(dapr)) {
      client.waitForSidecar(5000).block();
    }
  }

  @Test
  public void waitTimeout() throws IOException {
    int timeoutInMillis = (int) LATENCY.minusMillis(100).toMillis();
    long started = System.currentTimeMillis();

    Latency latency = proxy.toxics().latency("latency", ToxicDirection.DOWNSTREAM, LATENCY.toMillis());
    try {
      assertThrows(RuntimeException.class, () -> {
        try (DaprClient client = viaProxyClientBuilder().build()) {
          client.waitForSidecar(timeoutInMillis).block();
        }
      });
    } finally {
      latency.remove();
    }

    long duration = System.currentTimeMillis() - started;

    assertThat(duration).isGreaterThanOrEqualTo(timeoutInMillis);
  }

  @Test
  public void waitSlow() throws Exception {
    int timeoutInMillis = (int) LATENCY.plusMillis(100).toMillis();
    long started = System.currentTimeMillis();

    Latency latency = proxy.toxics().latency("latency", ToxicDirection.DOWNSTREAM, LATENCY.toMillis());
    try {
      try (DaprClient client = viaProxyClientBuilder().build()) {
        client.waitForSidecar(timeoutInMillis).block();
      }
    } finally {
      latency.remove();
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
      try (DaprClient client = notRunningClientBuilder().build()) {
        client.waitForSidecar(timeoutMilliseconds).block();
      }
    });

    long duration = System.currentTimeMillis() - started;

    assertThat(duration).isGreaterThanOrEqualTo(timeoutMilliseconds);
  }

  private static DaprClientBuilder viaProxyClientBuilder() {
    return new DaprClientBuilder()
        .withPropertyOverride(Properties.HTTP_ENDPOINT, "http://localhost:" + toxiproxy.getMappedPort(8666))
        .withPropertyOverride(Properties.GRPC_ENDPOINT, "http://localhost:" + toxiproxy.getMappedPort(8666));
  }

  private static DaprClientBuilder notRunningClientBuilder() {
    return new DaprClientBuilder()
        .withPropertyOverride(Properties.HTTP_ENDPOINT, "http://localhost:" + daprNotRunningHttpPort)
        .withPropertyOverride(Properties.GRPC_ENDPOINT, "http://localhost:" + daprNotRunningHttpPort);
  }
}
