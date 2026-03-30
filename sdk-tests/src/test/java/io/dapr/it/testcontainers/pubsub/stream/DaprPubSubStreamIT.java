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
 * limitations under the License.
 */
package io.dapr.it.testcontainers.pubsub.stream;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprPreviewClient;
import io.dapr.config.Properties;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import io.dapr.utils.TypeRef;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.Disposable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static io.dapr.it.Retry.callWithRetry;
import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Integration test for streaming subscription stop/restart behavior with RabbitMQ.
 *
 * <p>Reproduces <a href="https://github.com/dapr/java-sdk/issues/1701">#1701</a>:
 * When a streaming subscription is stopped and restarted, the Dapr sidecar reuses the
 * topic name as the RabbitMQ consumer tag. If the previous consumer hasn't been fully
 * cleaned up, RabbitMQ rejects the duplicate tag with a connection-level error (504)
 * that kills ALL consumers on that connection — not just the one being restarted.</p>
 */
@Testcontainers
@Tag("testcontainers")
public class DaprPubSubStreamIT {

  private static final Logger LOG = LoggerFactory.getLogger(DaprPubSubStreamIT.class);
  private static final Network DAPR_NETWORK = Network.newNetwork();
  private static final String PUBSUB_NAME = "pubsub";
  private static final int NUM_MESSAGES = 10;

  @Container
  private static final RabbitMQContainer RABBITMQ = new RabbitMQContainer(
      DockerImageName.parse("rabbitmq:3.7.25-management-alpine"))
      .withExposedPorts(5672)
      .withNetworkAliases("rabbitmq")
      .withNetwork(DAPR_NETWORK);

  @Container
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
      .withAppName("stream-test-app")
      .withNetwork(DAPR_NETWORK)
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withLogConsumer(outputFrame -> LOG.info(outputFrame.getUtf8String()))
      .withComponent(new Component(PUBSUB_NAME, "pubsub.rabbitmq", "v1", Map.of(
          "connectionString", "amqp://guest:guest@rabbitmq:5672",
          "user", "guest",
          "password", "guest"
      )))
      .dependsOn(RABBITMQ);

  private DaprClientBuilder createClientBuilder() {
    return new DaprClientBuilder()
        .withPropertyOverride(Properties.HTTP_ENDPOINT, "http://localhost:" + DAPR_CONTAINER.getHttpPort())
        .withPropertyOverride(Properties.GRPC_ENDPOINT, "http://localhost:" + DAPR_CONTAINER.getGrpcPort());
  }

