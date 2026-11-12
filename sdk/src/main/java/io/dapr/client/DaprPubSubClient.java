/*
 * Copyright 2026 The Dapr Authors
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

package io.dapr.client;

import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.dapr.client.domain.BulkPublishEntry;
import io.dapr.client.domain.BulkPublishRequest;
import io.dapr.client.domain.BulkPublishResponse;
import io.dapr.client.domain.BulkPublishResponseFailedEntry;
import io.dapr.client.domain.CloudEvent;
import io.dapr.client.domain.PublishEventRequest;
import io.dapr.exceptions.DaprException;
import io.dapr.internal.subscription.EventSubscriberStreamObserver;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.utils.DefaultContentTypeConverter;
import io.dapr.utils.TypeRef;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprPubsubProtos;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs the pub/sub calls of {@link DaprClientImpl}: publishing and streaming subscriptions.
 */
final class DaprPubSubClient {

  private final Logger logger = LoggerFactory.getLogger(DaprClientImpl.class);

  private final DaprGrpcCalls grpcCalls;

  private final DaprObjectSerializer objectSerializer;

  DaprPubSubClient(DaprGrpcCalls grpcCalls, DaprObjectSerializer objectSerializer) {
    this.grpcCalls = grpcCalls;
    this.objectSerializer = objectSerializer;
  }

