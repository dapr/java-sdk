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

package io.dapr.examples.pubsub.stream;

import io.dapr.client.DaprClientBuilder;
import io.dapr.utils.TypeRef;

/**
 * Subscriber using bi-directional gRPC streaming, which does not require an app port.
 *
 * <p>This example demonstrates subscribing to raw message data directly.
 * For CloudEvent subscription with metadata, see {@link SubscriberCloudEvent}.
 *
 * <p>Usage:
 * <ol>
 *   <li>Build and install jars: {@code mvn clean install}
 *   <li>cd [repo root]/examples
 *   <li>Run the subscriber:
 *       {@code dapr run --resources-path ./components/pubsub --app-id subscriber -- \
 *         java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.pubsub.stream.Subscriber}
 * </ol>
 */
public class Subscriber {

  private static final String DEFAULT_TOPIC_NAME = "testingtopic";
  private static final String PUBSUB_NAME = "messagebus";

  /**
   * Main entry point for the raw data subscriber example.
   *
   * @param args Used to optionally pass a topic name.
   * @throws Exception An Exception on startup.
   */
  public static void main(String[] args) throws Exception {
    String topicName = getTopicName(args);
    try (var client = new DaprClientBuilder().buildPreviewClient()) {
      System.out.println("Subscribing to topic: " + topicName);

      // Subscribe to events - receives raw String data directly
      client.subscribeToEvents(PUBSUB_NAME, topicName, TypeRef.STRING)
          .doOnNext(message -> {
            System.out.println("Subscriber got: " + message);
          })
          .doOnError(throwable -> {
            System.out.println("Subscriber got exception: " + throwable.getMessage());
          })
          .blockLast();
    }
  }

  private static String getTopicName(String[] args) {
    if (args.length >= 1) {
      return args[0];
    }
    return DEFAULT_TOPIC_NAME;
  }
}
