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

package io.dapr.it.pubsub.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.CloudEvent;
import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.Metadata;
import io.dapr.client.domain.PublishEventRequest;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.utils.TypeRef;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.HashSet;
import java.util.Iterator;

import static io.dapr.it.Retry.callWithRetry;
import static io.dapr.it.TestUtils.assertThrowsDaprException;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;


@RunWith(Parameterized.class)
public class PubSubIT extends BaseIT {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final TypeRef<List<CloudEvent>> CLOUD_EVENT_LIST_TYPE_REF = new TypeRef<>() {};
  private static final TypeRef<List<CloudEvent<ConvertToLong>>> CLOUD_EVENT_LONG_LIST_TYPE_REF = new TypeRef<>() {};
  private static final TypeRef<List<CloudEvent<MyObject>>> CLOUD_EVENT_MYOBJECT_LIST_TYPE_REF = new TypeRef<>() {};

  //Number of messages to be sent: 10
  private static final int NUM_MESSAGES = 10;

  private static final String PUBSUB_NAME = "messagebus";
  //The title of the topic to be used for publishing
  private static final String TOPIC_NAME = "testingtopic";
  private static final String TYPED_TOPIC_NAME = "typedtestingtopic";
  private static final String ANOTHER_TOPIC_NAME = "anothertopic";
  // Topic used for TTL test
  private static final String TTL_TOPIC_NAME = "ttltopic";
  // Topic to test binary data
  private static final String BINARY_TOPIC_NAME = "binarytopic";

   private static final String LONG_TOPIC_NAME = "testinglongvalues";

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

    DaprObjectSerializer serializer = new DaprObjectSerializer() {
      @Override
      public byte[] serialize(Object o) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsBytes(o);
      }

      @Override
      public <T> T deserialize(byte[] data, TypeRef<T> type) throws IOException {
        return (T) OBJECT_MAPPER.readValue(data, OBJECT_MAPPER.constructType(type.getType()));
      }

