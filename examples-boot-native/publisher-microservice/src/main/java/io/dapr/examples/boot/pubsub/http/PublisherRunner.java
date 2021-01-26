/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.examples.boot.pubsub.http;

import io.dapr.client.DaprClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonMap;

@Component
public class PublisherRunner implements CommandLineRunner {
  //Number of messages to be sent: 10
  private static final int NUM_MESSAGES = 10;

  //Time-to-live for messages published.
  private static final String MESSAGE_TTL_IN_SECONDS = "1000";

  //The name of topic
  public static final String TOPIC_NAME = "testingtopic";

  //The name of the pubsub
  public static final String PUBSUB_NAME = "pubsub";

  @Autowired
  private DaprClient client;

  @Override
  public void run(String... args) throws Exception {
    TimeUnit.SECONDS.sleep(5);

    //Creating the DaprClient: Using the default builder client produces an HTTP Dapr Client
    for (int i = 0; i < NUM_MESSAGES; i++) {
      String message = String.format("This is message #%d", i);
      //Publishing messages
      client.publishEvent(PUBSUB_NAME, TOPIC_NAME, message,
          singletonMap("ttlInSeconds", MESSAGE_TTL_IN_SECONDS)).block();
      System.out.println("Published message: " + message);

      try {
        Thread.sleep((long) (1000 * Math.random()));
      } catch (InterruptedException e) {
        e.printStackTrace();
        Thread.currentThread().interrupt();
        return;
      }
    }
  }
}
