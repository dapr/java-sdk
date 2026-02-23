/*
 * Copyright 2021 The Dapr Authors
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

package io.dapr.it.pubsub.stream;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.SubscriptionListener;
import io.dapr.client.domain.CloudEvent;
import io.dapr.it.testcontainers.DaprClientFactory;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.utils.TypeRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static io.dapr.it.Retry.callWithRetry;
import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@Tag("testcontainers")
public class PubSubStreamIT {

  // Must be a large enough number, so we validate that we get more than the initial batch
  // sent by the runtime. When this was first added, the batch size in runtime was set to 10.
  private static final int NUM_MESSAGES = 100;
  private static final String TOPIC_NAME = "stream-topic";
  private static final String TOPIC_NAME_FLUX = "stream-topic-flux";
  private static final String TOPIC_NAME_CLOUDEVENT = "stream-topic-cloudevent";
  private static final String TOPIC_NAME_RAWPAYLOAD = "stream-topic-rawpayload";
  private static final String PUBSUB_NAME = "messagebus";

  private static final Network NETWORK = io.dapr.it.testcontainers.TestContainerNetworks.SHARED_NETWORK;

  @Container
  private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
      .withNetwork(NETWORK)
      .withNetworkAliases("redis");

  @Container
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
      .withNetwork(NETWORK)
      .withAppName("pubsub-stream-it")
      .dependsOn(REDIS)
      .withComponent(new Component(
          PUBSUB_NAME,
          "pubsub.redis",
          "v1",
          Map.of(
              "redisHost", "redis:6379",
              "redisPassword", "",
              "processingTimeout", "100ms",
              "redeliverInterval", "100ms")));

  @BeforeEach
  public void setup() throws Exception {
    try (DaprClient client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build()) {
      client.waitForSidecar(10000).block();
    }
    REDIS.execInContainer("redis-cli", "FLUSHALL");
  }

  @Test
  public void testPubSub() throws Exception {
    var runId = UUID.randomUUID().toString();
    try (DaprClient client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build();
         DaprPreviewClient previewClient = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).buildPreviewClient()) {
      for (int i = 0; i < NUM_MESSAGES; i++) {
        String message = String.format("This is message #%d on topic %s for run %s", i, TOPIC_NAME, runId);
        client.publishEvent(PUBSUB_NAME, TOPIC_NAME, message).block();
      }

      Set<String> messages = Collections.synchronizedSet(new HashSet<>());
      Set<String> errors = Collections.synchronizedSet(new HashSet<>());

      var random = new Random(37);  // predictable random.
      var listener = new SubscriptionListener<String>() {
        @Override
        public Mono<Status> onEvent(CloudEvent<String> event) {
          return Mono.fromCallable(() -> {
            if (event.getData().contains(runId)) {
              var decision = random.nextInt(100);
              if (decision < 5) {
                if (decision % 2 == 0) {
                  throw new RuntimeException("artificial exception on message " + event.getId());
                }
                return Status.RETRY;
              }

              messages.add(event.getId());
              return Status.SUCCESS;
            }

            return Status.DROP;
          });
        }

        @Override
        public void onError(RuntimeException exception) {
          errors.add(exception.getMessage());
        }
      };

      try (var subscription = previewClient.subscribeToEvents(PUBSUB_NAME, TOPIC_NAME, listener, TypeRef.STRING)) {
        callWithRetry(() -> {
          assertEquals(NUM_MESSAGES, messages.size());
          assertEquals(4, errors.size());
        }, 120000);

        subscription.close();
        subscription.awaitTermination();
      }
    }
  }

  @Test
  public void testPubSubFlux() throws Exception {
    var runId = UUID.randomUUID().toString();
    try (DaprClient client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build();
         DaprPreviewClient previewClient = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).buildPreviewClient()) {

      for (int i = 0; i < NUM_MESSAGES; i++) {
        String message = String.format("Flux message #%d for run %s", i, runId);
        client.publishEvent(PUBSUB_NAME, TOPIC_NAME_FLUX, message).block();
      }

      Set<String> messages = Collections.synchronizedSet(new HashSet<>());
      var disposable = previewClient.subscribeToTopic(PUBSUB_NAME, TOPIC_NAME_FLUX, TypeRef.STRING)
          .doOnNext(rawMessage -> {
            if (rawMessage.contains(runId)) {
              messages.add(rawMessage);
            }
          })
          .subscribe();

      callWithRetry(() -> assertEquals(NUM_MESSAGES, messages.size()), 60000);
      disposable.dispose();
    }
  }

  @Test
  public void testPubSubCloudEvent() throws Exception {
    var runId = UUID.randomUUID().toString();
    try (DaprClient client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build();
         DaprPreviewClient previewClient = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).buildPreviewClient()) {
      for (int i = 0; i < NUM_MESSAGES; i++) {
        String message = String.format("CloudEvent message #%d for run %s", i, runId);
        client.publishEvent(PUBSUB_NAME, TOPIC_NAME_CLOUDEVENT, message).block();
      }

      Set<String> messageIds = Collections.synchronizedSet(new HashSet<>());
      var disposable = previewClient.subscribeToTopic(PUBSUB_NAME, TOPIC_NAME_CLOUDEVENT, new TypeRef<CloudEvent<String>>() {
      }).doOnNext(cloudEvent -> {
        if (cloudEvent.getData() != null && cloudEvent.getData().contains(runId)) {
          messageIds.add(cloudEvent.getId());
        }
      }).subscribe();

      callWithRetry(() -> assertEquals(NUM_MESSAGES, messageIds.size()), 60000);
      disposable.dispose();
    }
  }

  @Test
  public void testPubSubRawPayload() throws Exception {
    var runId = UUID.randomUUID().toString();
    try (DaprClient client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build();
         DaprPreviewClient previewClient = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).buildPreviewClient()) {
      for (int i = 0; i < NUM_MESSAGES; i++) {
        String message = String.format("RawPayload message #%d for run %s", i, runId);
        client.publishEvent(PUBSUB_NAME, TOPIC_NAME_RAWPAYLOAD, message, Map.of("rawPayload", "true")).block();
      }

      Set<String> messages = Collections.synchronizedSet(new HashSet<>());
      Map<String, String> metadata = Map.of("rawPayload", "true");
      var disposable = previewClient.subscribeToTopic(PUBSUB_NAME, TOPIC_NAME_RAWPAYLOAD, TypeRef.STRING, metadata)
          .doOnNext(rawMessage -> {
            if (rawMessage.contains(runId)) {
              messages.add(rawMessage);
            }
          })
          .subscribe();

      callWithRetry(() -> assertEquals(NUM_MESSAGES, messages.size()), 60000);
      disposable.dispose();
    }
  }
}
