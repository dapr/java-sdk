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
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import io.dapr.utils.TypeRef;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static io.dapr.it.Retry.callWithRetry;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class PubSubStreamIT extends BaseIT {

  // Must be a large enough number, so we validate that we get more than the initial batch
  // sent by the runtime. When this was first added, the batch size in runtime was set to 10.
  private static final int NUM_MESSAGES = 100;
  private static final String TOPIC_NAME = "stream-topic";
  private static final String PUBSUB_NAME = "messagebus";

  private final List<DaprRun> runs = new ArrayList<>();

  private DaprRun closeLater(DaprRun run) {
    this.runs.add(run);
    return run;
  }

  @AfterEach
  public void tearDown() throws Exception {
    for (DaprRun run : runs) {
      run.stop();
    }
  }

  @Test
  public void testPubSub() throws Exception {
    final DaprRun daprRun = closeLater(startDaprApp(
        this.getClass().getSimpleName(),
        60000));

    var runId = UUID.randomUUID().toString();
    try (DaprClient client = daprRun.newDaprClient();
         DaprPreviewClient previewClient = daprRun.newDaprPreviewClient()) {
      for (int i = 0; i < NUM_MESSAGES; i++) {
        String message = String.format("This is message #%d on topic %s for run %s", i, TOPIC_NAME, runId);
        //Publishing messages
        client.publishEvent(PUBSUB_NAME, TOPIC_NAME, message).block();
        System.out.println(
            String.format("Published message: '%s' to topic '%s' pubsub_name '%s'", message, TOPIC_NAME, PUBSUB_NAME));
      }

      System.out.println("Starting subscription for " + TOPIC_NAME);

      Set<String> messages = Collections.synchronizedSet(new HashSet<>());
      Set<String> errors = Collections.synchronizedSet(new HashSet<>());

      var random = new Random(37);  // predictable random.
      var listener = new SubscriptionListener<String>() {
        @Override
        public Mono<Status> onEvent(CloudEvent<String> event) {
          return Mono.fromCallable(() -> {
            // Useful to avoid false negatives running locally multiple times.
            if (event.getData().contains(runId)) {
              // 5% failure rate.
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
      try(var subscription = previewClient.subscribeToEvents(PUBSUB_NAME, TOPIC_NAME, listener, TypeRef.STRING)) {
        callWithRetry(() -> {
          var messageCount =  messages.size();
          System.out.println(
              String.format("Got %d messages out of %d for topic %s.", messageCount, NUM_MESSAGES, TOPIC_NAME));
          assertEquals(NUM_MESSAGES, messages.size());
          assertEquals(4, errors.size());
        }, 120000); // Time for runtime to retry messages.

        subscription.close();
        subscription.awaitTermination();
      }
    }
  }

  @Test
  public void testPubSubRawData() throws Exception {
    final DaprRun daprRun = closeLater(startDaprApp(
        this.getClass().getSimpleName() + "-rawdata",
        60000));

    var runId = UUID.randomUUID().toString();
    try (DaprClient client = daprRun.newDaprClient();
         DaprPreviewClient previewClient = daprRun.newDaprPreviewClient()) {

      // Publish messages
      for (int i = 0; i < NUM_MESSAGES; i++) {
        String message = String.format("Raw message #%d for run %s", i, runId);
        client.publishEvent(PUBSUB_NAME, TOPIC_NAME, message).block();
        System.out.println(
            String.format("Published raw message: '%s' to topic '%s'", message, TOPIC_NAME));
      }

      System.out.println("Starting raw data subscription for " + TOPIC_NAME);

      Set<String> messages = Collections.synchronizedSet(new HashSet<>());

      // Use new subscribeToEventsData - receives String directly, not CloudEvent<String>
      var disposable = previewClient.subscribeToEventsData(PUBSUB_NAME, TOPIC_NAME, TypeRef.STRING)
          .doOnNext(rawMessage -> {
            // rawMessage is String directly
            if (rawMessage.contains(runId)) {
              messages.add(rawMessage);
              System.out.println("Received raw message: " + rawMessage);
            }
          })
          .subscribe();

      callWithRetry(() -> {
        var messageCount = messages.size();
        System.out.println(
            String.format("Got %d raw messages out of %d for topic %s.", messageCount, NUM_MESSAGES, TOPIC_NAME));
        assertEquals(NUM_MESSAGES, messages.size());
      }, 60000);

      disposable.dispose();
    }
  }
}
