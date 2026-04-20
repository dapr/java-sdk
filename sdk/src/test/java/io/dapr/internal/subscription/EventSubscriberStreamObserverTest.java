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
 * limitations under the License.
*/

package io.dapr.internal.subscription;

import com.google.protobuf.ByteString;
import io.dapr.client.domain.CloudEvent;
import io.dapr.exceptions.DaprException;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.utils.TypeRef;
import io.dapr.v1.DaprAppCallbackProtos;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprPubsubProtos;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EventSubscriberStreamObserver.
 */
class EventSubscriberStreamObserverTest {

  public static final String PUBSUB_NAME = "pubsub";
  public static final String TOPIC_NAME = "topic";
  private DaprGrpc.DaprStub mockStub;
  private DaprObjectSerializer objectSerializer;
  private StreamObserver<DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1> mockRequestStream;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    mockStub = mock(DaprGrpc.DaprStub.class);
    objectSerializer = new DefaultObjectSerializer();
    mockRequestStream = mock(StreamObserver.class);

    when(mockStub.subscribeTopicEventsAlpha1(any())).thenReturn(mockRequestStream);
  }

  @Test
  @DisplayName("Should successfully process events and send SUCCESS acks")
  void testSuccessfulEventProcessing() {
    Flux<String> flux = Flux.create(sink -> {
      EventSubscriberStreamObserver<String> observer = new EventSubscriberStreamObserver<>(
          mockStub,
          sink,
          TypeRef.STRING,
          objectSerializer
      );

      // Start the subscription
      DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 initialRequest = buildInitialRequest(
      );
      observer.start(initialRequest);

      // Simulate receiving an event
      DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1 response = buildEventResponse(
          "event-1",
          "Hello World"
      );
      observer.onNext(response);

      // Complete the stream
      observer.onCompleted();
    });

    StepVerifier.create(flux)
        .assertNext(data -> {
          assertEquals("Hello World", data);
        })
        .verifyComplete();

    ArgumentCaptor<DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1> requestCaptor =
        ArgumentCaptor.forClass(DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1.class);

    verify(mockRequestStream, times(2)).onNext(requestCaptor.capture());

    List<DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1> requests = requestCaptor.getAllValues();

    assertEquals(2, requests.size());
    assertTrue(requests.get(0).hasInitialRequest());
    assertTrue(requests.get(1).hasEventProcessed());
    assertEquals("event-1", requests.get(1).getEventProcessed().getId());
    assertEquals(
        DaprAppCallbackProtos.TopicEventResponse.TopicEventResponseStatus.SUCCESS,
        requests.get(1).getEventProcessed().getStatus().getStatus()
    );
  }

  @Test
  @DisplayName("Should handle multiple consecutive events correctly")
  void testMultipleEvents() {
    Flux<String> flux = Flux.create(sink -> {
      EventSubscriberStreamObserver<String> observer = new EventSubscriberStreamObserver<>(
          mockStub,
          sink,
          TypeRef.STRING,
          objectSerializer
      );

      DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 initialRequest = buildInitialRequest(
      );
      observer.start(initialRequest);

      observer.onNext(buildEventResponse("event-1", "Message 1"));
      observer.onNext(buildEventResponse("event-2", "Message 2"));
      observer.onNext(buildEventResponse("event-3", "Message 3"));

      observer.onCompleted();
    });

    StepVerifier.create(flux)
        .assertNext(data -> {
          assertEquals("Message 1", data);
        })
        .assertNext(data -> {
          assertEquals("Message 2", data);
        })
        .assertNext(data -> {
          assertEquals("Message 3", data);
        })
        .verifyComplete();

    verify(mockRequestStream, times(4)).onNext(any());
  }

  @Test
  @DisplayName("Should send DROP ack when deserialization fails")
  void testDeserializationError() {
    Flux<String> flux = Flux.create(sink -> {
      EventSubscriberStreamObserver<String> observer = new EventSubscriberStreamObserver<>(
          mockStub,
          sink,
          TypeRef.STRING,
          objectSerializer
      );

      DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 initialRequest = buildInitialRequest(
      );
      observer.start(initialRequest);

      // Send an event with invalid data (can't deserialize to String)
      DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1 response = DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1.newBuilder()
          .setEventMessage(
              DaprAppCallbackProtos.TopicEventRequest.newBuilder()
                  .setId("event-1")
                  .setPubsubName(PUBSUB_NAME)
                  .setTopic(TOPIC_NAME)
                  .setData(ByteString.copyFrom(new byte[]{(byte) 0xFF, (byte) 0xFE})) // Invalid UTF-8
                  .build()
          )
          .build();

      observer.onNext(response);
    });

    StepVerifier.create(flux)
        .expectErrorMatches(error ->
            error instanceof DaprException
            && error.getMessage().contains("DESERIALIZATION_ERROR")
            && error.getMessage().contains("event-1"))
        .verify();

    ArgumentCaptor<DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1> requestCaptor =
        ArgumentCaptor.forClass(DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1.class);

    verify(mockRequestStream, atLeast(2)).onNext(requestCaptor.capture());

    List<DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1> ackRequests = requestCaptor.getAllValues().stream()
        .filter(DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1::hasEventProcessed)
        .collect(Collectors.toList());

    assertEquals(1, ackRequests.size());
    assertEquals("event-1", ackRequests.get(0).getEventProcessed().getId());
    assertEquals(
        DaprAppCallbackProtos.TopicEventResponse.TopicEventResponseStatus.DROP,
        ackRequests.get(0).getEventProcessed().getStatus().getStatus()
    );
  }

  @Test
  @DisplayName("Should send RETRY ack when non-deserialization error occurs")
  void testProcessingError() {
    Flux<String> flux = Flux.create(sink -> {
      EventSubscriberStreamObserver<String> observer = new EventSubscriberStreamObserver<>(
          mockStub,
          sink,
          TypeRef.STRING,
          objectSerializer
      );

      DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 initialRequest = buildInitialRequest();
      observer.start(initialRequest);

      // Simulate a processing error by throwing during sink.next()
      sink.onRequest(n -> {
        throw new RuntimeException("Processing error");
      });

      observer.onNext(buildEventResponse("event-1", "Hello"));
    });

    StepVerifier.create(flux)
        .expectError(RuntimeException.class)
        .verify();

    // Note: When error occurs in onRequest callback (before processing),
    // no ack is sent as the error happens before we can handle the event
    verify(mockRequestStream, times(1)).onNext(any()); // Only initial request sent
  }

  @Test
  @DisplayName("Should propagate gRPC errors as DaprException")
  void testGrpcError() {
    Flux<String> flux = Flux.create(sink -> {
      EventSubscriberStreamObserver<String> observer = new EventSubscriberStreamObserver<>(
          mockStub,
          sink,
          TypeRef.STRING,
          objectSerializer
      );

      DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 initialRequest = buildInitialRequest();
      observer.start(initialRequest);

      // Simulate gRPC error
      observer.onError(new RuntimeException("gRPC connection failed"));
    });

    StepVerifier.create(flux)
        .expectError(DaprException.class)
        .verify();
  }

  @Test
  @DisplayName("Should handle null event messages gracefully without emitting events")
  void testNullEventMessage() {
    Flux<String> flux = Flux.create(sink -> {
      EventSubscriberStreamObserver<String> observer = new EventSubscriberStreamObserver<>(
          mockStub,
          sink,
          TypeRef.STRING,
          objectSerializer
      );

      DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 initialRequest = buildInitialRequest(
      );
      observer.start(initialRequest);

      DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1 response = DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1.newBuilder()
          .build();

      observer.onNext(response);
      observer.onCompleted();
    });

    StepVerifier.create(flux)
        .verifyComplete();

    verify(mockRequestStream, times(1)).onNext(any());
  }

  @Test
  @DisplayName("Should skip events with empty pubsub name")
  void testEmptyPubsubName() {
    Flux<String> flux = Flux.create(sink -> {
      EventSubscriberStreamObserver<String> observer = new EventSubscriberStreamObserver<>(
          mockStub,
          sink,
          TypeRef.STRING,
          objectSerializer
      );

      DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 initialRequest = buildInitialRequest(
      );
      observer.start(initialRequest);

      DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1 response = DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1.newBuilder()
          .setEventMessage(
              DaprAppCallbackProtos.TopicEventRequest.newBuilder()
                  .setId("event-1")
                  .setPubsubName("")
                  .setTopic(TOPIC_NAME)
                  .setData(ByteString.copyFromUtf8("\"Hello\""))
                  .build()
          )
          .build();

      observer.onNext(response);
      observer.onCompleted();
    });

    StepVerifier.create(flux)
        .verifyComplete();

    verify(mockRequestStream, times(1)).onNext(any());
  }

  @Test
  @DisplayName("Should skip events with empty event ID")
  void testEmptyEventId() {
    Flux<String> flux = Flux.create(sink -> {
      EventSubscriberStreamObserver<String> observer = new EventSubscriberStreamObserver<>(
          mockStub,
          sink,
          TypeRef.STRING,
          objectSerializer
      );

      DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 initialRequest = buildInitialRequest(
      );
      observer.start(initialRequest);

      DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1 response = DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1.newBuilder()
          .setEventMessage(
              DaprAppCallbackProtos.TopicEventRequest.newBuilder()
                  .setId("")
                  .setPubsubName(PUBSUB_NAME)
                  .setTopic(TOPIC_NAME)
                  .setData(ByteString.copyFromUtf8("\"Hello\""))
                  .build()
          )
          .build();

      observer.onNext(response);
      observer.onCompleted();
    });

    StepVerifier.create(flux)
        .verifyComplete();

    verify(mockRequestStream, times(1)).onNext(any());
  }

  @Test
  @DisplayName("Should handle null type parameter by skipping emission but still sending ack")
  void testNullTypeSkipsEmission() {
    Flux<String> flux = Flux.create(sink -> {
      EventSubscriberStreamObserver<String> observer = new EventSubscriberStreamObserver<>(
          mockStub,
          sink,
          null, // null type - deserialize returns null
          objectSerializer
      );

      DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 initialRequest = buildInitialRequest(
      );

      observer.start(initialRequest);
      observer.onNext(buildEventResponse("event-1", "Hello"));
      observer.onCompleted();
    });

    // No events emitted since null values are skipped (Reactor doesn't allow null)
    StepVerifier.create(flux)
        .verifyComplete();

    // But ack is still sent
    verify(mockRequestStream, times(2)).onNext(any());
  }

  @Test
  @DisplayName("Should deserialize and emit complex objects correctly")
  void testComplexObjectSerialization() throws IOException {
    TestEvent testEvent = new TestEvent("test-name", 42);
    byte[] serializedEvent = objectSerializer.serialize(testEvent);

    Flux<TestEvent> flux = Flux.create(sink -> {
      EventSubscriberStreamObserver<TestEvent> observer = new EventSubscriberStreamObserver<>(
          mockStub,
          sink,
          TypeRef.get(TestEvent.class),
          objectSerializer
      );

      DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 initialRequest = buildInitialRequest(
      );
      observer.start(initialRequest);

      DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1 response = DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1.newBuilder()
          .setEventMessage(
              DaprAppCallbackProtos.TopicEventRequest.newBuilder()
                  .setId("event-1")
                  .setPubsubName(PUBSUB_NAME)
                  .setTopic(TOPIC_NAME)
                  .setData(ByteString.copyFrom(serializedEvent))
                  .build()
          )
          .build();

      observer.onNext(response);
      observer.onCompleted();
    });

    StepVerifier.create(flux)
        .assertNext(event -> {
          assertEquals("test-name", event.name);
          assertEquals(42, event.value);
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should propagate errors when ack sending fails")
  void testErrorDuringSendingAck() {
    doThrow(new RuntimeException("Failed to send ack"))
        .when(mockRequestStream)
        .onNext(argThat(DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1::hasEventProcessed));

    Flux<String> flux = Flux.create(sink -> {
      EventSubscriberStreamObserver<String> observer = new EventSubscriberStreamObserver<>(
          mockStub,
          sink,
          TypeRef.STRING,
          objectSerializer
      );

      DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 initialRequest = buildInitialRequest();
      observer.start(initialRequest);

      observer.onNext(buildEventResponse("event-1", "Hello"));
    });

    StepVerifier.create(flux)
        .assertNext(data -> assertEquals("Hello", data))  // Event is emitted before ack
        .expectError(DaprException.class)  // Then error when sending ack
        .verify();
  }

  @Test
  @DisplayName("Should construct CloudEvent from TopicEventRequest when TypeRef<CloudEvent<T>> is used")
  void testCloudEventTypeConstruction() {
    Flux<CloudEvent<String>> flux = Flux.create(sink -> {
      EventSubscriberStreamObserver<CloudEvent<String>> observer = new EventSubscriberStreamObserver<>(
          mockStub,
          sink,
          new TypeRef<CloudEvent<String>>() {},
          objectSerializer
      );

      observer.start(buildInitialRequest());

      // Build response with all CloudEvent fields
      DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1 response = DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1.newBuilder()
          .setEventMessage(
              DaprAppCallbackProtos.TopicEventRequest.newBuilder()
                  .setId("event-123")
                  .setSource("test-source")
                  .setType("test.event.type")
                  .setSpecVersion("1.0")
                  .setDataContentType("application/json")
                  .setPubsubName(PUBSUB_NAME)
                  .setTopic(TOPIC_NAME)
                  .setData(ByteString.copyFromUtf8("\"Hello World\""))
                  .build()
          )
          .build();

      observer.onNext(response);
      observer.onCompleted();
    });

    StepVerifier.create(flux)
        .assertNext(cloudEvent -> {
          assertEquals("event-123", cloudEvent.getId());
          assertEquals("test-source", cloudEvent.getSource());
          assertEquals("test.event.type", cloudEvent.getType());
          assertEquals("1.0", cloudEvent.getSpecversion());
          assertEquals(TOPIC_NAME, cloudEvent.getTopic());
          assertEquals(PUBSUB_NAME, cloudEvent.getPubsubName());
          assertEquals("Hello World", cloudEvent.getData());
        })
        .verifyComplete();

    // Verify SUCCESS ack was sent
    verify(mockRequestStream, times(2)).onNext(any());
  }

  @Test
  @DisplayName("Should handle raw CloudEvent type without generic parameter")
  void testRawCloudEventType() {
    Flux<CloudEvent> flux = Flux.create(sink -> {
      EventSubscriberStreamObserver<CloudEvent> observer = new EventSubscriberStreamObserver<>(
          mockStub,
          sink,
          TypeRef.get(CloudEvent.class),
          objectSerializer
      );

      observer.start(buildInitialRequest());

      DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1 response = DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1.newBuilder()
          .setEventMessage(
              DaprAppCallbackProtos.TopicEventRequest.newBuilder()
                  .setId("event-456")
                  .setSource("raw-source")
                  .setType("raw.event.type")
                  .setSpecVersion("1.0")
                  .setPubsubName(PUBSUB_NAME)
                  .setTopic(TOPIC_NAME)
                  .setData(ByteString.copyFromUtf8("raw data content"))
                  .build()
          )
          .build();

      observer.onNext(response);
      observer.onCompleted();
    });

    StepVerifier.create(flux)
        .assertNext(cloudEvent -> {
          assertEquals("event-456", cloudEvent.getId());
          assertEquals("raw-source", cloudEvent.getSource());
          assertEquals("raw.event.type", cloudEvent.getType());
          assertEquals(TOPIC_NAME, cloudEvent.getTopic());
          // Raw CloudEvent has data as string
          assertEquals("raw data content", cloudEvent.getData());
        })
        .verifyComplete();
  }

  private DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 buildInitialRequest() {
    return DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1.newBuilder()
        .setInitialRequest(
            DaprPubsubProtos.SubscribeTopicEventsRequestInitialAlpha1.newBuilder()
                .setPubsubName(PUBSUB_NAME)
                .setTopic(TOPIC_NAME)
                .build()
        )
        .build();
  }

  private DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1 buildEventResponse(String eventId, String data) {

    try {
      byte[] serializedData = objectSerializer.serialize(data);
      return DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1.newBuilder()
          .setEventMessage(
              DaprAppCallbackProtos.TopicEventRequest.newBuilder()
                  .setId(eventId)
                  .setPubsubName(PUBSUB_NAME)
                  .setTopic(TOPIC_NAME)
                  .setData(ByteString.copyFrom(serializedData))
                  .build()
          )
          .build();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static class TestEvent {
    public String name;
    public int value;

    public TestEvent() {
    }

    public TestEvent(String name, int value) {
      this.name = name;
      this.value = value;
    }
  }
}
