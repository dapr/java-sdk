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

package io.dapr.it.testcontainers;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.Metadata;
import io.dapr.client.domain.State;

import io.dapr.testcontainers.DaprContainer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.Testcontainers;

import java.io.IOException;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DaprContainerTest {

  // Time-to-live for messages published.
  private static final String MESSAGE_TTL_IN_SECONDS = "1000";
  private static final String STATE_STORE_NAME = "kvstore";
  private static final String KEY = "my-key";
  private static final String PUBSUB_NAME = "pubsub";
  private static final String PUBSUB_TOPIC_NAME = "topic";

  @ClassRule
  public static WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(8081));

  @ClassRule
  public static DaprContainer daprContainer = new DaprContainer("daprio/daprd")
      .withAppName("dapr-app")
      .withAppPort(8081)
      .withAppChannelAddress("host.testcontainers.internal");

  /**
   * Sets the Dapr properties for the test.
   */
  @BeforeClass
  public static void setDaprProperties() {
    configStub();
    Testcontainers.exposeHostPorts(8081);
    System.setProperty("dapr.grpc.port", Integer.toString(daprContainer.getGrpcPort()));
    System.setProperty("dapr.http.port", Integer.toString(daprContainer.getHttpPort()));
  }

  private static void configStub() {

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
    assertEquals(
        "The pubsub and kvstore component should be configured by default",
        2,
        daprContainer.getComponents().size());
    assertEquals(
        "A subscription should be configured by default if none is provided",
        1,
        daprContainer.getSubscriptions().size());
  }

  @Test
  public void testStateStore() throws Exception {
    try (DaprClient client = (new DaprClientBuilder()).build()) {
      client.waitForSidecar(5000).block();

      String value = "value";
      // Save state
      client.saveState(STATE_STORE_NAME, KEY, value).block();

      // Get the state back from the state store
      State<String> retrievedState = client.getState(STATE_STORE_NAME, KEY, String.class).block();

      assertEquals("The value retrieved should be the same as the one stored", value, retrievedState.getValue());
    }
  }

  @Test
  public void testPlacement() throws Exception {
    // Here we are just waiting for Dapr to be ready
    try (DaprClient client = (new DaprClientBuilder()).build()) {
      client.waitForSidecar(5000).block();
    }

    OkHttpClient client = new OkHttpClient.Builder().build();

    String url = "http://" + daprContainer.getHost() + ":" + daprContainer.getMappedPort(3500);
    Request request = new Request.Builder().url(url + "/v1.0/metadata").build();

    try (Response response = client.newCall(request).execute()) {
      if (response.isSuccessful()) {
        assertTrue(response.body().string().contains("placement: connected"));

      } else {
        throw new IOException("Unexpected response: " + response.code());
      }
    }

  }

  @Test
  public void testPubSub() throws Exception {
    try (DaprClient client = (new DaprClientBuilder()).build()) {
      client.waitForSidecar(5000).block();

      String message = "message content";
      Map<String, String> metadata = singletonMap(Metadata.TTL_IN_SECONDS, MESSAGE_TTL_IN_SECONDS);
      client.publishEvent(PUBSUB_NAME, PUBSUB_TOPIC_NAME, message, metadata).block();
    }

    verify(getRequestedFor(urlMatching("/dapr/config")));
    verify(postRequestedFor(urlEqualTo("/events")).withHeader("Content-Type", equalTo("application/cloudevents+json")));
  }
}
