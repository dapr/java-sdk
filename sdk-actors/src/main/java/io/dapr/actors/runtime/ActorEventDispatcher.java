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

package io.dapr.actors.runtime;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.InvalidProtocolBufferException;
import io.dapr.client.Headers;
import io.dapr.v1.DaprActorsProtos.ActorEntityConfig;
import io.dapr.v1.DaprActorsProtos.SubscribeActorEventsRequestAlpha1;
import io.dapr.v1.DaprActorsProtos.SubscribeActorEventsRequestDeactivateResponseAlpha1;
import io.dapr.v1.DaprActorsProtos.SubscribeActorEventsRequestFailedAlpha1;
import io.dapr.v1.DaprActorsProtos.SubscribeActorEventsRequestInitialAlpha1;
import io.dapr.v1.DaprActorsProtos.SubscribeActorEventsRequestInvokeResponseAlpha1;
import io.dapr.v1.DaprActorsProtos.SubscribeActorEventsRequestReminderResponseAlpha1;
import io.dapr.v1.DaprActorsProtos.SubscribeActorEventsResponseAlpha1;
import io.dapr.v1.DaprActorsProtos.SubscribeActorEventsResponseDeactivateRequestAlpha1;
import io.dapr.v1.DaprActorsProtos.SubscribeActorEventsResponseInvokeRequestAlpha1;
import io.dapr.v1.DaprActorsProtos.SubscribeActorEventsResponseReminderRequestAlpha1;
import io.dapr.v1.DaprActorsProtos.SubscribeActorEventsResponseTimerRequestAlpha1;
import io.grpc.Status;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * Maps messages received on the actor event stream to {@link ActorRuntime} calls, and their results back to
 * stream messages.
 */
final class ActorEventDispatcher {

  private static final JsonFactory JSON_FACTORY = new JsonFactory();

  private static final String CONTENT_TYPE = "content-type";

  private ActorEventDispatcher() {
  }

  /**
   * Builds the first message sent on the stream, which registers the actor types hosted by this app.
   *
   * @param config Actor runtime configuration.
   * @return Initial request.
   */
  static SubscribeActorEventsRequestAlpha1 buildInitialRequest(ActorRuntimeConfig config) {
    SubscribeActorEventsRequestInitialAlpha1.Builder builder = SubscribeActorEventsRequestInitialAlpha1.newBuilder()
        .addAllEntities(config.getRegisteredActorTypes());
    if (config.getActorIdleTimeout() != null) {
      builder.setActorIdleTimeout(toProto(config.getActorIdleTimeout()));
    }
    if (config.getDrainOngoingCallTimeout() != null) {
      builder.setDrainOngoingCallTimeout(toProto(config.getDrainOngoingCallTimeout()));
    }
    if (config.getDrainBalancedActors() != null) {
      builder.setDrainRebalancedActors(config.getDrainBalancedActors());
    }

    for (ActorTypeConfig typeConfig : config.getActorTypeConfigs()) {
      ActorEntityConfig.Builder entityConfig = ActorEntityConfig.newBuilder()
          .addEntities(typeConfig.getActorTypeName());
      if (typeConfig.getActorIdleTimeout() != null) {
        entityConfig.setActorIdleTimeout(toProto(typeConfig.getActorIdleTimeout()));
      }
      if (typeConfig.getDrainOngoingCallTimeout() != null) {
        entityConfig.setDrainOngoingCallTimeout(toProto(typeConfig.getDrainOngoingCallTimeout()));
      }
      if (typeConfig.getDrainBalancedActors() != null) {
        entityConfig.setDrainRebalancedActors(typeConfig.getDrainBalancedActors());
      }
      builder.addEntitiesConfig(entityConfig);
    }

    return SubscribeActorEventsRequestAlpha1.newBuilder().setInitialRequest(builder).build();
  }

