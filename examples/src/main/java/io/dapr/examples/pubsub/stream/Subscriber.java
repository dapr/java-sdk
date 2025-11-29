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
import io.dapr.client.SubscriptionListener;
import io.dapr.client.domain.CloudEvent;
import io.dapr.utils.TypeRef;
import reactor.core.publisher.Mono;

/**
 * Subscriber using bi-directional gRPC streaming, which does not require an app port.
 * 1. Build and install jars:
 * mvn clean install
 * 2. cd [repo root]/examples
 * 3. Run the subscriber:
 * dapr run --resources-path ./components/pubsub --app-id subscriber -- \
 *   java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.pubsub.stream.Subscriber
 */
public class Subscriber {

  //The title of the topic to be used for publishing
  private static final String DEFAULT_TOPIC_NAME = "testingtopic";

  //The name of the pubsub
  private static final String PUBSUB_NAME = "messagebus";

  /**
   * This is the entry point for this example app, which subscribes to a topic.
   * @param args Used to optionally pass a topic name.
   * @throws Exception An Exception on startup.
   */
  public static void main(String[] args) throws Exception {
    String topicName = getTopicName(args);
    try (var client = new DaprClientBuilder().build()) {
      var subscription = client.subscribeToEvents(
          PUBSUB_NAME,
          topicName,
          new SubscriptionListener<>() {

            @Override
            public Mono<Status> onEvent(CloudEvent<String> event) {
              System.out.println("Subscriber got: " + event.getData());
              return Mono.just(Status.SUCCESS);
            }

            @Override
            public void onError(RuntimeException exception) {
              System.out.println("Subscriber got exception: " + exception.getMessage());
            }
          },
          TypeRef.STRING);

      subscription.awaitTermination();
    }
  }

  /**
   * If a topic is specified in args, use that.
   * Else, fallback to the default topic.
   * @param args program arguments
   * @return name of the topic to publish messages to.
   */
  private static String getTopicName(String[] args) {
    if (args.length >= 1) {
      return args[0];
    }
    return DEFAULT_TOPIC_NAME;
  }
}
