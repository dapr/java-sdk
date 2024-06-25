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
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.BulkPublishEntry;
import io.dapr.client.domain.BulkPublishRequest;
import io.dapr.client.domain.BulkPublishResponse;
import io.dapr.client.domain.BulkSubscribeAppResponse;
import io.dapr.client.domain.BulkSubscribeAppResponseEntry;
import io.dapr.client.domain.BulkSubscribeAppResponseStatus;
import io.dapr.client.domain.CloudEvent;
import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.Metadata;
import io.dapr.client.domain.PublishEventRequest;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.utils.TypeRef;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import static io.dapr.it.Retry.callWithRetry;
import static io.dapr.it.TestUtils.assertThrowsDaprException;
import static io.dapr.it.TestUtils.assertThrowsDaprExceptionWithReason;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


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

  private static final String TOPIC_BULK = "testingbulktopic";
  private static final String TYPED_TOPIC_NAME = "typedtestingtopic";
  private static final String ANOTHER_TOPIC_NAME = "anothertopic";
  // Topic used for TTL test
  private static final String TTL_TOPIC_NAME = "ttltopic";
  // Topic to test binary data
  private static final String BINARY_TOPIC_NAME = "binarytopic";

  private static final String LONG_TOPIC_NAME = "testinglongvalues";
  // Topic to test bulk subscribe.
  private static final String BULK_SUB_TOPIC_NAME = "topicBulkSub";

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
  public void publishPubSubNotFound() throws Exception {
    DaprRun daprRun = closeLater(startDaprApp(
        this.getClass().getSimpleName(),
        60000));

    try (DaprClient client = new DaprClientBuilder().build()) {
      assertThrowsDaprExceptionWithReason(
          "INVALID_ARGUMENT",
          "INVALID_ARGUMENT: pubsub unknown pubsub is not found",
          "DAPR_PUBSUB_NOT_FOUND",
          () -> client.publishEvent("unknown pubsub", "mytopic", "payload").block());
    }
  }

  @Test
  public void testBulkPublishPubSubNotFound() throws Exception {
    DaprRun daprRun = closeLater(startDaprApp(
        this.getClass().getSimpleName(),
        60000));

    try (DaprPreviewClient client = new DaprClientBuilder().buildPreviewClient()) {
      assertThrowsDaprException(
            "INVALID_ARGUMENT",
            "INVALID_ARGUMENT: pubsub unknown pubsub is not found",
            () -> client.publishEvents("unknown pubsub", "mytopic","text/plain", "message").block());
    }
  }

  @Test
  public void testBulkPublish() throws Exception {
    final DaprRun daprRun = closeLater(startDaprApp(
        this.getClass().getSimpleName(),
        SubscriberService.SUCCESS_MESSAGE,
        SubscriberService.class,
        true,
        60000));
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
    try (DaprClient client = new DaprClientBuilder().withObjectSerializer(serializer).build();
         DaprPreviewClient previewClient = new DaprClientBuilder().withObjectSerializer(serializer).buildPreviewClient()) {
      // Only for the gRPC test
      // Send a multiple messages on one topic in messagebus pubsub via publishEvents API.
      List<String> messages = new ArrayList<>();
      for (int i = 0; i < NUM_MESSAGES; i++) {
        messages.add(String.format("This is message #%d on topic %s", i, TOPIC_BULK));
      }
      //Publishing 10 messages
      BulkPublishResponse response = previewClient.publishEvents(PUBSUB_NAME, TOPIC_BULK, "", messages).block();
      System.out.println(String.format("Published %d messages to topic '%s' pubsub_name '%s'",
          NUM_MESSAGES, TOPIC_BULK, PUBSUB_NAME));
      assertNotNull(response, "expected not null bulk publish response");
      assertEquals( 0, response.getFailedEntries().size(), "expected no failures in the response");

      //Publishing an object.
      MyObject object = new MyObject();
      object.setId("123");
      response = previewClient.publishEvents(PUBSUB_NAME, TOPIC_BULK,
          "application/json", Collections.singletonList(object)).block();
      System.out.println("Published one object.");
      assertNotNull(response, "expected not null bulk publish response");
      assertEquals(0, response.getFailedEntries().size(), "expected no failures in the response");

      //Publishing a single byte: Example of non-string based content published
      previewClient.publishEvents(PUBSUB_NAME, TOPIC_BULK, "",
          Collections.singletonList(new byte[]{1})).block();
      System.out.println("Published one byte.");

      assertNotNull(response, "expected not null bulk publish response");
      assertEquals(0, response.getFailedEntries().size(), "expected no failures in the response");

      CloudEvent cloudEvent = new CloudEvent();
      cloudEvent.setId("1234");
      cloudEvent.setData("message from cloudevent");
      cloudEvent.setSource("test");
      cloudEvent.setSpecversion("1");
      cloudEvent.setType("myevent");
      cloudEvent.setDatacontenttype("text/plain");
      BulkPublishRequest<CloudEvent> req = new BulkPublishRequest<>(PUBSUB_NAME, TOPIC_BULK,
          Collections.singletonList(
              new BulkPublishEntry<>("1", cloudEvent, "application/cloudevents+json", null)
          ));

      //Publishing a cloud event.
      previewClient.publishEvents(req).block();
      assertNotNull(response, "expected not null bulk publish response");
      assertEquals(0, response.getFailedEntries().size(), "expected no failures in the response");

      System.out.println("Published one cloud event.");

      // Introduce sleep
      Thread.sleep(10000);

      // Check messagebus subscription for topic testingbulktopic since it is populated only by publishEvents API call
      callWithRetry(() -> {
        System.out.println("Checking results for topic " + TOPIC_BULK + " in pubsub " + PUBSUB_NAME);
        // Validate text payload.
        final List<CloudEvent> cloudEventMessages = client.invokeMethod(
            daprRun.getAppName(),
            "messages/redis/testingbulktopic",
            null,
            HttpExtension.GET,
            CLOUD_EVENT_LIST_TYPE_REF).block();
        assertEquals(13, cloudEventMessages.size(), "expected 13 messages to be received on subscribe");
        for (int i = 0; i < NUM_MESSAGES; i++) {
          final int messageId = i;
          assertTrue(cloudEventMessages
              .stream()
              .filter(m -> m.getData() != null)
              .map(m -> m.getData())
              .filter(m -> m.equals(String.format("This is message #%d on topic %s", messageId, TOPIC_BULK)))
              .count() == 1, "expected data content to match");
        }

        // Validate object payload.
        assertTrue(cloudEventMessages
            .stream()
            .filter(m -> m.getData() != null)
            .filter(m -> m.getData() instanceof LinkedHashMap)
            .map(m -> (LinkedHashMap) m.getData())
            .filter(m -> "123".equals(m.get("id")))
            .count() == 1, "expected data content 123 to match");

        // Validate byte payload.
        assertTrue(cloudEventMessages
            .stream()
            .filter(m -> m.getData() != null)
            .map(m -> m.getData())
            .filter(m -> "AQ==".equals(m))
            .count() == 1, "expected bin data to match");

        // Validate cloudevent payload.
        assertTrue( cloudEventMessages
            .stream()
            .filter(m -> m.getData() != null)
            .map(m -> m.getData())
            .filter(m -> "message from cloudevent".equals(m))
            .count() == 1, "expected data to match");
      }, 2000);
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

      {
        CloudEvent cloudEventV2 = new CloudEvent();
        cloudEventV2.setId("2222");
        cloudEventV2.setData("message from cloudevent v2");
        cloudEventV2.setSource("test");
        cloudEventV2.setSpecversion("1");
        cloudEventV2.setType("myevent.v2");
        cloudEventV2.setDatacontenttype("text/plain");
        client.publishEvent(
            new PublishEventRequest(PUBSUB_NAME, TOPIC_NAME, cloudEventV2)
                .setContentType("application/cloudevents+json")).block();
        System.out.println("Published one cloud event for v2.");
      }

      {
        CloudEvent cloudEventV3 = new CloudEvent();
        cloudEventV3.setId("3333");
        cloudEventV3.setData("message from cloudevent v3");
        cloudEventV3.setSource("test");
        cloudEventV3.setSpecversion("1");
        cloudEventV3.setType("myevent.v3");
        cloudEventV3.setDatacontenttype("text/plain");
        client.publishEvent(
            new PublishEventRequest(PUBSUB_NAME, TOPIC_NAME, cloudEventV3)
                .setContentType("application/cloudevents+json")).block();
        System.out.println("Published one cloud event for v3.");
      }

      Thread.sleep(2000);

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
        System.out.println("Checking results for topic " + TOPIC_NAME + " V2");
        // Validate text payload.
        final List<CloudEvent> messages = client.invokeMethod(
                daprRun.getAppName(),
                "messages/testingtopicV2",
                null,
                HttpExtension.GET,
                CLOUD_EVENT_LIST_TYPE_REF).block();
        assertEquals(1, messages.size());
      }, 2000);

      callWithRetry(() -> {
        System.out.println("Checking results for topic " + TOPIC_NAME + " V3");
        // Validate text payload.
        final List<CloudEvent> messages = client.invokeMethod(
                daprRun.getAppName(),
                "messages/testingtopicV3",
                null,
                HttpExtension.GET,
                CLOUD_EVENT_LIST_TYPE_REF).block();
        assertEquals(1, messages.size());
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
  public void testPubSubBulkSubscribe() throws Exception {
    DaprRun daprRun = closeLater(startDaprApp(
            this.getClass().getSimpleName(),
            SubscriberService.SUCCESS_MESSAGE,
            SubscriberService.class,
            true,
            60000));

    // Send a batch of messages on one topic.
    try (DaprClient client = new DaprClientBuilder().build()) {
      for (int i = 0; i < NUM_MESSAGES; i++) {
        String message = String.format("This is message #%d on topic %s", i, BULK_SUB_TOPIC_NAME);
        // Publishing messages
        client.publishEvent(PUBSUB_NAME, BULK_SUB_TOPIC_NAME, message).block();
        System.out.printf("Published message: '%s' to topic '%s' pubSub_name '%s'\n",
                message, BULK_SUB_TOPIC_NAME, PUBSUB_NAME);
      }
    }

    // Sleeps for five seconds to give subscriber a chance to receive messages.
    Thread.sleep(5000);

    final String appId = daprRun.getAppName();
    try (DaprClient client = new DaprClientBuilder().build()) {
      callWithRetry(() -> {
        System.out.println("Checking results for topic " + BULK_SUB_TOPIC_NAME);

        @SuppressWarnings("unchecked")
        Class<List<BulkSubscribeAppResponse>> clazz = (Class) List.class;

        final List<BulkSubscribeAppResponse> messages = client.invokeMethod(
                appId,
                "messages/" + BULK_SUB_TOPIC_NAME,
                null,
                HttpExtension.GET,
                clazz).block();

        assertNotNull(messages);
        BulkSubscribeAppResponse response = OBJECT_MAPPER.convertValue(messages.get(0), BulkSubscribeAppResponse.class);

        // There should be a single bulk response.
        assertEquals(1, messages.size());

        // The bulk response should contain NUM_MESSAGES entries.
        assertEquals(NUM_MESSAGES, response.getStatuses().size());

        // All the entries should be SUCCESS.
        for (BulkSubscribeAppResponseEntry entry : response.getStatuses()) {
          assertEquals(entry.getStatus(), BulkSubscribeAppResponseStatus.SUCCESS);
        }
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

    Random random = new Random(590518626939830271L);
    Set<ConvertToLong> values = new HashSet<>();
    values.add(new ConvertToLong().setVal(590518626939830271L));
    ConvertToLong val;
    for (int i = 0; i < NUM_MESSAGES - 1; i++) {
      do {
        val = new ConvertToLong().setVal(random.nextLong());
      } while (values.contains(val));
      values.add(val);
    }
    Iterator<ConvertToLong> valuesIt = values.iterator();
    try (DaprClient client = new DaprClientBuilder().build()) {
      for (int i = 0; i < NUM_MESSAGES; i++) {
        ConvertToLong value = valuesIt.next();
        System.out.println("The long value sent " + value.getValue());
        //Publishing messages
        client.publishEvent(
            PUBSUB_NAME,
            LONG_TOPIC_NAME,
            value,
            Collections.singletonMap(Metadata.TTL_IN_SECONDS, "30")).block();

        try {
          Thread.sleep((long) (1000 * Math.random()));
        } catch (InterruptedException e) {
          e.printStackTrace();
          Thread.currentThread().interrupt();
          return;
        }
      }
    }

    Set<ConvertToLong> actual = new HashSet<>();
    try (DaprClient client = new DaprClientBuilder().build()) {
      callWithRetry(() -> {
        System.out.println("Checking results for topic " + LONG_TOPIC_NAME);
        final List<CloudEvent<ConvertToLong>> messages = client.invokeMethod(
            daprRun.getAppName(),
            "messages/testinglongvalues",
            null,
            HttpExtension.GET, CLOUD_EVENT_LONG_LIST_TYPE_REF).block();
        assertNotNull(messages);
        for (CloudEvent<ConvertToLong> message : messages) {
          actual.add(message.getData());
        }
        Assertions.assertEquals(values, actual);
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

    public ConvertToLong setVal(Long value) {
      this.value = value;
      return this;
    }

    public Long getValue() {
      return value;
    }


    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ConvertToLong that = (ConvertToLong) o;
      return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }
  }

}
