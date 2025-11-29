/*
 * Copyright 2024 The Dapr Authors
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
import io.dapr.exceptions.DaprException;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.utils.TypeRef;
import io.dapr.v1.DaprAppCallbackProtos;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprProtos;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventSubscriberStreamObserver.
 */
class EventSubscriberStreamObserverTest {

  private DaprGrpc.DaprStub mockStub;
  private DaprObjectSerializer objectSerializer;
  private StreamObserver<DaprProtos.SubscribeTopicEventsRequestAlpha1> mockRequestStream;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    mockStub = mock(DaprGrpc.DaprStub.class);
    objectSerializer = new DefaultObjectSerializer();
    mockRequestStream = mock(StreamObserver.class);

    // Setup stub to return mock request stream
    when(mockStub.subscribeTopicEventsAlpha1(any())).thenReturn(mockRequestStream);
  }

  @Test
  void testSuccessfulEventProcessing() {
    // Create a Flux and capture the sink
    List<String> emittedEvents = new ArrayList<>();
    Flux<String> flux = Flux.create(sink -> {
      EventSubscriberStreamObserver<String> observer = new EventSubscriberStreamObserver<>(
          mockStub,
          sink,
          TypeRef.STRING,
          objectSerializer
      );

      // Start the subscription
      DaprProtos.SubscribeTopicEventsRequestAlpha1 initialRequest = buildInitialRequest("pubsub", "topic");
      observer.start(initialRequest);

      // Simulate receiving an event
      DaprProtos.SubscribeTopicEventsResponseAlpha1 response = buildEventResponse("event-1", "pubsub", "topic", "Hello World");
      observer.onNext(response);

      // Complete the stream
      observer.onCompleted();
    });

    // Verify the flux emits the correct data
    StepVerifier.create(flux)
        .expectNext("Hello World")
        .verifyComplete();

    // Verify the initial request was sent
    ArgumentCaptor<DaprProtos.SubscribeTopicEventsRequestAlpha1> requestCaptor =
        ArgumentCaptor.forClass(DaprProtos.SubscribeTopicEventsRequestAlpha1.class);
    verify(mockRequestStream, times(2)).onNext(requestCaptor.capture());

    List<DaprProtos.SubscribeTopicEventsRequestAlpha1> requests = requestCaptor.getAllValues();
    assertEquals(2, requests.size());

    // First request should be the initial request
    assertTrue(requests.get(0).hasInitialRequest());

    // Second request should be a SUCCESS ack
    assertTrue(requests.get(1).hasEventProcessed());
    assertEquals("event-1", requests.get(1).getEventProcessed().getId());
    assertEquals(
        DaprAppCallbackProtos.TopicEventResponse.TopicEventResponseStatus.SUCCESS,
        requests.get(1).getEventProcessed().getStatus().getStatus()
    );
  }

  @Test
  void testMultipleEvents() {
    List<String> emittedEvents = new ArrayList<>();

    Flux<String> flux = Flux.create(sink -> {
      EventSubscriberStreamObserver<String> observer = new EventSubscriberStreamObserver<>(
          mockStub,
          sink,
          TypeRef.STRING,
          objectSerializer
      );

      DaprProtos.SubscribeTopicEventsRequestAlpha1 initialRequest = buildInitialRequest("pubsub", "topic");
      observer.start(initialRequest);

      // Send multiple events
      observer.onNext(buildEventResponse("event-1", "pubsub", "topic", "Message 1"));
      observer.onNext(buildEventResponse("event-2", "pubsub", "topic", "Message 2"));
      observer.onNext(buildEventResponse("event-3", "pubsub", "topic", "Message 3"));

      observer.onCompleted();
    });

    StepVerifier.create(flux)
        .expectNext("Message 1")
        .expectNext("Message 2")
        .expectNext("Message 3")
        .verifyComplete();

    // Verify 4 requests: 1 initial + 3 acks
    verify(mockRequestStream, times(4)).onNext(any());
  }

  @Test
  void testDeserializationError() {
    Flux<String> flux = Flux.create(sink -> {
      EventSubscriberStreamObserver<String> observer = new EventSubscriberStreamObserver<>(
          mockStub,
          sink,
          TypeRef.STRING,
          objectSerializer
      );

      DaprProtos.SubscribeTopicEventsRequestAlpha1 initialRequest = buildInitialRequest("pubsub", "topic");
      observer.start(initialRequest);

      // Send an event with invalid data (can't deserialize to String)
      DaprProtos.SubscribeTopicEventsResponseAlpha1 response = DaprProtos.SubscribeTopicEventsResponseAlpha1.newBuilder()
          .setEventMessage(
              DaprAppCallbackProtos.TopicEventRequest.newBuilder()
                  .setId("event-1")
                  .setPubsubName("pubsub")
                  .setTopic("topic")
                  .setData(ByteString.copyFrom(new byte[]{(byte) 0xFF, (byte) 0xFE})) // Invalid UTF-8
                  .build()
          )
          .build();

      observer.onNext(response);
    });

    // Verify error is propagated
    StepVerifier.create(flux)
        .expectError(DaprException.class)
        .verify();

    // Verify RETRY ack was sent
    ArgumentCaptor<DaprProtos.SubscribeTopicEventsRequestAlpha1> requestCaptor =
        ArgumentCaptor.forClass(DaprProtos.SubscribeTopicEventsRequestAlpha1.class);
    verify(mockRequestStream, atLeast(2)).onNext(requestCaptor.capture());

    // Find the ack request (not the initial request)
    List<DaprProtos.SubscribeTopicEventsRequestAlpha1> ackRequests = requestCaptor.getAllValues().stream()
        .filter(DaprProtos.SubscribeTopicEventsRequestAlpha1::hasEventProcessed)
        .collect(Collectors.toList());

    assertEquals(1, ackRequests.size());
    assertEquals("event-1", ackRequests.get(0).getEventProcessed().getId());
    assertEquals(
        DaprAppCallbackProtos.TopicEventResponse.TopicEventResponseStatus.RETRY,
        ackRequests.get(0).getEventProcessed().getStatus().getStatus()
    );
  }

  @Test
  void testGrpcError() {
    Flux<String> flux = Flux.create(sink -> {
      EventSubscriberStreamObserver<String> observer = new EventSubscriberStreamObserver<>(
          mockStub,
          sink,
          TypeRef.STRING,
          objectSerializer
      );

      DaprProtos.SubscribeTopicEventsRequestAlpha1 initialRequest = buildInitialRequest("pubsub", "topic");
      observer.start(initialRequest);

      // Simulate gRPC error
      observer.onError(new RuntimeException("gRPC connection failed"));
    });

    StepVerifier.create(flux)
        .expectError(DaprException.class)
        .verify();
  }

  @Test
  void testNullEventMessage() {
    Flux<String> flux = Flux.create(sink -> {
      EventSubscriberStreamObserver<String> observer = new EventSubscriberStreamObserver<>(
          mockStub,
          sink,
          TypeRef.STRING,
          objectSerializer
      );

      DaprProtos.SubscribeTopicEventsRequestAlpha1 initialRequest = buildInitialRequest("pubsub", "topic");
      observer.start(initialRequest);

      // Send response with null event message
      DaprProtos.SubscribeTopicEventsResponseAlpha1 response = DaprProtos.SubscribeTopicEventsResponseAlpha1.newBuilder()
          .build();

      observer.onNext(response);
      observer.onCompleted();
    });

    // Should complete without emitting any events
    StepVerifier.create(flux)
        .verifyComplete();

    // Verify only the initial request was sent (no ack for null event)
    verify(mockRequestStream, times(1)).onNext(any());
  }

  @Test
  void testEmptyPubsubName() {
    Flux<String> flux = Flux.create(sink -> {
      EventSubscriberStreamObserver<String> observer = new EventSubscriberStreamObserver<>(
          mockStub,
          sink,
          TypeRef.STRING,
          objectSerializer
      );

      DaprProtos.SubscribeTopicEventsRequestAlpha1 initialRequest = buildInitialRequest("pubsub", "topic");
      observer.start(initialRequest);

      // Send event with empty pubsub name
      DaprProtos.SubscribeTopicEventsResponseAlpha1 response = DaprProtos.SubscribeTopicEventsResponseAlpha1.newBuilder()
          .setEventMessage(
              DaprAppCallbackProtos.TopicEventRequest.newBuilder()
                  .setId("event-1")
                  .setPubsubName("")
                  .setTopic("topic")
                  .setData(ByteString.copyFromUtf8("\"Hello\""))
                  .build()
          )
          .build();

      observer.onNext(response);
      observer.onCompleted();
    });

    // Should complete without emitting any events
    StepVerifier.create(flux)
        .verifyComplete();

    // Verify only the initial request was sent
    verify(mockRequestStream, times(1)).onNext(any());
  }

  @Test
  void testEmptyEventId() {
    Flux<String> flux = Flux.create(sink -> {
      EventSubscriberStreamObserver<String> observer = new EventSubscriberStreamObserver<>(
          mockStub,
          sink,
          TypeRef.STRING,
          objectSerializer
      );

      DaprProtos.SubscribeTopicEventsRequestAlpha1 initialRequest = buildInitialRequest("pubsub", "topic");
      observer.start(initialRequest);

      // Send event with empty ID
      DaprProtos.SubscribeTopicEventsResponseAlpha1 response = DaprProtos.SubscribeTopicEventsResponseAlpha1.newBuilder()
          .setEventMessage(
              DaprAppCallbackProtos.TopicEventRequest.newBuilder()
                  .setId("")
                  .setPubsubName("pubsub")
                  .setTopic("topic")
                  .setData(ByteString.copyFromUtf8("\"Hello\""))
                  .build()
          )
          .build();

      observer.onNext(response);
      observer.onCompleted();
    });

    // Should complete without emitting any events
    StepVerifier.create(flux)
        .verifyComplete();

    // Verify only the initial request was sent
    verify(mockRequestStream, times(1)).onNext(any());
  }

  @Test
  void testNullData() {
    Flux<String> flux = Flux.create(sink -> {
      EventSubscriberStreamObserver<String> observer = new EventSubscriberStreamObserver<>(
          mockStub,
          sink,
          null, // null type
          objectSerializer
      );

      DaprProtos.SubscribeTopicEventsRequestAlpha1 initialRequest = buildInitialRequest("pubsub", "topic");
      observer.start(initialRequest);

      // Send event with valid structure but null type
      observer.onNext(buildEventResponse("event-1", "pubsub", "topic", "Hello"));

      observer.onCompleted();
    });

    // Should complete without emitting any events (data is null when type is null)
    StepVerifier.create(flux)
        .verifyComplete();

    // Verify initial request + ack were sent
    verify(mockRequestStream, times(2)).onNext(any());
  }

  @Test
  void testComplexObjectSerialization() throws IOException {
    // Test with a custom object
    TestEvent testEvent = new TestEvent("test-name", 42);
    byte[] serializedEvent = objectSerializer.serialize(testEvent);

    Flux<TestEvent> flux = Flux.create(sink -> {
      EventSubscriberStreamObserver<TestEvent> observer = new EventSubscriberStreamObserver<>(
          mockStub,
          sink,
          TypeRef.get(TestEvent.class),
          objectSerializer
      );

      DaprProtos.SubscribeTopicEventsRequestAlpha1 initialRequest = buildInitialRequest("pubsub", "topic");
      observer.start(initialRequest);

      DaprProtos.SubscribeTopicEventsResponseAlpha1 response = DaprProtos.SubscribeTopicEventsResponseAlpha1.newBuilder()
          .setEventMessage(
              DaprAppCallbackProtos.TopicEventRequest.newBuilder()
                  .setId("event-1")
                  .setPubsubName("pubsub")
                  .setTopic("topic")
                  .setData(ByteString.copyFrom(serializedEvent))
                  .build()
          )
          .build();

      observer.onNext(response);
      observer.onCompleted();
    });

    StepVerifier.create(flux)
        .expectNextMatches(event -> event.name.equals("test-name") && event.value == 42)
        .verifyComplete();
  }

  @Test
  void testErrorDuringSendingAck() {
    // Mock request stream to throw exception when sending ack
    doThrow(new RuntimeException("Failed to send ack"))
        .when(mockRequestStream).onNext(argThat(req -> req.hasEventProcessed()));

    Flux<String> flux = Flux.create(sink -> {
      EventSubscriberStreamObserver<String> observer = new EventSubscriberStreamObserver<>(
          mockStub,
          sink,
          TypeRef.STRING,
          objectSerializer
      );

      DaprProtos.SubscribeTopicEventsRequestAlpha1 initialRequest = buildInitialRequest("pubsub", "topic");
      observer.start(initialRequest);

      // Send an event - this should trigger an error when trying to send ack
      observer.onNext(buildEventResponse("event-1", "pubsub", "topic", "Hello"));
    });

    // The event is emitted successfully, then the error occurs when sending ack
    StepVerifier.create(flux)
        .expectNext("Hello")  // Event is emitted before ack
        .expectError(DaprException.class)  // Then error when sending ack
        .verify();
  }

  // Helper methods

  private DaprProtos.SubscribeTopicEventsRequestAlpha1 buildInitialRequest(String pubsubName, String topic) {
    return DaprProtos.SubscribeTopicEventsRequestAlpha1.newBuilder()
        .setInitialRequest(
            DaprProtos.SubscribeTopicEventsRequestInitialAlpha1.newBuilder()
                .setPubsubName(pubsubName)
                .setTopic(topic)
                .build()
        )
        .build();
  }

  private DaprProtos.SubscribeTopicEventsResponseAlpha1 buildEventResponse(
      String eventId,
      String pubsubName,
      String topic,
      String data) {
    try {
      byte[] serializedData = objectSerializer.serialize(data);
      return DaprProtos.SubscribeTopicEventsResponseAlpha1.newBuilder()
          .setEventMessage(
              DaprAppCallbackProtos.TopicEventRequest.newBuilder()
                  .setId(eventId)
                  .setPubsubName(pubsubName)
                  .setTopic(topic)
                  .setData(ByteString.copyFrom(serializedData))
                  .build()
          )
          .build();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // Test class for complex object serialization
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
