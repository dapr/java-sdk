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
package io.dapr.it.testcontainers.pubsub.stream;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.SubscriptionListener;
import io.dapr.client.domain.CloudEvent;
import io.dapr.it.testcontainers.DaprClientFactory;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.utils.TypeRef;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.dapr.it.Retry.callWithRetry;
import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@Tag("testcontainers")
public class DaprPubSubStreamIT {

  private static final int NUM_MESSAGES = 100;
  private static final String TOPIC_NAME = "stream-topic";
  private static final String TOPIC_NAME_FLUX = "stream-topic-flux";
  private static final String TOPIC_NAME_CLOUDEVENT = "stream-topic-cloudevent";
  private static final String TOPIC_NAME_RAWPAYLOAD = "stream-topic-rawpayload";
  private static final String PUBSUB_NAME = "pubsub";

  @Container
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
      .withAppName("pubsub-stream-app")
      .withComponent(new Component(PUBSUB_NAME, "pubsub.in-memory", "v1", Collections.emptyMap()));

  private void waitForSubscription(DaprClient client, String topic, CountDownLatch latch) throws InterruptedException {
    callWithRetry(() -> {
      client.publishEvent(PUBSUB_NAME, topic, "probe").block();
      try {
        assertTrue(latch.await(500, TimeUnit.MILLISECONDS), "Subscription not ready for " + topic);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
    }, 60000);
  }

  @Test
  public void testPubSub() throws Exception {
    var runId = UUID.randomUUID().toString();
    try (DaprClient client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build();
         DaprPreviewClient previewClient = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER)
             .buildPreviewClient()) {

      Set<String> received = Collections.synchronizedSet(new HashSet<>());
      CountDownLatch ready = new CountDownLatch(1);

      var listener = new SubscriptionListener<String>() {
        @Override
        public Mono<Status> onEvent(CloudEvent<String> event) {
          return Mono.fromCallable(() -> {
            ready.countDown();
            if (event.getData().contains(runId)) {
              received.add(event.getId());
              return Status.SUCCESS;
            }
            return Status.DROP;
          });
        }

        @Override
        public void onError(RuntimeException exception) {
        }
      };

      try (var subscription = previewClient.subscribeToEvents(PUBSUB_NAME, TOPIC_NAME, listener, TypeRef.STRING)) {
        waitForSubscription(client, TOPIC_NAME, ready);

        for (int i = 0; i < NUM_MESSAGES; i++) {
          String message = String.format("This is message #%d on topic %s for run %s", i, TOPIC_NAME, runId);
          client.publishEvent(PUBSUB_NAME, TOPIC_NAME, message).block();
        }

        callWithRetry(() -> {
          assertEquals(NUM_MESSAGES, received.size(),
              String.format("Got %d/%d messages for topic %s", received.size(), NUM_MESSAGES, TOPIC_NAME));
        }, 120000);
      }
    }
  }

  @Test
  public void testPubSubFlux() throws Exception {
    var runId = UUID.randomUUID().toString();
    try (DaprClient client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build();
         DaprPreviewClient previewClient = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER)
             .buildPreviewClient()) {

      Set<String> received = Collections.synchronizedSet(new HashSet<>());
      CountDownLatch ready = new CountDownLatch(1);

      var disposable = previewClient.subscribeToTopic(PUBSUB_NAME, TOPIC_NAME_FLUX, TypeRef.STRING)
          .doOnNext(rawMessage -> {
            ready.countDown();
            if (rawMessage.contains(runId)) {
              received.add(rawMessage);
            }
          })
          .subscribe();

      waitForSubscription(client, TOPIC_NAME_FLUX, ready);

      for (int i = 0; i < NUM_MESSAGES; i++) {
        String message = String.format("Flux message #%d for run %s", i, runId);
        client.publishEvent(PUBSUB_NAME, TOPIC_NAME_FLUX, message).block();
      }

      callWithRetry(() -> {
        assertEquals(NUM_MESSAGES, received.size(),
            String.format("Got %d/%d flux messages for topic %s", received.size(), NUM_MESSAGES, TOPIC_NAME_FLUX));
      }, 60000);

      disposable.dispose();
    }
  }

  @Test
  public void testPubSubCloudEvent() throws Exception {
    var runId = UUID.randomUUID().toString();
    try (DaprClient client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build();
         DaprPreviewClient previewClient = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER)
             .buildPreviewClient()) {

      Set<String> received = Collections.synchronizedSet(new HashSet<>());
      CountDownLatch ready = new CountDownLatch(1);

      var disposable = previewClient.subscribeToTopic(
              PUBSUB_NAME, TOPIC_NAME_CLOUDEVENT, new TypeRef<CloudEvent<String>>() {})
          .doOnNext(cloudEvent -> {
            ready.countDown();
            if (cloudEvent.getData() != null && cloudEvent.getData().contains(runId)) {
              received.add(cloudEvent.getId());
            }
          })
          .subscribe();

      waitForSubscription(client, TOPIC_NAME_CLOUDEVENT, ready);

      for (int i = 0; i < NUM_MESSAGES; i++) {
        String message = String.format("CloudEvent message #%d for run %s", i, runId);
        client.publishEvent(PUBSUB_NAME, TOPIC_NAME_CLOUDEVENT, message).block();
      }

      callWithRetry(() -> {
        assertEquals(NUM_MESSAGES, received.size(),
            String.format("Got %d/%d CloudEvent messages for topic %s",
                received.size(), NUM_MESSAGES, TOPIC_NAME_CLOUDEVENT));
      }, 60000);

      disposable.dispose();
    }
  }

  @Disabled("Streaming subscription with rawPayload metadata not supported by pubsub.in-memory")
  @Test
  public void testPubSubRawPayload() throws Exception {
    var runId = UUID.randomUUID().toString();
    try (DaprClient client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build();
         DaprPreviewClient previewClient = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER)
             .buildPreviewClient()) {

      Set<String> received = Collections.synchronizedSet(new HashSet<>());
      Map<String, String> metadata = Map.of("rawPayload", "true");
      CountDownLatch ready = new CountDownLatch(1);

      var disposable = previewClient.subscribeToTopic(PUBSUB_NAME, TOPIC_NAME_RAWPAYLOAD, TypeRef.STRING, metadata)
          .doOnNext(rawMessage -> {
            ready.countDown();
            if (rawMessage.contains(runId)) {
              received.add(rawMessage);
            }
          })
          .subscribe();

      waitForSubscription(client, TOPIC_NAME_RAWPAYLOAD, ready);

      for (int i = 0; i < NUM_MESSAGES; i++) {
        String message = String.format("RawPayload message #%d for run %s", i, runId);
        client.publishEvent(PUBSUB_NAME, TOPIC_NAME_RAWPAYLOAD, message, Map.of("rawPayload", "true")).block();
      }

      callWithRetry(() -> {
        assertEquals(NUM_MESSAGES, received.size(),
            String.format("Got %d/%d raw payload messages for topic %s",
                received.size(), NUM_MESSAGES, TOPIC_NAME_RAWPAYLOAD));
      }, 60000);

      disposable.dispose();
    }
  }
}
