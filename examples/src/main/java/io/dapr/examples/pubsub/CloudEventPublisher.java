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

package io.dapr.examples.pubsub;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.CloudEvent;
import io.dapr.client.domain.Metadata;
import io.dapr.client.domain.PublishEventRequest;

import java.util.UUID;

import static java.util.Collections.singletonMap;

/**
 * Message publisher.
 * 1. Build and install jars:
 * mvn clean install
 * 2. cd [repo root]/examples
 * 3. Run the program:
 * dapr run --components-path ./components/pubsub --app-id publisher -- \
 *   java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.pubsub.CloudEventPublisher
 */
public class CloudEventPublisher {

  //Number of messages to be sent.
  private static final int NUM_MESSAGES = 10;

  //Time-to-live for messages published.
  private static final String MESSAGE_TTL_IN_SECONDS = "1000";

  //The title of the topic to be used for publishing
  private static final String TOPIC_NAME = "testingtopic";

  //The name of the pubsub
  private static final String PUBSUB_NAME = "messagebus";

  /**
   * This is the entry point of the publisher app example.
   * @param args Args, unused.
   * @throws Exception A startup Exception.
   */
  public static void main(String[] args) throws Exception {
    //Creating the DaprClient: Using the default builder client produces an HTTP Dapr Client
    try (DaprClient client = new DaprClientBuilder().build()) {
      for (int i = 0; i < NUM_MESSAGES; i++) {
        CloudEvent<String> cloudEvent = new CloudEvent<>();
        cloudEvent.setId(UUID.randomUUID().toString());
        cloudEvent.setType("example");
        cloudEvent.setSpecversion("1");
        cloudEvent.setDatacontenttype("text/plain");
        cloudEvent.setData(String.format("This is message #%d", i));

        //Publishing messages
        client.publishEvent(
            new PublishEventRequest(PUBSUB_NAME, TOPIC_NAME, cloudEvent)
                .setContentType(CloudEvent.CONTENT_TYPE)
                .setMetadata(singletonMap(Metadata.TTL_IN_SECONDS, MESSAGE_TTL_IN_SECONDS))).block();
        System.out.println("Published cloud event with message: " + cloudEvent.getData());

        try {
          Thread.sleep((long) (1000 * Math.random()));
        } catch (InterruptedException e) {
          e.printStackTrace();
          Thread.currentThread().interrupt();
          return;
        }
      }

      // This is an example, so for simplicity we are just exiting here.
      // Normally a dapr app would be a web service and not exit main.
      System.out.println("Done.");
    }
  }
}
