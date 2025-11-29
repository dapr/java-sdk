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
limitations under the License.
*/

package io.dapr.internal.subscription;

import io.dapr.exceptions.DaprException;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.utils.TypeRef;
import io.dapr.v1.DaprAppCallbackProtos;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprProtos;
import io.grpc.stub.StreamObserver;
import reactor.core.publisher.FluxSink;

/**
 * StreamObserver implementation for subscribing to Dapr pub/sub events.
 * Thread Safety: This class relies on gRPC's StreamObserver contract, which guarantees that
 * onNext(), onError(), and onCompleted() are never called concurrently and always from the
 * same thread. Therefore, no additional synchronization is needed.
 *
 * @param <T> The type of the event payload
 */
public class EventSubscriberStreamObserver<T> implements StreamObserver<DaprProtos.SubscribeTopicEventsResponseAlpha1> {

  private final DaprGrpc.DaprStub stub;
  private final FluxSink<T> sink;
  private final TypeRef<T> type;
  private final DaprObjectSerializer objectSerializer;

  private StreamObserver<DaprProtos.SubscribeTopicEventsRequestAlpha1> requestStream;

  /**
   * Creates a new EventSubscriberStreamObserver.
   *
   * @param stub              The gRPC stub for making Dapr service calls
   * @param sink              The FluxSink to emit deserialized events to
   * @param type              The TypeRef for deserializing event payloads
   * @param objectSerializer  The serializer to use for deserialization
   */
  public EventSubscriberStreamObserver(
      DaprGrpc.DaprStub stub,
      FluxSink<T> sink,
      TypeRef<T> type,
      DaprObjectSerializer objectSerializer) {
    this.stub = stub;
    this.sink = sink;
    this.type = type;
    this.objectSerializer = objectSerializer;
  }

  /** Starts the subscription by sending the initial request.
   *
   * @param request The subscription request
   * @return The StreamObserver to send further requests (acknowledgments)
   */
  public StreamObserver<DaprProtos.SubscribeTopicEventsRequestAlpha1> start(
      DaprProtos.SubscribeTopicEventsRequestAlpha1 request
  ) {
    requestStream = stub.subscribeTopicEventsAlpha1(this);

    requestStream.onNext(request);

    return requestStream;
  }

  @Override
  public void onNext(DaprProtos.SubscribeTopicEventsResponseAlpha1 response) {
    try {
      if (response.getEventMessage() == null) {
        return;
      }

      DaprAppCallbackProtos.TopicEventRequest message = response.getEventMessage();
      String pubsubName = message.getPubsubName();

      if (pubsubName == null || pubsubName.isEmpty()) {
        return;
      }

      String id = message.getId();

      if (id == null || id.isEmpty()) {
        return;
      }

      // Deserialize the event data
      T data = null;

      if (type != null) {
        data = objectSerializer.deserialize(message.getData().toByteArray(), type);
      }

      // Emit the data to the Flux (only if not null)
      if (data != null) {
        sink.next(data);
      }

      // Send SUCCESS acknowledgment directly
      var ack = buildSuccessAck(id);

      requestStream.onNext(ack);
    } catch (Exception e) {
      // On error during processing, send RETRY acknowledgment
      try {
        var id = response.getEventMessage().getId();

        if (id != null && !id.isEmpty()) {
          var ack = buildRetryAck(id);

          requestStream.onNext(ack);
        }
      } catch (Exception ex) {
        // If we can't send ack, propagate the error
        sink.error(DaprException.propagate(ex));
        return;
      }

      sink.error(DaprException.propagate(e));
    }
  }

  @Override
  public void onError(Throwable throwable) {
    sink.error(DaprException.propagate(throwable));
  }

  @Override
  public void onCompleted() {
    sink.complete();
  }

  /**
   * Builds a SUCCESS acknowledgment request.
   *
   * @param eventId The ID of the event to acknowledge
   * @return The acknowledgment request
   */
  private static DaprProtos.SubscribeTopicEventsRequestAlpha1 buildSuccessAck(String eventId) {
    return buildAckRequest(eventId, DaprAppCallbackProtos.TopicEventResponse.TopicEventResponseStatus.SUCCESS);
  }

  /**
   * Builds a RETRY acknowledgment request.
   *
   * @param eventId The ID of the event to acknowledge
   * @return The acknowledgment request
   */
  private static DaprProtos.SubscribeTopicEventsRequestAlpha1 buildRetryAck(String eventId) {
    return buildAckRequest(eventId, DaprAppCallbackProtos.TopicEventResponse.TopicEventResponseStatus.RETRY);
  }

  /**
   * Builds a DROP acknowledgment request.
   *
   * @param eventId The ID of the event to acknowledge
   * @return The acknowledgment request
   */
  @SuppressWarnings("unused")  // May be used in the future
  private static DaprProtos.SubscribeTopicEventsRequestAlpha1 buildDropAck(String eventId) {
    return buildAckRequest(eventId, DaprAppCallbackProtos.TopicEventResponse.TopicEventResponseStatus.DROP);
  }

  /**
   * Builds an acknowledgment request with the specified status.
   * 
   * @param eventId The ID of the event to acknowledge
   * @param status  The acknowledgment status (SUCCESS, RETRY, or DROP)
   * @return The acknowledgment request
   */
  private static DaprProtos.SubscribeTopicEventsRequestAlpha1 buildAckRequest(
      String eventId,
      DaprAppCallbackProtos.TopicEventResponse.TopicEventResponseStatus status) {
    DaprProtos.SubscribeTopicEventsRequestProcessedAlpha1 eventProcessed =
        DaprProtos.SubscribeTopicEventsRequestProcessedAlpha1.newBuilder()
            .setId(eventId)
            .setStatus(
                DaprAppCallbackProtos.TopicEventResponse.newBuilder()
                    .setStatus(status)
                    .build())
            .build();

    return DaprProtos.SubscribeTopicEventsRequestAlpha1.newBuilder()
        .setEventProcessed(eventProcessed)
        .build();
  }
}
