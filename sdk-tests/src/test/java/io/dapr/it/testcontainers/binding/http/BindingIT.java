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
package io.dapr.it.testcontainers.binding.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import io.dapr.exceptions.DaprException;
import io.dapr.it.testcontainers.binding.http.app.InputBindingService;
import io.dapr.it.testcontainers.support.DaprTestEnvironment;
import io.dapr.testcontainers.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exercises HTTP and Kafka bindings against an isolated sidecar. */
@Testcontainers
@Tag("testcontainers")
@Execution(ExecutionMode.SAME_THREAD)
public class BindingIT {
  private static final String APP_NAME = "bindingit-grpc";
  private static final String BINDING = "sample123";
  private static final String TOPIC = "binding-it";
  private static final String GROUP = "binding-it";
  private static final String STRICT = "fixture-http-strict";
  private static final String FORWARD = "fixture-http-forward";
  private static final String MARKER = "binding-fixture-not-found";

  @Container
  private static final DaprTestEnvironment ENVIRONMENT = new DaprTestEnvironment(APP_NAME, InputBindingService.class)
      .withPrerequisite(new KafkaContainer("apache/kafka:3.8.0")
          .withListener("kafka:19092").withEnv("KAFKA_NUM_PARTITIONS", "1"), "kafka", List.of(
              new Component(BINDING, "bindings.kafka", "v1", Map.of(
                  "brokers", "kafka:19092", "topics", TOPIC, "publishTopic", TOPIC,
                  "consumerGroup", GROUP, "authRequired", "false", "initialOffset", "oldest")),
              httpBinding(STRICT, true), httpBinding(FORWARD, false)));

  private static Component httpBinding(String name, boolean errorIfNot2xx) {
    return new Component(name, "bindings.http", "v1", Map.of(
        "url", "http://app:8080/fixture/upstream/not-found", "errorIfNot2XX", Boolean.toString(errorIfNot2xx)));
  }

  @BeforeEach
  public void reset() throws Exception {
    HttpRequest request = HttpRequest.newBuilder(ENVIRONMENT.appEndpoint().resolve("/reset"))
        .timeout(Duration.ofSeconds(10)).POST(HttpRequest.BodyPublishers.noBody()).build();
    assertEquals(200, HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding()).statusCode());
    System.out.printf("[%s] broker=kafka:19092 topic=%s group=%s components=%s,%s,%s reset%n",
        ENVIRONMENT.identity(), TOPIC, GROUP, BINDING, STRICT, FORWARD);
  }

  @Test
  public void httpOutputBindingError() throws Exception {
    try (DaprClient client = ENVIRONMENT.clientBuilder().build()) {
      DaprException error = assertThrows(DaprException.class, () -> client.invokeBinding(STRICT, "get", "").block());
      assertEquals(404, error.getHttpStatusCode());
      assertTrue(new String(error.getPayload(), StandardCharsets.UTF_8)
          .contains("error invoking output binding " + STRICT + ": received status code 404"));
    }
  }

  @Test
  public void httpOutputBindingErrorIgnoredByComponent() throws Exception {
    try (DaprClient client = ENVIRONMENT.clientBuilder().build()) {
      DaprException error = assertThrows(DaprException.class, () -> client.invokeBinding(FORWARD, "get", "").block());
      assertEquals(404, error.getHttpStatusCode());
      assertEquals(MARKER, new ObjectMapper().readTree(error.getPayload()).get("marker").asText());
    }
  }

  @Test
  public void inputOutputBinding() throws Exception {
    try (DaprClient client = ENVIRONMENT.clientBuilder().build()) {
      long deadline = System.nanoTime() + Duration.ofMinutes(2).toNanos();
      boolean ready = false;
      while (System.nanoTime() < deadline && !ready) {
        client.invokeBinding(BINDING, "create", "ping").block(Duration.ofSeconds(10));
        ready = Boolean.TRUE.equals(client.invokeMethod(APP_NAME, "initialized", null,
            HttpExtension.GET, Boolean.class).block(Duration.ofSeconds(10)));
        if (!ready) {
          pause(500);
        }
      }
      assertTrue(ready, "Kafka input callback did not initialize: environment=" + ENVIRONMENT.identity()
          + " broker=kafka:19092 topic=" + TOPIC + " group=" + GROUP);

      MyClass payload = new MyClass();
      payload.message = "hello";
      client.invokeBinding(BINDING, "create", payload, Map.of("MyMetadata", "MyValue"), Void.class)
          .block(Duration.ofSeconds(10));
      client.invokeBinding(BINDING, "create", "cat", Map.of("MyMetadata", "MyValue"), Void.class)
          .block(Duration.ofSeconds(10));

      List<String> messages = List.of();
      deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
      while (System.nanoTime() < deadline) {
        messages = client.invokeMethod(APP_NAME, "messages", null, HttpExtension.GET, List.class)
            .block(Duration.ofSeconds(10));
        if (messages.size() >= 2) {
          break;
        }
        pause(200);
      }
      assertEquals(2, messages.size(), "environment=" + ENVIRONMENT.identity() + " received=" + messages);
      ObjectMapper mapper = new ObjectMapper();
      assertEquals("hello", mapper.readValue(messages.get(0), MyClass.class).message);
      assertEquals("cat", mapper.readValue(messages.get(1), String.class));
    }
  }

  private static void pause(long milliseconds) {
    try {
      Thread.sleep(milliseconds);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Binding readiness interrupted", e);
    }
  }

  public static class MyClass {
    public String message;
  }
}
