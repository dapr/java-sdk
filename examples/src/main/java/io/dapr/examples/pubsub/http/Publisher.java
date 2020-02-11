/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.examples.pubsub.http;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;

import java.util.Collections;

/**
 * Message publisher.
 * 1. Build and install jars:
 * mvn clean install
 * 2. Run the program:
 * dapr run --app-id publisher --port 3006 -- \
 *  java -jar examples/target/dapr-sdk-examples-0.2.0-SNAPSHOT-exec.jar io.dapr.examples.pubsub.http.Publisher
 */
public class Publisher {

  //Number of messages to be sent: 10
  private static final int NUM_MESSAGES = 10;
  //The title of the topic to be used for publishing
  private static final String TOPIC_NAME = "testingtopic";

  /**
   * This is the entry point of the publisher app example.
   * @param args Args, unused.
   * @throws Exception A startup Exception.
   */
  public static void main(String[] args) throws Exception {
    //Creating the DaprClient: Using the default builder client produces an HTTP Dapr Client
    DaprClient client = new DaprClientBuilder().build();
    for (int i = 0; i < NUM_MESSAGES; i++) {
      String message = String.format("This is message #%d", i);
      //Publishing messages
      client.publishEvent(TOPIC_NAME, message).block();
      System.out.println("Published message: " + message);

      try {
        Thread.sleep((long)(1000 * Math.random()));
      } catch (InterruptedException e) {
        e.printStackTrace();
        Thread.currentThread().interrupt();
        return;
      }
    }

    //Publishing a single bite: Example of non-string based content published
    client.publishEvent(
            TOPIC_NAME,
            new byte[] { 1 },
            Collections.singletonMap("content-type", "application/octet-stream")).block();
    System.out.println("Published one byte.");

    System.out.println("Done.");
  }
}