  Mono<Void> publishEvent(PublishEventRequest request) {
    try {
      String pubsubName = request.getPubsubName();
      String topic = request.getTopic();
      Object data = request.getData();

      // Content-type can be overwritten on a per-request basis.
      // It allows CloudEvents to be handled differently, for example.
      String contentType = request.getContentType();
      byte[] serializedData;
      if (Strings.isNullOrEmpty(contentType)) {
        serializedData = objectSerializer.serialize(data);
        contentType = objectSerializer.getContentType();
      } else if (objectSerializer instanceof DefaultObjectSerializer
          && DefaultContentTypeConverter.isStringContentType(contentType)
          && (data instanceof String || data instanceof Number || data instanceof Boolean)) {
        // Text content is sent as-is, otherwise the JSON serializer would quote it and the
        // payload would not match the given content type.
        serializedData = DefaultContentTypeConverter.convertEventToBytesForGrpc(data, contentType);
      } else {
        serializedData = objectSerializer.serialize(data);
      }

      DaprPubsubProtos.PublishEventRequest.Builder envelopeBuilder = DaprPubsubProtos.PublishEventRequest.newBuilder()
          .setTopic(topic)
          .setPubsubName(pubsubName)
          .setData(ByteString.copyFrom(serializedData))
          .setDataContentType(contentType);

      Map<String, String> metadata = request.getMetadata();
      if (metadata != null) {
        envelopeBuilder.putAllMetadata(metadata);
      }

      return Mono.deferContextual(
          context ->
              grpcCalls.<Empty>createMono(
                  it -> grpcCalls.intercept(context).publishEvent(envelopeBuilder.build(), it)
              )
      ).then();
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  <T> Mono<BulkPublishResponse<T>> publishEvents(BulkPublishRequest<T> request) {
    try {
      String pubsubName = request.getPubsubName();
      String topic = request.getTopic();
      DaprPubsubProtos.BulkPublishRequest.Builder envelopeBuilder = DaprPubsubProtos.BulkPublishRequest.newBuilder();
      envelopeBuilder.setTopic(topic);
      envelopeBuilder.setPubsubName(pubsubName);

      if (Strings.isNullOrEmpty(pubsubName) || Strings.isNullOrEmpty(topic)) {
        throw new IllegalArgumentException("pubsubName and topic name cannot be null or empty");
      }

      for (BulkPublishEntry<?> entry : request.getEntries()) {
        Object event = entry.getEvent();
        byte[] data;
        String contentType = entry.getContentType();
        try {
          // Serialize event into bytes
          if (!Strings.isNullOrEmpty(contentType) && objectSerializer instanceof DefaultObjectSerializer) {
            // If content type is given by user and default object serializer is used
            data = DefaultContentTypeConverter.convertEventToBytesForGrpc(event, contentType);
          } else {
            // perform the serialization as per user given input of serializer
            // this is also the case when content type is empty

            data = objectSerializer.serialize(event);

            if (Strings.isNullOrEmpty(contentType)) {
              // Only override content type if not given in input by user
              contentType = objectSerializer.getContentType();
            }
          }
        } catch (IOException ex) {
          throw DaprException.propagate(ex);
        }

        DaprPubsubProtos.BulkPublishRequestEntry.Builder reqEntryBuilder = DaprPubsubProtos.BulkPublishRequestEntry
            .newBuilder()
            .setEntryId(entry.getEntryId())
            .setEvent(ByteString.copyFrom(data))
            .setContentType(contentType);
        Map<String, String> metadata = entry.getMetadata();
        if (metadata != null) {
          reqEntryBuilder.putAllMetadata(metadata);
        }
        envelopeBuilder.addEntries(reqEntryBuilder.build());
      }

      // Set metadata if available
      Map<String, String> metadata = request.getMetadata();
      if (metadata != null) {
        envelopeBuilder.putAllMetadata(metadata);
      }

      Map<String, BulkPublishEntry<T>> entryMap = new HashMap<>();
      for (BulkPublishEntry<T> entry : request.getEntries()) {
        entryMap.put(entry.getEntryId(), entry);
      }
      return Mono.deferContextual(
          context ->
              grpcCalls.<DaprPubsubProtos.BulkPublishResponse>createMono(
                  it -> grpcCalls.intercept(context).bulkPublishEvent(envelopeBuilder.build(), it)
              ).onErrorResume(throwable -> {
                // Check if the error is UNIMPLEMENTED, and try the alpha API just in case
                // The exception might be wrapped in DaprException, so check both
                Throwable cause = throwable;
                if (throwable instanceof DaprException && throwable.getCause() != null) {
                  cause = throwable.getCause();
                }
                
                if (cause instanceof io.grpc.StatusRuntimeException) {
                  io.grpc.StatusRuntimeException statusException = (io.grpc.StatusRuntimeException) cause;
                  if (statusException.getStatus().getCode() == io.grpc.Status.Code.UNIMPLEMENTED) {
                    return grpcCalls.<DaprPubsubProtos.BulkPublishResponse>createMono(
                        it -> grpcCalls.intercept(context).bulkPublishEventAlpha1(envelopeBuilder.build(), it)
                    );
                  }
                }
                return Mono.error(throwable);
              })
      ).map(
          it -> {
            List<BulkPublishResponseFailedEntry<T>> entries = new ArrayList<>();
            for (DaprPubsubProtos.BulkPublishResponseFailedEntry entry : it.getFailedEntriesList()) {
              BulkPublishResponseFailedEntry<T> domainEntry = new BulkPublishResponseFailedEntry<T>(
                  entryMap.get(entry.getEntryId()),
                  entry.getError());
              entries.add(domainEntry);
            }
            if (entries.size() > 0) {
              return new BulkPublishResponse<>(entries);
            }
            return new BulkPublishResponse<>();
          }
      );
    } catch (RuntimeException ex) {
      return DaprException.wrapMono(ex);
    }
  }

  <T> Subscription subscribeToEvents(
      String pubsubName, String topic, SubscriptionListener<T> listener, TypeRef<T> type) {
    return subscribeToEvents(pubsubName, topic, null, listener, type);
  }

  <T> Subscription subscribeToEvents(
      String pubsubName,
      String topic,
      String deadLetterTopic,
      SubscriptionListener<T> listener,
      TypeRef<T> type) {
    DaprPubsubProtos.SubscribeTopicEventsRequestInitialAlpha1.Builder initialRequestBuilder =
        DaprPubsubProtos.SubscribeTopicEventsRequestInitialAlpha1.newBuilder()
            .setTopic(topic)
            .setPubsubName(pubsubName);
    if (deadLetterTopic != null && !deadLetterTopic.isEmpty()) {
      initialRequestBuilder.setDeadLetterTopic(deadLetterTopic);
    }
    DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 request =
        DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1.newBuilder()
            .setInitialRequest(initialRequestBuilder.build())
            .build();
    return buildSubscription(listener, type, request);
  }

  @Deprecated
  <T> Flux<T> subscribeToEvents(String pubsubName, String topic, TypeRef<T> type) {
    return subscribeToTopic(pubsubName, topic, type);
  }

  @Deprecated
  <T> Flux<T> subscribeToEvents(String pubsubName, String topic, TypeRef<T> type, Map<String, String> metadata) {
    return subscribeToTopic(pubsubName, topic, type, metadata);
  }

  <T> Flux<T> subscribeToTopic(String pubsubName, String topic, TypeRef<T> type) {
    return subscribeToTopic(pubsubName, topic, null, type, null);
  }

  <T> Flux<T> subscribeToTopic(String pubsubName, String topic, TypeRef<T> type, Map<String, String> metadata) {
    return subscribeToTopic(pubsubName, topic, null, type, metadata);
  }

  <T> Flux<T> subscribeToTopic(String pubsubName, String topic, String deadLetterTopic, TypeRef<T> type) {
    return subscribeToTopic(pubsubName, topic, deadLetterTopic, type, null);
  }

  <T> Flux<T> subscribeToTopic(
      String pubsubName,
      String topic,
      String deadLetterTopic,
      TypeRef<T> type,
      Map<String, String> metadata) {
    DaprPubsubProtos.SubscribeTopicEventsRequestInitialAlpha1.Builder initialRequestBuilder =
        DaprPubsubProtos.SubscribeTopicEventsRequestInitialAlpha1.newBuilder()
            .setTopic(topic)
            .setPubsubName(pubsubName);

    if (metadata != null && !metadata.isEmpty()) {
      initialRequestBuilder.putAllMetadata(metadata);
    }

    if (deadLetterTopic != null && !deadLetterTopic.isEmpty()) {
      initialRequestBuilder.setDeadLetterTopic(deadLetterTopic);
    }

    DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 request =
        DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1.newBuilder()
            .setInitialRequest(initialRequestBuilder.build())
            .build();

    return Flux.create(sink -> {
      DaprGrpc.DaprStub interceptedStub = this.grpcCalls.intercept();
      EventSubscriberStreamObserver<T> eventSubscriber = new EventSubscriberStreamObserver<>(
          interceptedStub,
          sink,
          type,
          this.objectSerializer
      );
      StreamObserver<DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1> requestStream = eventSubscriber.start(request);

      // Cleanup when Flux is cancelled or completed
      sink.onDispose(() -> {
        try {
          requestStream.onCompleted();
        } catch (Exception e) {
          logger.debug("Completing the subscription stream resulted in an error: {}", e.getMessage());
        }
      });
    }, FluxSink.OverflowStrategy.BUFFER);
  }

  @Nonnull
  private <T> Subscription<T> buildSubscription(
      SubscriptionListener<T> listener,
      TypeRef<T> type,
      DaprPubsubProtos.SubscribeTopicEventsRequestAlpha1 request) {
    var interceptedStub = this.grpcCalls.intercept();
    Subscription<T> subscription = new Subscription<>(interceptedStub, request, listener, response -> {
      if (response.getEventMessage() == null) {
        return null;
      }

      var message = response.getEventMessage();
      if ((message.getPubsubName() == null) || message.getPubsubName().isEmpty()) {
        return null;
      }

      try {
        CloudEvent<T> cloudEvent = new CloudEvent<>();
        T object = null;
        if (type != null) {
          object = DaprPubSubClient.this.objectSerializer.deserialize(message.getData().toByteArray(), type);
        }
        cloudEvent.setData(object);
        cloudEvent.setDatacontenttype(message.getDataContentType());
        cloudEvent.setId(message.getId());
        cloudEvent.setTopic(message.getTopic());
        cloudEvent.setSpecversion(message.getSpecVersion());
        cloudEvent.setType(message.getType());
        cloudEvent.setPubsubName(message.getPubsubName());
        return cloudEvent;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    subscription.start();
    return subscription;
  }
}
