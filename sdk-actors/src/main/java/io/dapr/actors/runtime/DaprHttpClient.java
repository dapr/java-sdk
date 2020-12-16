/*
 * Copyright (c) Microsoft Corporation.
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
   * Base URL for Dapr Actor APIs.
   */
  private static final String ACTORS_BASE_URL = DaprHttp.API_VERSION + "/" + "actors";

  /**
   * String format for Actors state management relative url.
   */
  private static final String ACTOR_STATE_KEY_RELATIVE_URL_FORMAT = ACTORS_BASE_URL + "/%s/%s/state/%s";

  /**
   * String format for Actors state management relative url.
   */
  private static final String ACTOR_STATE_RELATIVE_URL_FORMAT = ACTORS_BASE_URL + "/%s/%s/state";

  /**
   * String format for Actors reminder registration relative url.
   */
  private static final String ACTOR_REMINDER_RELATIVE_URL_FORMAT = ACTORS_BASE_URL + "/%s/%s/reminders/%s";

  /**
   * String format for Actors timer registration relative url.
   */
  private static final String ACTOR_TIMER_RELATIVE_URL_FORMAT = ACTORS_BASE_URL + "/%s/%s/timers/%s";

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
    String url = String.format(ACTOR_STATE_KEY_RELATIVE_URL_FORMAT, actorType, actorId, keyName);
    Mono<DaprHttp.Response> responseMono =
        this.client.invokeApi(DaprHttp.HttpMethods.GET.name(), url, null, "", null, null);
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

    String url = String.format(ACTOR_STATE_RELATIVE_URL_FORMAT, actorType, actorId);
    return this.client.invokeApi(DaprHttp.HttpMethods.PUT.name(), url, null, payload, null, null).then();
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
    String url = String.format(ACTOR_REMINDER_RELATIVE_URL_FORMAT, actorType, actorId, reminderName);
    return Mono.fromCallable(() -> INTERNAL_SERIALIZER.serialize(reminderParams))
        .flatMap(data ->
            this.client.invokeApi(DaprHttp.HttpMethods.PUT.name(), url, null, data, null, null)
        ).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> unregisterReminder(String actorType, String actorId, String reminderName) {
    String url = String.format(ACTOR_REMINDER_RELATIVE_URL_FORMAT, actorType, actorId, reminderName);
    return this.client.invokeApi(DaprHttp.HttpMethods.DELETE.name(), url, null, null, null).then();
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
          String url = String.format(ACTOR_TIMER_RELATIVE_URL_FORMAT, actorType, actorId, timerName);
          return this.client.invokeApi(DaprHttp.HttpMethods.PUT.name(), url, null, data, null, null);
        }).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> unregisterTimer(String actorType, String actorId, String timerName) {
    String url = String.format(ACTOR_TIMER_RELATIVE_URL_FORMAT, actorType, actorId, timerName);
    return this.client.invokeApi(DaprHttp.HttpMethods.DELETE.name(), url, null, null, null).then();
  }

}
