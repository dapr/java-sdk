/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collection;

/**
 * State Provider to interact with Dapr runtime to handle state.
 */
class DaprStateAsyncProvider {

  /**
   * Shared Json serializer/deserializer as per Jackson's documentation, used only for this class.
   */
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final AppToDaprAsyncClient daprAsyncClient;

  private final ActorStateProviderSerializer serializer;

  DaprStateAsyncProvider(AppToDaprAsyncClient daprAsyncClient, ActorStateProviderSerializer serializer) {
    this.daprAsyncClient = daprAsyncClient;
    this.serializer = serializer;
  }

  <T> Mono<T> load(String actorType, String actorId, String stateName, Class<T> clazz) {
    Mono<String> result = this.daprAsyncClient.getState(actorType, actorId, stateName);

    return result.map(s -> {
      if (s == null) {
        return (T)null;
      }

      try {
        return this.serializer.deserialize(s, clazz);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  Mono<Boolean> contains(String actorType, String actorId, String stateName) {
    Mono<String> result = this.daprAsyncClient.getState(actorType, actorId, stateName);

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
  Mono<Void> apply(String actorType, String actorId, Collection<ActorStateChange> stateChanges)
  {
    if ((stateChanges == null) || stateChanges.isEmpty()) {
      return Mono.just(null);
    }

    // Constructing the JSON "manually" to avoid creating transient classes to be parsed.
    ArrayNode operations = OBJECT_MAPPER.createArrayNode();
    for (ActorStateChange stateChange : stateChanges) {
      if ((stateChange == null) || (stateChange.getChangeKind() == null)) {
        continue;
      }

      String operationName = stateChange.getChangeKind().getDaprStateChangeOperation();
      if ((operationName == null) || (operationName.length() == 0)) {
        continue;
      }

      try {
        ObjectNode operation = OBJECT_MAPPER.createObjectNode();
        operation.set("operation", operation.textNode(operationName));
        ObjectNode request = OBJECT_MAPPER.createObjectNode();
        request.put("key", stateChange.getStateName());
        if ((stateChange.getChangeKind() == ActorStateChangeKind.UPDATE) || (stateChange.getChangeKind() == ActorStateChangeKind.ADD)) {
          request.put("value", this.serializer.serialize(stateChange.getValue()));
        }

        operations.add(operation);
      } catch (IOException e) {
        e.printStackTrace();
        return Mono.error(e);
      }
    }

    if (operations.size() == 0) {
      // No-op since there is no operation to be performed.
      Mono.just(null);
    }

    try {
      return this.daprAsyncClient.saveStateTransactionally(actorType, actorId, OBJECT_MAPPER.writeValueAsString(operations));
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      return Mono.error(e);
    }
  }
}
