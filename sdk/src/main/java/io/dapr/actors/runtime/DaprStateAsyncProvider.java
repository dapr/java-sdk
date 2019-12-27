/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import io.dapr.actors.ActorId;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * State Provider to interact with Dapr runtime to handle state.
 */
class DaprStateAsyncProvider {

  /**
   * Shared Json Factory as per Jackson's documentation, used only for this class.
   */
  private static final JsonFactory JSON_FACTORY = new JsonFactory();

  private final AppToDaprAsyncClient daprAsyncClient;

  private final ActorStateSerializer serializer;

  DaprStateAsyncProvider(AppToDaprAsyncClient daprAsyncClient, ActorStateSerializer serializer) {
    this.daprAsyncClient = daprAsyncClient;
    this.serializer = serializer;
  }

  <T> Mono<T> load(String actorType, ActorId actorId, String stateName, Class<T> clazz) {
    Mono<String> result = this.daprAsyncClient.getState(actorType, actorId.toString(), stateName);

    return result
            .filter(s -> (s != null) && (!s.isEmpty()))
            .map(s -> {
              try {
                return this.serializer.deserialize(s, clazz);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
  }

  Mono<Boolean> contains(String actorType, ActorId actorId, String stateName) {
    Mono<String> result = this.daprAsyncClient.getState(actorType, actorId.toString(), stateName);

    return result.map(s -> {
      return (s != null) && (s.length() > 0);
    });
  }

  /**
   * Saves state changes transactionally.
   *             [
   *                 {
   *                     "operation": "upsert",
   *                     "request": {
   *                         "key": "key1",
   *                         "value": "myData"
   *                     }
   *                 },
   *                 {
   *                     "operation": "delete",
   *                     "request": {
   *                         "key": "key2"
   *                     }
   *                 }
   *             ]
   * @param actorType Name of the actor being changed.
   * @param actorId Identifier of the actor being changed.
   * @param stateChanges Collection of changes to be performed transactionally.
   * @return Void.
   */
  Mono<Void> apply(String actorType, ActorId actorId, ActorStateChange... stateChanges)
  {
    if ((stateChanges == null) || stateChanges.length == 0) {
      return Mono.empty();
    }

    int count = 0;
    // Constructing the JSON via a stream API to avoid creating transient objects to be instantiated.
    String payload = null;
    try (Writer writer = new StringWriter()) {
      JsonGenerator generator = JSON_FACTORY.createGenerator(writer);
      // Start array
      generator.writeStartArray();

      for (ActorStateChange stateChange : stateChanges) {
        if ((stateChange == null) || (stateChange.getChangeKind() == null)) {
          continue;
        }

        String operationName = stateChange.getChangeKind().getDaprStateChangeOperation();
        if ((operationName == null) || (operationName.length() == 0)) {
          continue;
        }

        count++;

        // Start operation object.
        generator.writeStartObject();
        generator.writeStringField("operation", operationName);

        // Start request object.
        generator.writeObjectFieldStart("request");
        generator.writeStringField("key", stateChange.getStateName());
        if ((stateChange.getChangeKind() == ActorStateChangeKind.UPDATE) || (stateChange.getChangeKind() == ActorStateChangeKind.ADD)) {
          generator.writeStringField("value", this.serializer.serialize(stateChange.getValue()));
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
      payload = writer.toString();
    } catch (IOException e) {
      e.printStackTrace();
      return Mono.error(e);
    }

    if (count == 0) {
      // No-op since there is no operation to be performed.
      Mono.empty();
    }

    return this.daprAsyncClient.saveStateTransactionally(actorType, actorId.toString(), payload);
  }
}