  /**
   * Dispatches a callback received from Dapr to the actor runtime.
   *
   * @param runtime Actor runtime.
   * @param message Callback received on the stream.
   * @return The response to send back on the stream. Errors are mapped to a request_failed message.
   */
  static Mono<SubscribeActorEventsRequestAlpha1> dispatch(
      ActorRuntime runtime, SubscribeActorEventsResponseAlpha1 message) {
    switch (message.getResponseTypeCase()) {
      case INVOKE_REQUEST:
        return invoke(runtime, message.getInvokeRequest());
      case REMINDER_REQUEST:
        return reminder(runtime, message.getReminderRequest());
      case TIMER_REQUEST:
        return timer(runtime, message.getTimerRequest());
      case DEACTIVATE_REQUEST:
        return deactivate(runtime, message.getDeactivateRequest());
      default:
        return Mono.error(new IllegalArgumentException(
            "Unexpected actor event: " + message.getResponseTypeCase()));
    }
  }

  /**
   * Rejects a callback received while the subscription is closing, so Dapr does not wait for a reply that would
   * be dropped.
   *
   * @param message Callback received on the stream.
   * @return The request_failed message to send back on the stream.
   */
  static SubscribeActorEventsRequestAlpha1 rejectClosing(SubscribeActorEventsResponseAlpha1 message) {
    return failed(requestId(message), Status.Code.UNAVAILABLE, "actor event subscription is closing");
  }

  private static String requestId(SubscribeActorEventsResponseAlpha1 message) {
    switch (message.getResponseTypeCase()) {
      case INVOKE_REQUEST:
        return message.getInvokeRequest().getId();
      case REMINDER_REQUEST:
        return message.getReminderRequest().getId();
      case TIMER_REQUEST:
        return message.getTimerRequest().getId();
      case DEACTIVATE_REQUEST:
        return message.getDeactivateRequest().getId();
      default:
        return "";
    }
  }

  private static Mono<SubscribeActorEventsRequestAlpha1> invoke(
      ActorRuntime runtime, SubscribeActorEventsResponseInvokeRequestAlpha1 request) {
    String id = request.getId();
    if (!isRegistered(runtime, request.getActorType())) {
      return Mono.just(notRegistered(id, request.getActorType()));
    }

    byte[] payload = request.getData().isEmpty() ? null : request.getData().toByteArray();
    String contentType = runtime.getObjectSerializer(request.getActorType()).getContentType();
    return runtime.invoke(request.getActorType(), request.getActorId(), request.getMethod(), payload,
            activationId(request.getMetadataMap()))
        .map(ByteString::copyFrom)
        .defaultIfEmpty(ByteString.EMPTY)
        .map(data -> SubscribeActorEventsRequestAlpha1.newBuilder()
            .setInvokeResponse(SubscribeActorEventsRequestInvokeResponseAlpha1.newBuilder()
                .setId(id)
                .setData(data)
                .putMetadata(CONTENT_TYPE, contentType))
            .build())
        .onErrorResume(e -> Mono.just(failed(id, e)));
  }

  private static Mono<SubscribeActorEventsRequestAlpha1> reminder(
      ActorRuntime runtime, SubscribeActorEventsResponseReminderRequestAlpha1 request) {
    String id = request.getId();
    if (!isRegistered(runtime, request.getActorType())) {
      return Mono.just(notRegistered(id, request.getActorType()));
    }

    return Mono.fromCallable(() -> buildCallbackBody(
            null, request.getDueTime(), request.getPeriod(), request.hasData() ? request.getData() : null))
        .flatMap(body -> runtime.invokeReminder(request.getActorType(), request.getActorId(), request.getName(), body,
            activationId(request.getMetadataMap())))
        .then(Mono.fromSupplier(() -> SubscribeActorEventsRequestAlpha1.newBuilder()
            .setReminderResponse(SubscribeActorEventsRequestReminderResponseAlpha1.newBuilder().setId(id))
            .build()))
        .onErrorResume(e -> Mono.just(failed(id, e)));
  }

  private static Mono<SubscribeActorEventsRequestAlpha1> timer(
      ActorRuntime runtime, SubscribeActorEventsResponseTimerRequestAlpha1 request) {
    String id = request.getId();
    if (!isRegistered(runtime, request.getActorType())) {
      return Mono.just(notRegistered(id, request.getActorType()));
    }

    return Mono.fromCallable(() -> buildCallbackBody(
            request.getCallback(), request.getDueTime(), request.getPeriod(),
            request.hasData() ? request.getData() : null))
        .flatMap(body -> runtime.invokeTimer(request.getActorType(), request.getActorId(), request.getName(), body,
            activationId(request.getMetadataMap())))
        .then(Mono.fromSupplier(() -> SubscribeActorEventsRequestAlpha1.newBuilder()
            .setTimerResponse(SubscribeActorEventsRequestReminderResponseAlpha1.newBuilder().setId(id))
            .build()))
        .onErrorResume(e -> Mono.just(failed(id, e)));
  }

