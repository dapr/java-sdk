/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import io.dapr.client.DaprHttp;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * A DaprClient over HTTP for Actor's runtime.
 */
class DaprHttpClient implements DaprClient {

  /**
   * Internal serializer for state.
   */
  private static final ActorObjectSerializer INTERNAL_SERIALIZER = new ActorObjectSerializer();

  /**
   * Shared Json Factory as per Jackson's documentation, used only for this class.
   */
  private static final JsonFactory JSON_FACTORY = new JsonFactory();

  /**
   * The HTTP client to be used.
   *
   * @see DaprHttp
   */
  private final DaprHttp client;

  /**
   * Internal constructor.
   *
   * @param client Dapr's http client.
   */
  DaprHttpClient(DaprHttp client) {
    this.client = client;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<byte[]> getState(String actorType, String actorId, String keyName) {
    String[] pathSegments = new String[] { DaprHttp.API_VERSION, "actors", actorType, actorId, "state", keyName };
    Mono<DaprHttp.Response> responseMono =
        this.client.invokeApi(DaprHttp.HttpMethods.GET.name(), pathSegments, null, "", null, null);
    return responseMono.map(r -> {
      if ((r.getStatusCode() != 200) && (r.getStatusCode() != 204)) {
        throw new IllegalStateException(
            String.format("Error getting actor state: %s/%s/%s", actorType, actorId, keyName));
      }
      return r.getBody();
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> saveStateTransactionally(
      String actorType,
      String actorId,
      List<ActorStateOperation> operations) {
    // Constructing the JSON via a stream API to avoid creating transient objects to be instantiated.
    byte[] payload = null;
    try (ByteArrayOutputStream writer = new ByteArrayOutputStream()) {
      JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
      // Start array
      generator.writeStartArray();

      for (ActorStateOperation stateOperation : operations) {
        // Start operation object.
        generator.writeStartObject();
        generator.writeStringField("operation", stateOperation.getOperationType());

        // Start request object.
        generator.writeObjectFieldStart("request");
        generator.writeStringField("key", stateOperation.getKey());

        Object value = stateOperation.getValue();
        if (value != null) {
          if (value instanceof String) {
            // DefaultObjectSerializer is a JSON serializer, so we just pass it on.
            generator.writeFieldName("value");
            generator.writeRawValue((String) value);
          } else if (value instanceof byte[]) {
            // Custom serializer uses byte[].
            // DefaultObjectSerializer is just a passthrough for byte[], so we handle it here too.
            generator.writeBinaryField("value", (byte[]) value);
          } else {
            return Mono.error(() -> {
              throw new IllegalArgumentException("Actor state value must be String or byte[]");
            });
          }
        }
        // End request object.
        generator.writeEndObject();

        // End operation object.
        generator.writeEndObject();
      }

      // End array
      generator.writeEndArray();

      generator.close();
      writer.flush();
      payload = writer.toByteArray();
    } catch (IOException e) {
      return Mono.error(e);
    }

    String[] pathSegments = new String[] { DaprHttp.API_VERSION, "actors", actorType, actorId, "state" };
    return this.client.invokeApi(DaprHttp.HttpMethods.PUT.name(), pathSegments, null, payload, null, null).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> registerReminder(
      String actorType,
      String actorId,
      String reminderName,
      ActorReminderParams reminderParams) {
    String[] pathSegments = new String[] {
        DaprHttp.API_VERSION,
        "actors",
        actorType,
        actorId,
        "reminders",
        reminderName
    };
    return Mono.fromCallable(() -> INTERNAL_SERIALIZER.serialize(reminderParams))
        .flatMap(data ->
            this.client.invokeApi(DaprHttp.HttpMethods.PUT.name(), pathSegments, null, data, null, null)
        ).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> unregisterReminder(String actorType, String actorId, String reminderName) {
    String[] pathSegments = new String[] {
        DaprHttp.API_VERSION,
        "actors",
        actorType,
        actorId,
        "reminders",
        reminderName
    };
    return this.client.invokeApi(DaprHttp.HttpMethods.DELETE.name(), pathSegments, null, null, null).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> registerTimer(
      String actorType,
      String actorId,
      String timerName,
      ActorTimerParams timerParams) {
    return Mono.fromCallable(() -> INTERNAL_SERIALIZER.serialize(timerParams))
        .flatMap(data -> {
          String[] pathSegments = new String[] {
              DaprHttp.API_VERSION,
              "actors",
              actorType,
              actorId,
              "timers",
              timerName
          };
          return this.client.invokeApi(DaprHttp.HttpMethods.PUT.name(), pathSegments, null, data, null, null);
        }).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> unregisterTimer(String actorType, String actorId, String timerName) {
    String[] pathSegments = new String[] { DaprHttp.API_VERSION, "actors", actorType, actorId, "timers", timerName };
    return this.client.invokeApi(DaprHttp.HttpMethods.DELETE.name(), pathSegments, null, null, null).then();
  }

}
