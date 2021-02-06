/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.actors.ActorId;
import io.dapr.config.Properties;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.utils.TypeRef;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 * State Provider to interact with Dapr runtime to handle state.
 */
class DaprStateAsyncProvider {

  /**
   * Dapr's charset.
   */
  private static final Charset CHARSET = Properties.STRING_CHARSET.get();

  /**
   * Handles special serialization cases.
   */
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * Dapr's client for Actor runtime.
   */
  private final DaprClient daprClient;

  /**
   * Serializer for state objects.
   */
  private final DaprObjectSerializer stateSerializer;

  /**
   * Flag determining if state serializer is the default serializer instead of user provided.
   */
  private final boolean isStateSerializerDefault;

  /**
   * Instantiates a new Actor's state provider.
   *
   * @param daprClient      Dapr client for Actor runtime.
   * @param stateSerializer Serializer for state objects.
   */
  DaprStateAsyncProvider(DaprClient daprClient, DaprObjectSerializer stateSerializer) {
    this.daprClient = daprClient;
    this.stateSerializer = stateSerializer;
    this.isStateSerializerDefault = stateSerializer.getClass() == DefaultObjectSerializer.class;
  }

  <T> Mono<T> load(String actorType, ActorId actorId, String stateName, TypeRef<T> type) {
    Mono<byte[]> result = this.daprClient.getState(actorType, actorId.toString(), stateName);

    return result.flatMap(s -> {
      try {
        if (s == null) {
          return Mono.empty();
        }

        T response = this.stateSerializer.deserialize(s, type);
        if (this.isStateSerializerDefault && (response instanceof byte[])) {
          if (s.length == 0) {
            return Mono.empty();
          }
          // Default serializer just passes through byte arrays, so we need to decode it here.
          response = (T) OBJECT_MAPPER.readValue(s, byte[].class);
        }
        if (response == null) {
          return Mono.empty();
        }

        return Mono.just(response);
      } catch (IOException e) {
        return Mono.error(new RuntimeException(e));
      }
    });
  }

  Mono<Boolean> contains(String actorType, ActorId actorId, String stateName) {
    Mono<byte[]> result = this.daprClient.getState(actorType, actorId.toString(), stateName);
    return result.map(s -> s.length > 0).defaultIfEmpty(false);
  }

  /**
   * Saves state changes transactionally.
   * [
   * {
   * "operation": "upsert",
   * "request": {
   * "key": "key1",
   * "value": "myData"
   * }
   * },
   * {
   * "operation": "delete",
   * "request": {
   * "key": "key2"
   * }
   * }
   * ]
   *
   * @param actorType    Name of the actor being changed.
   * @param actorId      Identifier of the actor being changed.
   * @param stateChanges Collection of changes to be performed transactionally.
   * @return Void.
   */
  Mono<Void> apply(String actorType, ActorId actorId, ActorStateChange... stateChanges) {
    if ((stateChanges == null) || stateChanges.length == 0) {
      return Mono.empty();
    }

    ArrayList<ActorStateOperation> operations = new ArrayList<>(stateChanges.length);
    for (ActorStateChange stateChange : stateChanges) {
      if ((stateChange == null) || (stateChange.getChangeKind() == null)) {
        continue;
      }

      String operationName = stateChange.getChangeKind().getDaprStateChangeOperation();
      if ((operationName == null) || (operationName.length() == 0)) {
        continue;
      }

      String key = stateChange.getStateName();
      Object value = null;
      if ((stateChange.getChangeKind() == ActorStateChangeKind.UPDATE)
          || (stateChange.getChangeKind() == ActorStateChangeKind.ADD)) {
        try {
          byte[] data = this.stateSerializer.serialize(stateChange.getValue());
          if (data != null) {
            if (this.isStateSerializerDefault && !(stateChange.getValue() instanceof byte[])) {
              // DefaultObjectSerializer is a JSON serializer, so we just pass it on.
              value = new String(data, CHARSET);
            } else {
              // Custom serializer uses byte[].
              // DefaultObjectSerializer is just a passthrough for byte[], so we handle it here too.
              value = data;
            }
          }
        } catch (IOException e) {
          return Mono.error(e);
        }
      }

      operations.add(new ActorStateOperation(operationName, key, value));
    }

    return this.daprClient.saveStateTransactionally(actorType, actorId.toString(), operations);
  }

}