  private static Mono<SubscribeActorEventsRequestAlpha1> deactivate(
      ActorRuntime runtime, SubscribeActorEventsResponseDeactivateRequestAlpha1 request) {
    String id = request.getId();
    if (!isRegistered(runtime, request.getActorType())) {
      return Mono.just(notRegistered(id, request.getActorType()));
    }

    return runtime.deactivate(request.getActorType(), request.getActorId())
        .then(Mono.fromSupplier(() -> SubscribeActorEventsRequestAlpha1.newBuilder()
            .setDeactivateResponse(SubscribeActorEventsRequestDeactivateResponseAlpha1.newBuilder().setId(id))
            .build()))
        .onErrorResume(e -> Mono.just(failed(id, e)));
  }

  /**
   * Builds the same JSON body Dapr sends to the HTTP reminder and timer callbacks, so the existing
   * {@link ActorObjectSerializer} parsing is reused.
   */
  private static byte[] buildCallbackBody(String callback, String dueTime, String period, Any data)
      throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (JsonGenerator generator = JSON_FACTORY.createGenerator(output)) {
      generator.writeStartObject();
      if (callback != null) {
        generator.writeStringField("callback", callback);
      }
      // Dapr always sends both fields over HTTP, empty when unset, and the serializer reads an empty one as zero.
      generator.writeStringField("dueTime", dueTime);
      generator.writeStringField("period", period);
      byte[] raw = unwrapData(data);
      if (raw != null && raw.length > 0) {
        generator.writeFieldName("data");
        generator.writeRawValue(new String(raw, StandardCharsets.UTF_8));
      }
      generator.writeEndObject();
    }
    return output.toByteArray();
  }

  /**
   * Dapr stores reminder and timer data as a BytesValue holding JSON. Over HTTP it sends that JSON as is.
   */
  private static byte[] unwrapData(Any data) throws InvalidProtocolBufferException {
    if (data == null) {
      return null;
    }
    if (data.is(BytesValue.class)) {
      return data.unpack(BytesValue.class).getValue().toByteArray();
    }
    return data.getValue().toByteArray();
  }

  private static boolean isRegistered(ActorRuntime runtime, String actorType) {
    return runtime.getConfig().getRegisteredActorTypes().contains(actorType);
  }

  private static String activationId(Map<String, String> metadata) {
    for (Map.Entry<String, String> entry : metadata.entrySet()) {
      if (Headers.DAPR_ACTOR_ACTIVATION_ID.equalsIgnoreCase(entry.getKey()) && !entry.getValue().isEmpty()) {
        return entry.getValue();
      }
    }
    return null;
  }

  private static SubscribeActorEventsRequestAlpha1 notRegistered(String id, String actorType) {
    return failed(id, Status.Code.NOT_FOUND,
        String.format("Actor type %s is not registered with Actor runtime.", actorType));
  }

  private static SubscribeActorEventsRequestAlpha1 failed(String id, Throwable error) {
    // Only the method lookup itself is a not-found: a NoSuchMethodException thrown by the actor's own code
    // arrives wrapped, and must stay retryable.
    if (error instanceof NoSuchMethodException) {
      return failed(id, Status.Code.NOT_FOUND, error.getMessage());
    }
    return failed(id, Status.Code.UNKNOWN, error.toString());
  }

  private static SubscribeActorEventsRequestAlpha1 failed(String id, Status.Code code, String message) {
    return SubscribeActorEventsRequestAlpha1.newBuilder()
        .setRequestFailed(SubscribeActorEventsRequestFailedAlpha1.newBuilder()
            .setId(id)
            .setCode(code.value())
            .setMessage(message == null ? "" : message))
        .build();
  }

  private static com.google.protobuf.Duration toProto(Duration duration) {
    return com.google.protobuf.Duration.newBuilder()
        .setSeconds(duration.getSeconds())
        .setNanos(duration.getNano())
        .build();
  }
}
