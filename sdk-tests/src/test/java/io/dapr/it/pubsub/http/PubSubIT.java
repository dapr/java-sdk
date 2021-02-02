/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.pubsub.http;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.Metadata;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static io.dapr.it.Retry.callWithRetry;
import static io.dapr.it.TestUtils.assertThrowsDaprException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class PubSubIT extends BaseIT {

  //Number of messages to be sent: 10
  private static final int NUM_MESSAGES = 10;

  private static final String PUBSUB_NAME = "messagebus";
  //The title of the topic to be used for publishing
  private static final String TOPIC_NAME = "testingtopic";
  private static final String ANOTHER_TOPIC_NAME = "anothertopic";
  // Topic used for TTL test
  private static final String TTL_TOPIC_NAME = "ttltopic";

  /**
   * Parameters for this test.
   * Param #1: useGrpc.
   *
   * @return Collection of parameter tuples.
   */
  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{{false}, {true}});
  }

  @Parameterized.Parameter
  public boolean useGrpc;

  private final List<DaprRun> runs = new ArrayList<>();

  private DaprRun closeLater(DaprRun run) {
    this.runs.add(run);
    return run;
  }

  @After
  public void tearDown() throws Exception {
    for (DaprRun run : runs) {
      run.stop();
    }
  }

  @Test
  public void publishPubSubNotFound() throws Exception {
    DaprRun daprRun = closeLater(startDaprApp(
        this.getClass().getSimpleName(),
        60000));
    if (this.useGrpc) {
      daprRun.switchToGRPC();
    } else {
      daprRun.switchToHTTP();
    }

    try (DaprClient client = new DaprClientBuilder().build()) {

      if (this.useGrpc) {
        assertThrowsDaprException(
            "INVALID_ARGUMENT",
            "INVALID_ARGUMENT: pubsub unknown pubsub not found",
            () -> client.publishEvent("unknown pubsub", "mytopic", "payload").block());
      } else {
        assertThrowsDaprException(
            "ERR_PUBSUB_NOT_FOUND",
            "ERR_PUBSUB_NOT_FOUND: pubsub unknown pubsub not found",
            () -> client.publishEvent("unknown pubsub", "mytopic", "payload").block());
      }
    }
  }

  @Test
  public void testPubSub() throws Exception {
    System.out.println("Working Directory = " + System.getProperty("user.dir"));

    final DaprRun daprRun = closeLater(startDaprApp(
        this.getClass().getSimpleName(),
        SubscriberService.SUCCESS_MESSAGE,
        SubscriberService.class,
        true,
        60000));
    // At this point, it is guaranteed that the service above is running and all ports being listened to.
    if (this.useGrpc) {
      daprRun.switchToGRPC();
    } else {
      daprRun.switchToHTTP();
    }

    // Send a batch of messages on one topic
    try (DaprClient client = new DaprClientBuilder().build()) {
      for (int i = 0; i < NUM_MESSAGES; i++) {
        String message = String.format("This is message #%d on topic %s", i, TOPIC_NAME);
        //Publishing messages
        client.publishEvent(PUBSUB_NAME, TOPIC_NAME, message).block();
        System.out.println(String.format("Published message: '%s' to topic '%s' pubsub_name '%s'", message, TOPIC_NAME, PUBSUB_NAME));
      }

      // Send a batch of different messages on the other.
      for (int i = 0; i < NUM_MESSAGES; i++) {
        String message = String.format("This is message #%d on topic %s", i, ANOTHER_TOPIC_NAME);
        //Publishing messages
        client.publishEvent(PUBSUB_NAME, ANOTHER_TOPIC_NAME, message).block();
        System.out.println(String.format("Published message: '%s' to topic '%s' pubsub_name '%s'", message, ANOTHER_TOPIC_NAME, PUBSUB_NAME));
      }

      //Publishing a single byte: Example of non-string based content published
      client.publishEvent(
          PUBSUB_NAME,
          TOPIC_NAME,
          new byte[]{1}).block();
      System.out.println("Published one byte.");

      Thread.sleep(3000);

      callWithRetry(() -> {
        System.out.println("Checking results for topic " + TOPIC_NAME);
        final List<String> messages = client.invokeMethod(daprRun.getAppName(), "messages/testingtopic", null, HttpExtension.GET, List.class).block();
        assertEquals(11, messages.size());
        for (int i = 0; i < NUM_MESSAGES; i++) {
          assertTrue(messages.toString(), messages.contains(String.format("This is message #%d on topic %s", i, TOPIC_NAME)));
        }

        boolean foundByte = false;
        for (String message : messages) {
          if ((message.getBytes().length == 1) && (message.getBytes()[0] == 1)) {
            foundByte = true;
          }
        }
        assertTrue(foundByte);

      }, 2000);

      callWithRetry(() -> {
        System.out.println("Checking results for topic " + ANOTHER_TOPIC_NAME);
        final List<String> messages = client.invokeMethod(daprRun.getAppName(), "messages/anothertopic", null, HttpExtension.GET, List.class).block();
        assertEquals(10, messages.size());

        for (int i = 0; i < NUM_MESSAGES; i++) {
          assertTrue(messages.contains(String.format("This is message #%d on topic %s", i, ANOTHER_TOPIC_NAME)));
        }
      }, 2000);
    }
  }

  @Test
  public void testPubSubTTLMetadata() throws Exception {
    System.out.println("Working Directory = " + System.getProperty("user.dir"));

    DaprRun daprRun = closeLater(startDaprApp(
        this.getClass().getSimpleName(),
        60000));
    if (this.useGrpc) {
      daprRun.switchToGRPC();
    } else {
      daprRun.switchToHTTP();
    }

    // Send a batch of messages on one topic, all to be expired in 1 second.
    try (DaprClient client = new DaprClientBuilder().build()) {
      for (int i = 0; i < NUM_MESSAGES; i++) {
        String message = String.format("This is message #%d on topic %s", i, TTL_TOPIC_NAME);
        //Publishing messages
        client.publishEvent(
            PUBSUB_NAME,
            TTL_TOPIC_NAME,
            message,
            Collections.singletonMap(Metadata.TTL_IN_SECONDS, "1")).block();
        System.out.println(String.format("Published message: '%s' to topic '%s' pubsub_name '%s'", message, TOPIC_NAME, PUBSUB_NAME));
      }
    }

    daprRun.stop();

    // Sleeps for two seconds to let them expire.
    Thread.sleep(2000);

    daprRun = closeLater(startDaprApp(
        this.getClass().getSimpleName(),
        SubscriberService.SUCCESS_MESSAGE,
        SubscriberService.class,
        true,
        60000));
    if (this.useGrpc) {
      daprRun.switchToGRPC();
    } else {
      daprRun.switchToHTTP();
    }

    // Sleeps for five seconds to give subscriber a chance to receive messages.
    Thread.sleep(5000);

    final String appId = daprRun.getAppName();
    try (DaprClient client = new DaprClientBuilder().build()) {
      callWithRetry(() -> {
        System.out.println("Checking results for topic " + TTL_TOPIC_NAME);
        final List<String> messages = client.invokeMethod(appId, "messages/" + TTL_TOPIC_NAME, null, HttpExtension.GET, List.class).block();
        assertEquals(0, messages.size());
      }, 2000);
    }

    daprRun.stop();
  }
}
