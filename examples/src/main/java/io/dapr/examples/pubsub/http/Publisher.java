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
 * dapr run --app-id publisher --port 3006 -- mvn exec:java -pl=examples -Dexec.mainClass=io.dapr.examples.pubsub.http.Publisher
 */
public class Publisher {

  private static final int NUM_MESSAGES = 10;

  private static final String TOPIC_NAME = "message";

  public static void main(String[] args) throws Exception {
    DaprClient client = new DaprClientBuilder(null).build();
    for (int i = 0; i < NUM_MESSAGES; i++) {
      String message = String.format("This is message #%d", i);
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

    client.publishEvent(
        TOPIC_NAME,
        new byte[] { 1 },
        Collections.singletonMap("content-type", "application/octet-stream")).block();
    System.out.println("Published one byte.");

    System.out.println("Done.");
  }
}
