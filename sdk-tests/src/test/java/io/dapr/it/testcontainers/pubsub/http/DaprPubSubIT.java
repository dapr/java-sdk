/*
 * Copyright 2025 The Dapr Authors
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
package io.dapr.it.testcontainers.pubsub.http;

import io.dapr.it.testcontainers.TestContainerNetworks;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.BulkPublishEntry;
import io.dapr.client.domain.BulkPublishRequest;
import io.dapr.client.domain.BulkPublishResponse;
import io.dapr.client.domain.BulkSubscribeAppResponseStatus;
import io.dapr.client.domain.CloudEvent;
import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.Metadata;
import io.dapr.client.domain.PublishEventRequest;
import io.dapr.it.pubsub.http.PubSubPayloads;
import io.dapr.it.testcontainers.DaprClientFactory;
import io.dapr.serializer.CustomizableObjectSerializer;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import io.dapr.utils.TypeRef;
import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static io.dapr.it.Retry.callWithRetry;
import static io.dapr.it.TestUtils.assertThrowsDaprException;
import static io.dapr.it.TestUtils.assertThrowsDaprExceptionWithReason;
import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    classes = {
        TestPubSubApplication.class
    }
)
@Testcontainers
@Tag("testcontainers")
public class DaprPubSubIT {

  private static final Logger LOG = LoggerFactory.getLogger(DaprPubSubIT.class);
  private static final Network DAPR_NETWORK = TestContainerNetworks.PUBSUB_NETWORK;
  private static final int PORT = TestContainerNetworks.allocateFreePort();
  private static final String APP_FOUND_MESSAGE_PATTERN = ".*application discovered on port.*";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final String PUBSUB_APP_ID = "pubsub-dapr-app";
  private static final String PUBSUB_NAME = "pubsub";

  // topics
  private static final String TOPIC_BULK = "testingbulktopic";
  private static final String TOPIC_NAME = "testingtopic";
  private static final String ANOTHER_TOPIC_NAME = "anothertopic";
  private static final String TYPED_TOPIC_NAME = "typedtestingtopic";
  private static final String BINARY_TOPIC_NAME = "binarytopic";
  private static final String TTL_TOPIC_NAME = "ttltopic";
  private static final String LONG_TOPIC_NAME = "testinglongvalues";

  private static final int NUM_MESSAGES = 10;

  // typeRefs
  private static final TypeRef<List<CloudEvent>> CLOUD_EVENT_LIST_TYPE_REF = new TypeRef<>() {
  };
  private static final TypeRef<List<CloudEvent<PubSubPayloads.ConvertToLong>>> CLOUD_EVENT_LONG_LIST_TYPE_REF =
      new TypeRef<>() {
      };
  private static final TypeRef<List<CloudEvent<PubSubPayloads.MyObject>>> CLOUD_EVENT_MYOBJECT_LIST_TYPE_REF =
      new TypeRef<>() {
      };

  @Container
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
      .withAppName(PUBSUB_APP_ID)
      .withNetwork(DAPR_NETWORK)
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withLogConsumer(outputFrame -> LOG.info(outputFrame.getUtf8String()))
      .withAppChannelAddress("host.testcontainers.internal")
      .withAppPort(PORT);

  /**
   * Expose the Dapr ports to the host.
   *
   * @param registry the dynamic property registry
   */
  @DynamicPropertySource
  static void daprProperties(DynamicPropertyRegistry registry) {
    registry.add("dapr.http.endpoint", DAPR_CONTAINER::getHttpEndpoint);
    registry.add("dapr.grpc.endpoint", DAPR_CONTAINER::getGrpcEndpoint);
    registry.add("server.port", () -> PORT);
  }


  @BeforeEach
  public void setUp() throws Exception {
    org.testcontainers.Testcontainers.exposeHostPorts(PORT);
    try (DaprClient client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build()) {
      client.invokeMethod(PUBSUB_APP_ID, "messages/clear", null, HttpExtension.POST, Void.class).block();
    }
  }

  @Test
  @DisplayName("Should receive INVALID_ARGUMENT when the specified Pub/Sub name does not exist")
  public void shouldReceiveInvalidArgument() throws Exception {
    Wait.forLogMessage(APP_FOUND_MESSAGE_PATTERN, 1).waitUntilReady(DAPR_CONTAINER);

    try (DaprClient client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build()) {
      assertThrowsDaprExceptionWithReason(
          "INVALID_ARGUMENT",
          "INVALID_ARGUMENT: pubsub unknown pubsub is not found",
          "DAPR_PUBSUB_NOT_FOUND",
          () -> client.publishEvent("unknown pubsub", "mytopic", "payload").block());
    }
  }

  @Test
  @DisplayName("Should receive INVALID_ARGUMENT using bulk publish when the specified Pub/Sub name does not exist")
  public void shouldReceiveInvalidArgumentWithBulkPublish() throws Exception {
    try (DaprClient client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build()) {
      assertThrowsDaprException(
          "INVALID_ARGUMENT",
          "INVALID_ARGUMENT: pubsub unknown pubsub is not found",
          () -> client.publishEvents("unknown pubsub", "mytopic", "text/plain", "message").block());
    }
  }

  @Test
  @DisplayName("Should publish some payload types successfully")
  public void shouldPublishSomePayloadTypesWithNoError() throws Exception {

    try (
        DaprClient client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).withObjectSerializer(
                createJacksonObjectSerializer())
            .build()
    ) {

      publishBulkStringsAsserting(client);

      publishMyObjectAsserting(client);

      publishByteAsserting(client);

      publishCloudEventAsserting(client);

      Thread.sleep(10000);

      callWithRetry(() -> validatePublishedMessages(client), 2000);
    }
  }

  @Test
  @DisplayName("Should publish various payload types to different topics")
  public void testPubSub() throws Exception {

    // Send a batch of messages on one topic
    try (DaprClient client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).withObjectSerializer(
            createJacksonObjectSerializer()
    ).build()) {

      sendBulkMessagesAsText(client, TOPIC_NAME);

      sendBulkMessagesAsText(client, ANOTHER_TOPIC_NAME);

      //Publishing an object.
      PubSubPayloads.MyObject object = new PubSubPayloads.MyObject();
      object.setId("123");
      client.publishEvent(PUBSUB_NAME, TOPIC_NAME, object).block();
      LOG.info("Published one object.");

      client.publishEvent(PUBSUB_NAME, TYPED_TOPIC_NAME, object).block();
      LOG.info("Published another object.");

      //Publishing a single byte: Example of non-string based content published
      publishOneByteSync(client, TOPIC_NAME);

      CloudEvent<String> cloudEvent = new CloudEvent<>();
      cloudEvent.setId("1234");
      cloudEvent.setData("message from cloudevent");
      cloudEvent.setSource("test");
      cloudEvent.setSpecversion("1");
      cloudEvent.setType("myevent");
      cloudEvent.setDatacontenttype("text/plain");

      //Publishing a cloud event.
      client.publishEvent(new PublishEventRequest(PUBSUB_NAME, TOPIC_NAME, cloudEvent)
          .setContentType("application/cloudevents+json")).block();
      LOG.info("Published one cloud event.");

      {
        CloudEvent<String> cloudEventV2 = new CloudEvent<>();
        cloudEventV2.setId("2222");
        cloudEventV2.setData("message from cloudevent v2");
        cloudEventV2.setSource("test");
        cloudEventV2.setSpecversion("1");
        cloudEventV2.setType("myevent.v2");
        cloudEventV2.setDatacontenttype("text/plain");
        client.publishEvent(
            new PublishEventRequest(PUBSUB_NAME, TOPIC_NAME, cloudEventV2)
                .setContentType("application/cloudevents+json")).block();
        LOG.info("Published one cloud event for v2.");
      }

      {
        CloudEvent<String> cloudEventV3 = new CloudEvent<>();
        cloudEventV3.setId("3333");
        cloudEventV3.setData("message from cloudevent v3");
        cloudEventV3.setSource("test");
        cloudEventV3.setSpecversion("1");
        cloudEventV3.setType("myevent.v3");
        cloudEventV3.setDatacontenttype("text/plain");
        client.publishEvent(
            new PublishEventRequest(PUBSUB_NAME, TOPIC_NAME, cloudEventV3)
                .setContentType("application/cloudevents+json")).block();
        LOG.info("Published one cloud event for v3.");
      }

      Thread.sleep(2000);

      callWithRetry(() -> {
        LOG.info("Checking results for topic " + TOPIC_NAME);

        List<CloudEvent> messages = client.invokeMethod(
            PUBSUB_APP_ID,
            "messages/testingtopic",
            null,
            HttpExtension.GET,
            CLOUD_EVENT_LIST_TYPE_REF
        ).block();

        assertThat(messages)
            .hasSize(13)
            .extracting(CloudEvent::getData)
            .filteredOn(Objects::nonNull)
            .contains(
                "AQ==",
                "message from cloudevent"
            );

        for (int i = 0; i < NUM_MESSAGES; i++) {
          String expectedMessage = String.format("This is message #%d on topic %s", i, TOPIC_NAME);
          assertThat(messages)
              .extracting(CloudEvent::getData)
              .filteredOn(Objects::nonNull)
              .anyMatch(expectedMessage::equals);
        }

        assertThat(messages)
            .extracting(CloudEvent::getData)
            .filteredOn(LinkedHashMap.class::isInstance)
            .map(data -> (String) ((LinkedHashMap<?, ?>) data).get("id"))
            .contains("123");
      }, 2000);

      callWithRetry(() -> {
        LOG.info("Checking results for topic " + TOPIC_NAME + " V2");

        List<CloudEvent> messages = client.invokeMethod(
            PUBSUB_APP_ID,
            "messages/testingtopicV2",
            null,
            HttpExtension.GET,
            CLOUD_EVENT_LIST_TYPE_REF
        ).block();

        assertThat(messages)
            .hasSize(1);
      }, 2000);

      callWithRetry(() -> {
        LOG.info("Checking results for topic " + TOPIC_NAME + " V3");

        List<CloudEvent> messages = client.invokeMethod(
            PUBSUB_APP_ID,
            "messages/testingtopicV3",
            null,
            HttpExtension.GET,
            CLOUD_EVENT_LIST_TYPE_REF
        ).block();

        assertThat(messages)
            .hasSize(1);
      }, 2000);

      callWithRetry(() -> {
        LOG.info("Checking results for topic " + TYPED_TOPIC_NAME);

        List<CloudEvent<PubSubPayloads.MyObject>> messages = client.invokeMethod(
            PUBSUB_APP_ID,
            "messages/typedtestingtopic",
            null,
            HttpExtension.GET,
            CLOUD_EVENT_MYOBJECT_LIST_TYPE_REF
        ).block();

        assertThat(messages)
            .extracting(CloudEvent::getData)
            .filteredOn(Objects::nonNull)
            .filteredOn(PubSubPayloads.MyObject.class::isInstance)
            .map(PubSubPayloads.MyObject::getId)
            .contains("123");
      }, 2000);

      callWithRetry(() -> {
        LOG.info("Checking results for topic " + ANOTHER_TOPIC_NAME);

        List<CloudEvent> messages = client.invokeMethod(
            PUBSUB_APP_ID,
            "messages/anothertopic",
            null,
            HttpExtension.GET,
            CLOUD_EVENT_LIST_TYPE_REF
        ).block();

        assertThat(messages)
            .hasSize(10);

        for (int i = 0; i < NUM_MESSAGES; i++) {
          String expectedMessage = String.format("This is message #%d on topic %s", i, ANOTHER_TOPIC_NAME);
          assertThat(messages)
              .extracting(CloudEvent::getData)
              .filteredOn(Objects::nonNull)
              .anyMatch(expectedMessage::equals);
        }
      }, 2000);

    }
  }

  @Test
  @DisplayName("Should publish binary payload type successfully")
  public void shouldPublishBinary() throws Exception {

    DaprObjectSerializer serializer = createBinaryObjectSerializer();

    try (DaprClient client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).withObjectSerializer(serializer).build()) {
      publishOneByteSync(client, BINARY_TOPIC_NAME);
    }

    Thread.sleep(3000);

    try (DaprClient client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build()) {
      callWithRetry(() -> {
        LOG.info("Checking results for topic " + BINARY_TOPIC_NAME);
        final List<CloudEvent> messages = client.invokeMethod(
            PUBSUB_APP_ID,
            "messages/binarytopic",
            null,
            HttpExtension.GET, CLOUD_EVENT_LIST_TYPE_REF).block();

        SoftAssertions.assertSoftly(softly -> {
          softly.assertThat(messages.size()).isEqualTo(1);
          softly.assertThat(messages.get(0).getData()).isNull();
          softly.assertThat(messages.get(0).getBinaryData()).isEqualTo(new byte[] {1});
        });
      }, 2000);
    }
  }

  private static void publishOneByteSync(DaprClient client, String topicName) {
    client.publishEvent(
        PUBSUB_NAME,
        topicName,
        new byte[] {1}).block();
  }

  private static void sendBulkMessagesAsText(DaprClient client, String topicName) {
    for (int i = 0; i < NUM_MESSAGES; i++) {
      String message = String.format("This is message #%d on topic %s", i, topicName);
      client.publishEvent(PUBSUB_NAME, topicName, message).block();
    }
  }

  private void publishMyObjectAsserting(DaprClient client) {
    PubSubPayloads.MyObject object = new PubSubPayloads.MyObject();
    object.setId("123");
    BulkPublishResponse<PubSubPayloads.MyObject> response = client.publishEvents(
        PUBSUB_NAME,
        TOPIC_BULK,
        "application/json",
        Collections.singletonList(object)
    ).block();
    SoftAssertions.assertSoftly(softly -> {
      softly.assertThat(response).isNotNull();
      softly.assertThat(response.getFailedEntries().size()).isZero();
    });
  }

  private void publishBulkStringsAsserting(DaprClient client) {
    List<String> messages = new ArrayList<>();
    for (int i = 0; i < NUM_MESSAGES; i++) {
      messages.add(String.format("This is message #%d on topic %s", i, TOPIC_BULK));
    }
    BulkPublishResponse<String> response = client.publishEvents(PUBSUB_NAME, TOPIC_BULK, "", messages).block();
    SoftAssertions.assertSoftly(softly -> {
      softly.assertThat(response).isNotNull();
      softly.assertThat(response.getFailedEntries().size()).isZero();
    });
  }

  private void publishByteAsserting(DaprClient client) {
    BulkPublishResponse<byte[]> response = client.publishEvents(
        PUBSUB_NAME,
        TOPIC_BULK,
        "",
        Collections.singletonList(new byte[] {1})
    ).block();
    SoftAssertions.assertSoftly(softly -> {
      assertThat(response).isNotNull();
      softly.assertThat(response.getFailedEntries().size()).isZero();
    });
  }

  private void publishCloudEventAsserting(DaprClient client) {
    CloudEvent<String> cloudEvent = new CloudEvent<>();
    cloudEvent.setId("1234");
    cloudEvent.setData("message from cloudevent");
    cloudEvent.setSource("test");
    cloudEvent.setSpecversion("1");
    cloudEvent.setType("myevent");
    cloudEvent.setDatacontenttype("text/plain");

    BulkPublishRequest<CloudEvent<String>> req = new BulkPublishRequest<>(
        PUBSUB_NAME,
        TOPIC_BULK,
        Collections.singletonList(
            new BulkPublishEntry<>("1", cloudEvent, "application/cloudevents+json", null)
        )
    );
    BulkPublishResponse<CloudEvent<String>> response = client.publishEvents(req).block();
    SoftAssertions.assertSoftly(softly -> {
      softly.assertThat(response).isNotNull();
      softly.assertThat(response.getFailedEntries().size()).isZero();
    });
  }

  private void validatePublishedMessages(DaprClient client) {
    List<CloudEvent> cloudEventMessages = client.invokeMethod(
        PUBSUB_APP_ID,
        "messages/redis/testingbulktopic",
        null,
        HttpExtension.GET,
        CLOUD_EVENT_LIST_TYPE_REF
    ).block();

    assertThat(cloudEventMessages)
        .as("expected non-null list of cloud events")
        .isNotNull()
        .hasSize(13);

    for (int i = 0; i < NUM_MESSAGES; i++) {
      String expectedMessage = String.format("This is message #%d on topic %s", i, TOPIC_BULK);
      assertThat(cloudEventMessages)
          .as("expected text payload to match for message %d", i)
          .anySatisfy(event -> assertThat(event.getData()).isEqualTo(expectedMessage));
    }

    assertThat(cloudEventMessages)
        .filteredOn(event -> event.getData() instanceof LinkedHashMap)
        .map(event -> (LinkedHashMap<?, ?>) event.getData())
        .anySatisfy(map -> assertThat(map.get("id")).isEqualTo("123"));

    assertThat(cloudEventMessages)
        .map(CloudEvent::getData)
        .anySatisfy(data -> assertThat(data).isEqualTo("AQ=="));

    assertThat(cloudEventMessages)
        .map(CloudEvent::getData)
        .anySatisfy(data -> assertThat(data).isEqualTo("message from cloudevent"));
  }

  @Test
  @DisplayName("Should publish with TTL")
  public void testPubSubTTLMetadata() throws Exception {

    // Send a batch of messages on one topic, all to be expired in 1 second.
    try (DaprClient client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build()) {
      for (int i = 0; i < NUM_MESSAGES; i++) {
        String message = String.format("This is message #%d on topic %s", i, TTL_TOPIC_NAME);
        //Publishing messages
        client.publishEvent(
            PUBSUB_NAME,
            TTL_TOPIC_NAME,
            message,
            Map.of(Metadata.TTL_IN_SECONDS, "1"))
            .block();
        LOG.info("Published message: '{}' to topic '{}' pubsub_name '{}'\n", message, TOPIC_NAME, PUBSUB_NAME);
      }
    }

    // Sleeps for two seconds to let them expire.
    Thread.sleep(2000);

    try (DaprClient client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build()) {
      callWithRetry(() -> {
        LOG.info("Checking results for topic " + TTL_TOPIC_NAME);
        final List
            messages = client.invokeMethod(PUBSUB_APP_ID, "messages/" + TTL_TOPIC_NAME, null, HttpExtension.GET, List.class).block();
        assertThat(messages).hasSize(0);
      }, 2000);
    }
  }

  @Test
  @DisplayName("Should publish long values")
  public void testLongValues() throws Exception {

    Random random = new Random(590518626939830271L);
    Set<PubSubPayloads.ConvertToLong> values = new HashSet<>();
    values.add(new PubSubPayloads.ConvertToLong().setVal(590518626939830271L));
    PubSubPayloads.ConvertToLong val;
    for (int i = 0; i < NUM_MESSAGES - 1; i++) {
      do {
        val = new PubSubPayloads.ConvertToLong().setVal(random.nextLong());
      } while (values.contains(val));
      values.add(val);
    }
    Iterator<PubSubPayloads.ConvertToLong> valuesIt = values.iterator();
    try (DaprClient client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build()) {
      for (int i = 0; i < NUM_MESSAGES; i++) {
        PubSubPayloads.ConvertToLong value = valuesIt.next();
        LOG.info("The long value sent " + value.getValue());
        //Publishing messages
        client.publishEvent(
            PUBSUB_NAME,
            LONG_TOPIC_NAME,
            value,
            Map.of(Metadata.TTL_IN_SECONDS, "30")).block();

        try {
          Thread.sleep((long) (1000 * Math.random()));
        } catch (InterruptedException e) {
          e.printStackTrace();
          Thread.currentThread().interrupt();
          return;
        }
      }
    }

    Set<PubSubPayloads.ConvertToLong> actual = new HashSet<>();
    try (DaprClient client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build()) {
      callWithRetry(() -> {
        LOG.info("Checking results for topic " + LONG_TOPIC_NAME);
        final List<CloudEvent<PubSubPayloads.ConvertToLong>> messages = client.invokeMethod(
            PUBSUB_APP_ID,
            "messages/testinglongvalues",
            null,
            HttpExtension.GET, CLOUD_EVENT_LONG_LIST_TYPE_REF).block();
        assertNotNull(messages);
        for (CloudEvent<PubSubPayloads.ConvertToLong> message : messages) {
          actual.add(message.getData());
        }
        assertThat(values).containsAll(actual);
      }, 2000);
    }
  }

  @Test
  @DisplayName("Should bulk subscribe successfully")
  public void testPubSubBulkSubscribe() throws Exception {
    try (DaprClient client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build()) {
      for (int i = 0; i < NUM_MESSAGES; i++) {
        String message = String.format("This is message #%d on topic %s", i, "topicBulkSub");
        client.publishEvent(PUBSUB_NAME, "topicBulkSub", message).block();
      }
    }

    Thread.sleep(5000);

    try (DaprClient client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build()) {
      callWithRetry(() -> {
        List<?> messages = client.invokeMethod(
            PUBSUB_APP_ID,
            "messages/topicBulkSub",
            null,
            HttpExtension.GET,
            List.class).block();

        assertThat(messages).isNotNull().isNotEmpty();
        List<String> allStatuses = messages.stream()
            .flatMap(message -> ((List<?>) ((Map<?, ?>) message).get("statuses")).stream())
            .map(statusEntry -> String.valueOf(((Map<?, ?>) statusEntry).get("status")))
            .collect(Collectors.toList());

        assertThat(allStatuses).hasSize(NUM_MESSAGES);
        for (String status : allStatuses) {
          assertThat(status).isEqualTo(BulkSubscribeAppResponseStatus.SUCCESS.name());
        }
      }, 2000);
    }
  }

  private @NotNull DaprObjectSerializer createBinaryObjectSerializer() {
    return new DaprObjectSerializer() {
      @Override
      public byte[] serialize(Object o) {
        return (byte[]) o;
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
  }

  private DaprObjectSerializer createJacksonObjectSerializer() {
    return new DaprObjectSerializer() {
      @Override
      public byte[] serialize(Object o) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsBytes(o);
      }

      @Override
      public <T> T deserialize(byte[] data, TypeRef<T> type) throws IOException {
        return OBJECT_MAPPER.readValue(data, OBJECT_MAPPER.constructType(type.getType()));
      }

      @Override
      public String getContentType() {
        return "application/json";
      }
    };
  }
}