  /**
   * Verifies that stopping and restarting a streaming subscription does not
   * disrupt other active streaming subscriptions.
   *
   * <p>Steps:
   * <ol>
   *   <li>Start streaming subscriptions on topic-one and topic-two</li>
   *   <li>Publish messages and verify both subscriptions receive them</li>
   *   <li>Start a subscription on topic-three, then stop it</li>
   *   <li>Restart the subscription on topic-three</li>
   *   <li>Publish more messages to topic-one and topic-two</li>
   *   <li>Verify topic-one and topic-two still receive messages without errors</li>
   * </ol>
   */
  @Test
  public void restartingSubscriptionShouldNotDisruptOtherSubscriptions() throws Exception {
    var topicOne = "stream-topic-one";
    var topicTwo = "stream-topic-two";
    var topicThree = "stream-topic-three";
    var runId = UUID.randomUUID().toString();

    try (DaprClient client = createClientBuilder().build();
         DaprPreviewClient previewClient = (DaprPreviewClient) client) {

      Set<String> topicOneMessages = Collections.synchronizedSet(new HashSet<>());
      Set<String> topicTwoMessages = Collections.synchronizedSet(new HashSet<>());
      AtomicReference<Throwable> topicOneError = new AtomicReference<>();
      AtomicReference<Throwable> topicTwoError = new AtomicReference<>();

      // Step 1: Start streaming subscriptions on topic-one and topic-two
      Disposable sub1 = previewClient.subscribeToTopic(PUBSUB_NAME, topicOne, TypeRef.STRING)
          .doOnNext(msg -> {
            if (msg.contains(runId)) {
              topicOneMessages.add(msg);
              LOG.info("topic-one received: {}", msg);
            }
          })
          .doOnError(e -> {
            LOG.error("topic-one error: {}", e.getMessage());
            topicOneError.set(e);
          })
          .subscribe();

      Disposable sub2 = previewClient.subscribeToTopic(PUBSUB_NAME, topicTwo, TypeRef.STRING)
          .doOnNext(msg -> {
            if (msg.contains(runId)) {
              topicTwoMessages.add(msg);
              LOG.info("topic-two received: {}", msg);
            }
          })
          .doOnError(e -> {
            LOG.error("topic-two error: {}", e.getMessage());
            topicTwoError.set(e);
          })
          .subscribe();

      // Give subscriptions time to establish
      Thread.sleep(2000);

      // Step 2: Publish messages and verify both receive them
      for (int i = 0; i < NUM_MESSAGES; i++) {
        client.publishEvent(PUBSUB_NAME, topicOne, String.format("pre-%s-%d", runId, i)).block();
        client.publishEvent(PUBSUB_NAME, topicTwo, String.format("pre-%s-%d", runId, i)).block();
      }

      callWithRetry(() -> {
        LOG.info("topic-one has {} messages, topic-two has {} messages",
            topicOneMessages.size(), topicTwoMessages.size());
        assertEquals(NUM_MESSAGES, topicOneMessages.size(), "topic-one should receive all pre-restart messages");
        assertEquals(NUM_MESSAGES, topicTwoMessages.size(), "topic-two should receive all pre-restart messages");
      }, 30000);

      // Step 3: Start and stop a subscription on topic-three
      Disposable sub3 = previewClient.subscribeToTopic(PUBSUB_NAME, topicThree, TypeRef.STRING)
          .subscribe();
      Thread.sleep(2000);
      sub3.dispose();
      LOG.info("Disposed topic-three subscription");
      Thread.sleep(2000);

      // Step 4: Restart the subscription on topic-three
      Set<String> topicThreeMessages = Collections.synchronizedSet(new HashSet<>());
      Disposable sub3Restarted = previewClient.subscribeToTopic(PUBSUB_NAME, topicThree, TypeRef.STRING)
          .doOnNext(msg -> {
            if (msg.contains(runId)) {
              topicThreeMessages.add(msg);
              LOG.info("topic-three received: {}", msg);
            }
          })
          .subscribe();
      Thread.sleep(2000);

      // Step 5: Publish more messages to all topics
      topicOneMessages.clear();
      topicTwoMessages.clear();

      for (int i = 0; i < NUM_MESSAGES; i++) {
        client.publishEvent(PUBSUB_NAME, topicOne, String.format("post-%s-%d", runId, i)).block();
        client.publishEvent(PUBSUB_NAME, topicTwo, String.format("post-%s-%d", runId, i)).block();
        client.publishEvent(PUBSUB_NAME, topicThree, String.format("post-%s-%d", runId, i)).block();
      }

      // Step 6: Verify topic-one and topic-two still work after topic-three was restarted
      callWithRetry(() -> {
        LOG.info("Post-restart: topic-one has {} messages, topic-two has {} messages, topic-three has {} messages",
            topicOneMessages.size(), topicTwoMessages.size(), topicThreeMessages.size());
        assertEquals(NUM_MESSAGES, topicOneMessages.size(),
            "topic-one should still receive messages after topic-three restart");
        assertEquals(NUM_MESSAGES, topicTwoMessages.size(),
            "topic-two should still receive messages after topic-three restart");
        assertEquals(NUM_MESSAGES, topicThreeMessages.size(),
            "topic-three should receive messages after restart");
      }, 30000);

      // Verify no errors occurred on the active subscriptions
      assertNull(topicOneError.get(),
          "topic-one should not have received any errors, but got: " + topicOneError.get());
      assertNull(topicTwoError.get(),
          "topic-two should not have received any errors, but got: " + topicTwoError.get());

      // Cleanup
      sub1.dispose();
      sub2.dispose();
      sub3Restarted.dispose();
    }
  }
}
