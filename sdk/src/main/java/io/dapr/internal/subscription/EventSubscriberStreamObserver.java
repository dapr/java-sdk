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

package io.dapr.internal.subscription;

import io.dapr.client.domain.CloudEvent;
import io.dapr.exceptions.DaprException;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.utils.TypeRef;
import io.dapr.v1.DaprAppCallbackProtos;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprPubsubProtos;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.FluxSink;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * StreamObserver implementation for subscribing to Dapr pub/sub events.
 * Thread Safety: This class relies on gRPC's StreamObserver contract, which guarantees that
 * onNext(), onError(), and onCompleted() are never called concurrently and always from the
 * same thread. Therefore, no additional synchronization is needed.
 *
 * @param <T> The type of the event payload
 */
public class EventSubscriberStreamObserver<T>
    implements StreamObserver<DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1> {

  private static final Logger logger = LoggerFactory.getLogger(EventSubscriberStreamObserver.class);

  private final DaprGrpc.DaprStub stub;
  private final FluxSink<T> sink;
  private final TypeRef<T> type;
  private final DaprObjectSerializer objectSerializer;

  private StreamObserver<DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1> requestStream;

  /**
   * Creates a new EventSubscriberStreamObserver.
   *
   * @param stub              The gRPC stub for making Dapr service calls
   * @param sink              The FluxSink to emit deserialized event data to
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

  private static DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 buildSuccessAck(String eventId) {
    return buildAckRequest(eventId, DaprAppCallbackProtos.TopicEventResponse.TopicEventResponseStatus.SUCCESS);
  }

  private static DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 buildRetryAck(String eventId) {
    return buildAckRequest(eventId, DaprAppCallbackProtos.TopicEventResponse.TopicEventResponseStatus.RETRY);
  }

  @Override
  public void onError(Throwable throwable) {
    sink.error(DaprException.propagate(throwable));
  }

  @Override
  public void onCompleted() {
    sink.complete();
  }

  private static DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 buildDropAck(String eventId) {
    return buildAckRequest(eventId, DaprAppCallbackProtos.TopicEventResponse.TopicEventResponseStatus.DROP);
  }

  private T deserializeEventData(DaprAppCallbackProtos.TopicEventRequest message) throws IOException {
    if (type == null) {
      logger.debug("Type is null, skipping deserialization for event ID: {}", message.getId());
      return null;
    }

    // Check if the user requested CloudEvent<T> - we need to construct it from protobuf fields
    if (isCloudEventType(type)) {
      return buildCloudEventFromMessage(message);
    }

    return objectSerializer.deserialize(message.getData().toByteArray(), type);
  }

  private boolean isCloudEventType(TypeRef<T> typeRef) {
    Type t = typeRef.getType();

    if (t instanceof ParameterizedType) {
      ParameterizedType pt = (ParameterizedType) t;
      return pt.getRawType() == CloudEvent.class;
    }

    return t == CloudEvent.class;
  }

  @SuppressWarnings("unchecked")
  private T buildCloudEventFromMessage(DaprAppCallbackProtos.TopicEventRequest message) throws IOException {
    // Extract inner type from CloudEvent<T>
    TypeRef<?> innerType = extractInnerType(type);

    // Deserialize the data field into the inner type
    Object data;
    if (innerType != null) {
      data = objectSerializer.deserialize(message.getData().toByteArray(), innerType);
    } else {
      data = message.getData().toStringUtf8();
    }

    // Build CloudEvent from protobuf fields
    CloudEvent<Object> cloudEvent = new CloudEvent<>();
    cloudEvent.setId(message.getId());
    cloudEvent.setSource(message.getSource());
    cloudEvent.setType(message.getType());
    cloudEvent.setSpecversion(message.getSpecVersion());
    cloudEvent.setDatacontenttype(message.getDataContentType());
    cloudEvent.setData(data);
    cloudEvent.setTopic(message.getTopic());
    cloudEvent.setPubsubName(message.getPubsubName());

    return (T) cloudEvent;
  }

  private TypeRef<?> extractInnerType(TypeRef<T> cloudEventType) {
    Type t = cloudEventType.getType();

    if (t instanceof ParameterizedType) {
      ParameterizedType pt = (ParameterizedType) t;
      Type[] typeArgs = pt.getActualTypeArguments();
      if (typeArgs.length > 0) {
        return TypeRef.get(typeArgs[0]);
      }
    }

    return null; // Raw CloudEvent without type parameter
  }

  private void emitDataAndAcknowledge(T data, String eventId) {
    // Only emit if data is not null (Reactor doesn't allow null values in Flux)
    if (data != null) {
      sink.next(data);
    }

    // Send SUCCESS acknowledgment
    requestStream.onNext(buildSuccessAck(eventId));
  }

  private void handleDeserializationError(String eventId, IOException cause) {
    logger.error("Deserialization failed for event ID: {}, sending DROP ack", eventId, cause);

    // Send DROP ack - cannot process malformed data
    requestStream.onNext(buildDropAck(eventId));

    // Propagate error to sink
    sink.error(new DaprException("DESERIALIZATION_ERROR",
        "Failed to deserialize event with ID: " + eventId, cause));
  }

  private void handleProcessingError(String eventId, Exception cause) {
    logger.error("Processing error for event ID: {}, attempting to send RETRY ack", eventId, cause);

    try {
      // Try to send RETRY acknowledgment
      requestStream.onNext(buildRetryAck(eventId));
    } catch (Exception ackException) {
      // Failed to send ack - this is critical
      logger.error("Failed to send RETRY ack for event ID: {}", eventId, ackException);
      sink.error(DaprException.propagate(ackException));

      return;
    }

    // Propagate the original processing error
    sink.error(DaprException.propagate(cause));
  }

  private static DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 buildAckRequest(
      String eventId,
      DaprAppCallbackProtos.TopicEventResponse.TopicEventResponseStatus status) {
    DaprPubsubProtos.SubscribeTopicEventsRequestProcessedAlpha1 eventProcessed =
        DaprPubsubProtos.SubscribeTopicEventsRequestProcessedAlpha1.newBuilder()
            .setId(eventId)
            .setStatus(
                DaprAppCallbackProtos.TopicEventResponse.newBuilder()
                    .setStatus(status)
                    .build())
            .build();

    return DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1.newBuilder()
        .setEventProcessed(eventProcessed)
        .build();
  }

  /**
   * Starts the subscription by sending the initial request.
   *
   * @param request The subscription request
   * @return The StreamObserver to send further requests (acknowledgments)
   */
  public StreamObserver<DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1> start(
      DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 request
  ) {
    requestStream = stub.subscribeTopicEventsAlpha1(this);

    requestStream.onNext(request);

    return requestStream;
  }

  @Override
  public void onNext(DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1 response) {
    if (!isValidEventMessage(response)) {
      return;
    }

    DaprAppCallbackProtos.TopicEventRequest message = response.getEventMessage();
    String eventId = message.getId();

    try {
      T data = deserializeEventData(message);
      emitDataAndAcknowledge(data, eventId);
    } catch (IOException e) {
      // Deserialization failure - send DROP ack
      handleDeserializationError(eventId, e);
    } catch (Exception e) {
      // Processing failure - send RETRY ack
      handleProcessingError(eventId, e);
    }
  }

  private boolean isValidEventMessage(DaprPubsubProtos.SubscribeTopicEventsResponseAlpha1 response) {
    if (response.getEventMessage() == null) {
      logger.debug("Received response with null event message, skipping");
      return false;
    }

    DaprAppCallbackProtos.TopicEventRequest message = response.getEventMessage();

    if (message.getPubsubName() == null || message.getPubsubName().isEmpty()) {
      logger.debug("Received event with empty pubsub name, skipping");
      return false;
    }

    if (message.getId() == null || message.getId().isEmpty()) {
      logger.debug("Received event with empty ID, skipping");
      return false;
    }

    return true;
  }
}
