/*
 * Copyright 2023 The Dapr Authors
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

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import eu.rekawek.toxiproxy.model.toxic.Latency;
import eu.rekawek.toxiproxy.model.toxic.Timeout;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.resiliency.ResiliencyOptions;
import io.dapr.config.Properties;
import io.dapr.exceptions.DaprException;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.toxiproxy.ToxiproxyContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.shaded.org.awaitility.core.ConditionTimeoutException;

import java.io.IOException;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static io.dapr.it.testcontainers.ContainerConstants.TOXI_PROXY_IMAGE_TAG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
@Tags({@Tag("testcontainers"), @Tag("resiliency")})
public class SdkResiliencyIT {

  private static final Network NETWORK = Network.newNetwork();
  private static final String STATE_STORE_NAME = "kvstore";
  private static final int INFINITE_RETRY = -1;

  @RegisterExtension
  static WireMockExtension wireMock = WireMockExtension.newInstance()
      .options(WireMockConfiguration.wireMockConfig().dynamicPort())
      .build();

  private static DaprContainer daprContainer;

  @Container
  private static final ToxiproxyContainer TOXIPROXY = new ToxiproxyContainer(TOXI_PROXY_IMAGE_TAG)
      .withNetwork(NETWORK);

  private static Proxy proxy;

  private void configStub() {
    wireMock.stubFor(any(urlMatching("/actuator/health"))
        .willReturn(aResponse().withBody("[]").withStatus(200)));

    wireMock.stubFor(any(urlMatching("/dapr/subscribe"))
        .willReturn(aResponse().withBody("[]").withStatus(200)));

    wireMock.stubFor(get(urlMatching("/dapr/config"))
        .willReturn(aResponse().withBody("[]").withStatus(200)));

    wireMock.stubFor(post(urlEqualTo("/dapr.proto.runtime.v1.Dapr/SaveState"))
        .willReturn(aResponse().withStatus(204).withFixedDelay(1000)));

    wireMock.stubFor(any(urlMatching("/([a-z1-9]*)"))
        .willReturn(aResponse().withBody("[]").withStatus(200)));

    WireMock.configureFor("localhost", wireMock.getPort());
  }

  @BeforeAll
  static void configure() throws IOException {
    int wmPort = wireMock.getPort();
    org.testcontainers.Testcontainers.exposeHostPorts(wmPort);

    daprContainer = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
        .withAppName("dapr-app")
        .withAppPort(wmPort)
        .withDaprLogLevel(DaprLogLevel.DEBUG)
        .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("dapr-logs")))
        .withAppHealthCheckPath("/actuator/health")
        .withAppChannelAddress("host.testcontainers.internal")
        .withNetworkAliases("dapr")
        .withNetwork(NETWORK);
    daprContainer.start();

    ToxiproxyClient toxiproxyClient = new ToxiproxyClient(TOXIPROXY.getHost(), TOXIPROXY.getControlPort());
    proxy = toxiproxyClient.createProxy("dapr", "0.0.0.0:8666", "dapr:3500");
  }

  @AfterAll
  static void afterAll() {
    if (daprContainer != null) {
      daprContainer.stop();
    }
  }

  @BeforeEach
  public void beforeEach() {
    configStub();
  }

  @Test
  @DisplayName("should throw exception when the configured timeout exceeding waitForSidecar's timeout")
  public void testSidecarWithoutTimeout() {
    Assertions.assertThrows(RuntimeException.class, () -> {
      try (DaprClient client = createDaprClientBuilder().build()) {
        Timeout timeout = proxy.toxics().timeout("timeout", ToxicDirection.DOWNSTREAM, 3000);
        client.waitForSidecar(2000).block();
        timeout.remove();
      }
    });
  }

  @Test
  @DisplayName("should fail when resiliency options has 900ms and the latency is 950ms")
  public void shouldFailDueToLatencyExceedingConfiguration() throws Exception {
    Latency latency = proxy.toxics().latency("latency", ToxicDirection.DOWNSTREAM, Duration.ofMillis(950).toMillis());

    DaprClient client =
        createDaprClientBuilder().withResiliencyOptions(new ResiliencyOptions().setTimeout(Duration.ofMillis(900)))
            .build();

    String errorMessage = assertThrows(DaprException.class, () -> {
      client.saveState(STATE_STORE_NAME, "users", "[]").block();
    }).getMessage();

    assertThat(errorMessage).contains("DEADLINE_EXCEEDED");

    latency.remove();
    client.close();
  }

  @Test
  @DisplayName("should fail when resiliency's options has infinite retry with time 900ms and the latency is 950ms")
  public void shouldFailDueToLatencyExceedingConfigurationWithInfiniteRetry() throws Exception {
    Duration ms900 = Duration.ofMillis(900);
    Duration ms950 = Duration.ofMillis(950);

    Latency latency = proxy.toxics().latency("latency-infinite-retry", ToxicDirection.DOWNSTREAM, ms950.toMillis());

    DaprClient client =
        createDaprClientBuilder().withResiliencyOptions(new ResiliencyOptions().setTimeout(ms900).setMaxRetries(
                INFINITE_RETRY))
            .build();

    Assertions.assertThrows(ConditionTimeoutException.class, () -> {
      Awaitility.await("10 seconds because the retry should be infinite")
          .atMost(Duration.ofSeconds(10))
          .until(() -> {
            boolean finished = true;
            client.saveState(STATE_STORE_NAME, "users", "[]").block();
            return finished;
          });
    });

    latency.remove();
    client.close();
  }

  @Test
  @DisplayName("should fail due to latency exceeding configuration with once retry")
  public void shouldFailDueToLatencyExceedingConfigurationWithOnceRetry() throws Exception {
    int wmPort = wireMock.getPort();

    DaprClient client =
        new DaprClientBuilder().withPropertyOverride(Properties.HTTP_ENDPOINT, "http://localhost:" + wmPort)
            .withPropertyOverride(Properties.GRPC_ENDPOINT, "http://localhost:" + wmPort)
            .withResiliencyOptions(new ResiliencyOptions().setTimeout(Duration.ofMillis(900))
                .setMaxRetries(1))
            .build();

    try {
      client.saveState(STATE_STORE_NAME, "users", "[]").block();
    } catch (Exception ignored) {
    }

    wireMock.verify(2, postRequestedFor(urlEqualTo("/dapr.proto.runtime.v1.Dapr/SaveState")));

    client.close();
  }

  private static DaprClientBuilder createDaprClientBuilder() {
    return new DaprClientBuilder()
        .withPropertyOverride(Properties.HTTP_ENDPOINT, "http://localhost:" + TOXIPROXY.getMappedPort(8666))
        .withPropertyOverride(Properties.GRPC_ENDPOINT, "http://localhost:" + TOXIPROXY.getMappedPort(8666));
  }

}
