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

package io.dapr.it.testcontainers.core;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.Metadata;
import io.dapr.client.domain.State;
import io.dapr.config.Properties;
import io.dapr.testcontainers.DaprContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.awaitility.core.ConditionTimeoutException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;


@Testcontainers
@WireMockTest(httpPort = 8081)
@Tag("testcontainers")
public class DaprContainerIT {

  // Time-to-live for messages published.
  private static final String MESSAGE_TTL_IN_SECONDS = "1000";
  private static final String STATE_STORE_NAME = "kvstore";
  private static final String KEY = "my-key";
  private static final String PUBSUB_NAME = "pubsub";
  private static final String PUBSUB_TOPIC_NAME = "topic";
  private static final String APP_FOUND_MESSAGE_PATTERN = ".*application discovered on port 8081.*";

  @Container
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
          .withAppName("dapr-app")
          .withAppPort(8081)
          .withAppHealthCheckPath("/actuator/health")
          .withAppChannelAddress("host.testcontainers.internal");

  /**
   * Sets the Dapr properties for the test.
   */
  @BeforeEach
  public void setDaprProperties() {
    configStub();
    org.testcontainers.Testcontainers.exposeHostPorts(8081);
  }

  private void configStub() {
    stubFor(any(urlMatching("/actuator/health"))
            .willReturn(aResponse().withBody("[]").withStatus(200)));

    stubFor(any(urlMatching("/dapr/subscribe"))
            .willReturn(aResponse().withBody("[]").withStatus(200)));

    stubFor(get(urlMatching("/dapr/config"))
            .willReturn(aResponse().withBody("[]").withStatus(200)));

    stubFor(any(urlMatching("/([a-z1-9]*)"))
            .willReturn(aResponse().withBody("[]").withStatus(200)));

    // create a stub
    stubFor(post(urlEqualTo("/events"))
            .willReturn(aResponse().withBody("event received!").withStatus(200)));

    configureFor("localhost", 8081);
  }

  @Test
  public void testDaprContainerDefaults() {
    assertEquals(2,
            DAPR_CONTAINER.getComponents().size(),
            "The pubsub and kvstore component should be configured by default"
    );
    assertEquals(
            1,
            DAPR_CONTAINER.getSubscriptions().size(),
            "A subscription should be configured by default if none is provided"
    );
  }

  @Test
  public void testStateStore() throws Exception {
    DaprClientBuilder builder = createDaprClientBuilder();

    try (DaprClient client = (builder).build()) {
      String value = "value";
      // Save state
      client.saveState(STATE_STORE_NAME, KEY, value).block();

      // Get the state back from the state store
      State<String> retrievedState = client.getState(STATE_STORE_NAME, KEY, String.class).block();

      assertNotNull(retrievedState);
      assertEquals(value, retrievedState.getValue(), "The value retrieved should be the same as the one stored");
    }
  }

  @Test
  public void testPlacement() throws Exception {
    Wait.forLogMessage(APP_FOUND_MESSAGE_PATTERN, 1).waitUntilReady(DAPR_CONTAINER);
    try {
      await().atMost(10, TimeUnit.SECONDS)
              .pollDelay(500, TimeUnit.MILLISECONDS)
              .pollInterval(500, TimeUnit.MILLISECONDS)
              .until(() -> {
                String metadata = checkSidecarMetadata();
                if (metadata.contains("placement: connected")) {
                  return true;
                } else {
                  return false;
                }
              });
    } catch (ConditionTimeoutException timeoutException) {
      fail("The placement server is not connected");
    }

  }

  private String checkSidecarMetadata() throws IOException, InterruptedException {
    String url = DAPR_CONTAINER.getHttpEndpoint() + "/v1.0/metadata";
    HttpClient client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .build();
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      throw new IOException("Unexpected response: " + response.statusCode());
    }

    return response.body();
  }

  @Test
  public void testPubSub() throws Exception {
    DaprClientBuilder builder = createDaprClientBuilder();

    try (DaprClient client = (builder).build()) {
      String message = "message content";
      Map<String, String> metadata = Map.of(Metadata.TTL_IN_SECONDS, MESSAGE_TTL_IN_SECONDS);
      client.publishEvent(PUBSUB_NAME, PUBSUB_TOPIC_NAME, message, metadata).block();
    }

    verify(postRequestedFor(urlEqualTo("/events")).withHeader("Content-Type", equalTo("application/cloudevents+json")));
  }

  private DaprClientBuilder createDaprClientBuilder() {
    return new DaprClientBuilder()
            .withPropertyOverride(Properties.HTTP_ENDPOINT, DAPR_CONTAINER.getHttpEndpoint())
            .withPropertyOverride(Properties.GRPC_ENDPOINT, DAPR_CONTAINER.getGrpcEndpoint());
  }
}