      @Override
      public String getContentType() {
        return "application/json";
      }
    };

    // Send a batch of messages on one topic
    try (DaprClient client = new DaprClientBuilder().withObjectSerializer(serializer).build()) {
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

      //Publishing an object.
      MyObject object = new MyObject();
      object.setId("123");
      client.publishEvent(PUBSUB_NAME, TOPIC_NAME, object).block();
      System.out.println("Published one object.");

      client.publishEvent(PUBSUB_NAME, TYPED_TOPIC_NAME, object).block();
      System.out.println("Published another object.");

      //Publishing a single byte: Example of non-string based content published
      client.publishEvent(
          PUBSUB_NAME,
          TOPIC_NAME,
          new byte[]{1}).block();
      System.out.println("Published one byte.");

      CloudEvent cloudEvent = new CloudEvent();
      cloudEvent.setId("1234");
      cloudEvent.setData("message from cloudevent");
      cloudEvent.setSource("test");
      cloudEvent.setSpecversion("1");
      cloudEvent.setType("myevent");
      cloudEvent.setDatacontenttype("text/plain");

      //Publishing a cloud event.
      client.publishEvent(new PublishEventRequest(PUBSUB_NAME, TOPIC_NAME, cloudEvent)
          .setContentType("application/cloudevents+json")).block();
      System.out.println("Published one cloud event.");

      Thread.sleep(3000);

      callWithRetry(() -> {
        System.out.println("Checking results for topic " + TOPIC_NAME);
        // Validate text payload.
        final List<CloudEvent> messages = client.invokeMethod(
            daprRun.getAppName(),
            "messages/testingtopic",
            null,
            HttpExtension.GET,
            CLOUD_EVENT_LIST_TYPE_REF).block();
        assertEquals(13, messages.size());
        for (int i = 0; i < NUM_MESSAGES; i++) {
          final int messageId = i;
          assertTrue(messages
              .stream()
              .filter(m -> m.getData() != null)
              .map(m -> m.getData())
              .filter(m -> m.equals(String.format("This is message #%d on topic %s", messageId, TOPIC_NAME)))
              .count() == 1);
        }

        // Validate object payload.
        assertTrue(messages
            .stream()
            .filter(m -> m.getData() != null)
            .filter(m -> m.getData() instanceof LinkedHashMap)
            .map(m -> (LinkedHashMap)m.getData())
            .filter(m -> "123".equals(m.get("id")))
            .count() == 1);

        // Validate byte payload.
        assertTrue(messages
            .stream()
            .filter(m -> m.getData() != null)
            .map(m -> m.getData())
            .filter(m -> "AQ==".equals(m))
            .count() == 1);

        // Validate cloudevent payload.
        assertTrue(messages
            .stream()
            .filter(m -> m.getData() != null)
            .map(m -> m.getData())
            .filter(m -> "message from cloudevent".equals(m))
            .count() == 1);
      }, 2000);

      callWithRetry(() -> {
        System.out.println("Checking results for topic " + TYPED_TOPIC_NAME);
        // Validate object payload.
        final List<CloudEvent<MyObject>> messages = client.invokeMethod(
                daprRun.getAppName(),
                "messages/typedtestingtopic",
                null,
                HttpExtension.GET,
                CLOUD_EVENT_MYOBJECT_LIST_TYPE_REF).block();

        assertTrue(messages
                .stream()
                .filter(m -> m.getData() != null)
                .filter(m -> m.getData() instanceof MyObject)
                .map(m -> (MyObject)m.getData())
                .filter(m -> "123".equals(m.getId()))
                .count() == 1);
      }, 2000);

      callWithRetry(() -> {
        System.out.println("Checking results for topic " + ANOTHER_TOPIC_NAME);
        final List<CloudEvent> messages = client.invokeMethod(
            daprRun.getAppName(),
            "messages/anothertopic",
            null,
            HttpExtension.GET,
            CLOUD_EVENT_LIST_TYPE_REF).block();
        assertEquals(10, messages.size());

        for (int i = 0; i < NUM_MESSAGES; i++) {
          final int messageId = i;
          assertTrue(messages
              .stream()
              .filter(m -> m.getData() != null)
              .map(m -> m.getData())
              .filter(m -> m.equals(String.format("This is message #%d on topic %s", messageId, ANOTHER_TOPIC_NAME)))
              .count() == 1);
        }
      }, 2000);
    }
  }

  @Test
  public void testPubSubBinary() throws Exception {
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

    DaprObjectSerializer serializer = new DaprObjectSerializer() {
      @Override
      public byte[] serialize(Object o) {
        return (byte[])o;
      }

      @Override
      public <T> T deserialize(byte[] data, TypeRef<T> type) {
        return (T) data;
      }

      @Override
      public String getContentType() {
        return "application/octet-stream";
      }
    };
    try (DaprClient client = new DaprClientBuilder().withObjectSerializer(serializer).build()) {
      client.publishEvent(
          PUBSUB_NAME,
          BINARY_TOPIC_NAME,
          new byte[]{1}).block();
      System.out.println("Published one byte.");
    }

    Thread.sleep(3000);

    try (DaprClient client = new DaprClientBuilder().build()) {
      callWithRetry(() -> {
        System.out.println("Checking results for topic " + BINARY_TOPIC_NAME);
        final List<CloudEvent> messages = client.invokeMethod(
            daprRun.getAppName(),
            "messages/binarytopic",
            null,
            HttpExtension.GET, CLOUD_EVENT_LIST_TYPE_REF).block();
        assertEquals(1, messages.size());
        assertNull(messages.get(0).getData());
        assertArrayEquals(new byte[]{1}, messages.get(0).getBinaryData());
      }, 2000);
    }
  }

  @Test
  public void testPubSubTTLMetadata() throws Exception {
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

  @Test
  public void testLongValues() throws Exception {
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

    ConvertToLong toLong = new ConvertToLong();
    HashSet<ConvertToLong> expected = new HashSet<>();
    Random random = new Random();
    Long randomLong = 590518626939830271L;
    random.setSeed(randomLong);
    toLong.setValue(randomLong);
    expected.add(toLong);
    for (int i = 1; i < NUM_MESSAGES; i++) {
      ConvertToLong value = new ConvertToLong();
      randomLong = random.nextLong();
      value.setValue(randomLong);
      expected.add(value);
    }
    Iterator expectVal = expected.iterator();
    try (DaprClient client = new DaprClientBuilder().build()) {
      while(expectVal.hasNext()) {

        //Publishing messages
        client.publishEvent(
            PUBSUB_NAME,
            LONG_TOPIC_NAME,
            expectVal.next(),
            Collections.singletonMap(Metadata.TTL_IN_SECONDS, "1")).block();

        try {
          Thread.sleep((long) (1000 * Math.random()));
        } catch (InterruptedException e) {
          e.printStackTrace();
          Thread.currentThread().interrupt();
          return;
        }
      }
    }

    HashSet<ConvertToLong> actual = new HashSet<>();
    try (DaprClient client = new DaprClientBuilder().build()) {
      callWithRetry(() -> {
        System.out.println("Checking results for topic " + LONG_TOPIC_NAME);
        final List<CloudEvent<ConvertToLong>> messages = client.invokeMethod(
            daprRun.getAppName(),
            "messages/testinglongvalues",
            null,
            HttpExtension.GET, CLOUD_EVENT_LONG_LIST_TYPE_REF).block();
            assertNotNull(messages);
        for (int i = 0; i < NUM_MESSAGES; i++) { 
          actual.add(messages.get(i).getData());
        }
          assertTrue(expected.equals(actual));
      }, 2000);
    }
  }

  public static class MyObject {
    private String id;

    public String getId() {
      return this.id;
    }

    public void setId(String id) {
      this.id = id;
    }
  }

  public static class ConvertToLong {
    private Long value;

    public Long getValue() {
      return value;
    }

    public void setValue(Long value) {
      this.value = value;
    }

    public boolean equals(Object object) {
      if (this == object) return true;
      if (!(object instanceof ConvertToLong)) return false;
      if (!super.equals(object)) return false;
      ConvertToLong that = (ConvertToLong) object;
      return java.util.Objects.equals(getValue(), that.getValue());
    }

    public int hashCode() {
      return java.util.Objects.hash(super.hashCode(), getValue());
    }
  }

}
