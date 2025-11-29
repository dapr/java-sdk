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
import io.dapr.v1.DaprProtos;
import io.grpc.stub.StreamObserver;
import reactor.core.publisher.FluxSink;

import java.util.concurrent.atomic.AtomicReference;

/**
 * StreamObserver implementation for subscribing to Dapr pub/sub events.
 * <p>
 * This class handles the bidirectional gRPC streaming for event subscriptions, including:
 * <ul>
 *   <li>Receiving events from Dapr</li>
 *   <li>Deserializing event payloads</li>
 *   <li>Emitting deserialized data to a Reactor Flux</li>
 *   <li>Sending acknowledgments (SUCCESS/RETRY) back to Dapr</li>
 * </ul>
 * </p>
 *
 * @param <T> The type of the event payload
 */
public class EventSubscriberStreamObserver<T> implements StreamObserver<DaprProtos.SubscribeTopicEventsResponseAlpha1> {

  private final FluxSink<T> sink;
  private final TypeRef<T> type;
  private final DaprObjectSerializer objectSerializer;
  private final AtomicReference<StreamObserver<DaprProtos.SubscribeTopicEventsRequestAlpha1>> requestStreamRef;

  /**
   * Creates a new EventSubscriberStreamObserver.
   *
   * @param sink              The FluxSink to emit deserialized events to
   * @param type              The TypeRef for deserializing event payloads
   * @param objectSerializer  The serializer to use for deserialization
   * @param requestStreamRef  Reference to the request stream for sending acknowledgments
   */
  public EventSubscriberStreamObserver(
      FluxSink<T> sink,
      TypeRef<T> type,
      DaprObjectSerializer objectSerializer,
      AtomicReference<StreamObserver<DaprProtos.SubscribeTopicEventsRequestAlpha1>> requestStreamRef) {
    this.sink = sink;
    this.type = type;
    this.objectSerializer = objectSerializer;
    this.requestStreamRef = requestStreamRef;
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

      requestStreamRef.get().onNext(ack);
    } catch (Exception e) {
      // On error during processing, send RETRY acknowledgment
      try {
        var id = response.getEventMessage().getId();

        if (id != null && !id.isEmpty()) {
          var ack = buildRetryAck(id);

          requestStreamRef.get().onNext(ack);
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
   * <p>
   * This method directly uses the protobuf enum instead of depending on
   * {@code SubscriptionListener.Status} to keep this class independent
   * of the older callback-based API.
   * </p>
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
