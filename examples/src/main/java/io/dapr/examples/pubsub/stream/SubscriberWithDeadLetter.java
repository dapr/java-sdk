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

package io.dapr.examples.pubsub.stream;

import io.dapr.client.DaprClientBuilder;
import io.dapr.client.SubscriptionListener;
import io.dapr.client.domain.CloudEvent;
import io.dapr.utils.TypeRef;
import reactor.core.publisher.Mono;

/**
 * Subscriber using bi-directional gRPC streaming with a dead-letter topic.
 *
 * <p>This example demonstrates how to forward failed messages to a dead-letter topic.
 * The main listener returns {@code Status.DROP} for messages whose payload contains
 * "fail", which causes the Dapr runtime to publish them to the configured
 * dead-letter topic. A second subscription consumes the dead-letter topic so the
 * forwarded messages are visible in the same process.
 *
 * <p>Usage:
 * <ol>
 *   <li>Build and install jars: {@code mvn clean install}
 *   <li>cd [repo root]/examples
 *   <li>Run the subscriber:
 *       {@code dapr run --resources-path ./components/pubsub --app-id subscriber -- \
 *         java -jar target/dapr-java-sdk-examples-exec.jar \
 *         io.dapr.examples.pubsub.stream.SubscriberWithDeadLetter}
 *   <li>Publish messages from another terminal, e.g. containing the word "fail" to
 *       see them routed to the dead-letter topic.
 * </ol>
 */
public class SubscriberWithDeadLetter {

  private static final String DEFAULT_TOPIC_NAME = "testingtopic";
  private static final String DEFAULT_DEAD_LETTER_TOPIC_NAME = "testingtopic-deadletter";
  private static final String PUBSUB_NAME = "messagebus";

  /**
   * Main entry point for the dead-letter subscriber example.
   *
   * @param args Optional positional args: [topicName] [deadLetterTopicName].
   * @throws Exception An Exception on startup.
   */
  public static void main(String[] args) throws Exception {
    String topicName = args.length >= 1 ? args[0] : DEFAULT_TOPIC_NAME;
    String deadLetterTopicName = args.length >= 2 ? args[1] : DEFAULT_DEAD_LETTER_TOPIC_NAME;

    try (var client = new DaprClientBuilder().buildPreviewClient()) {
      System.out.println("Subscribing to dead-letter topic: " + deadLetterTopicName);

      var deadLetterListener = new SubscriptionListener<String>() {
        @Override
        public Mono<Status> onEvent(CloudEvent<String> event) {
          System.out.println("Dead-letter subscriber got: " + event.getData());
          return Mono.just(Status.SUCCESS);
        }

        @Override
        public void onError(RuntimeException exception) {
          System.out.println("Dead-letter subscriber got exception: " + exception.getMessage());
        }
      };

      System.out.println("Subscribing to topic: " + topicName
          + " (dead-letter topic: " + deadLetterTopicName + ")");

      var mainListener = new SubscriptionListener<String>() {
        @Override
        public Mono<Status> onEvent(CloudEvent<String> event) {
          String data = event.getData();
          if (data != null && data.toLowerCase().contains("fail")) {
            System.out.println("Subscriber dropping message to dead-letter: " + data);
            return Mono.just(Status.DROP);
          }
          System.out.println("Subscriber got: " + data);
          return Mono.just(Status.SUCCESS);
        }

        @Override
        public void onError(RuntimeException exception) {
          System.out.println("Subscriber got exception: " + exception.getMessage());
        }
      };

      try (var deadLetterSubscription = client.subscribeToEvents(
              PUBSUB_NAME, deadLetterTopicName, deadLetterListener, TypeRef.STRING);
           var mainSubscription = client.subscribeToEvents(
              PUBSUB_NAME, topicName, deadLetterTopicName, mainListener, TypeRef.STRING)) {
        mainSubscription.awaitTermination();
      }
    }
  }
}
