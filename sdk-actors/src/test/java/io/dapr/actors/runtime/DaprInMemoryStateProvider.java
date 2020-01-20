/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import io.dapr.actors.ActorId;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Fake state provider for tests in Actors - data is kept in memory only.
 */
public class DaprInMemoryStateProvider extends DaprStateAsyncProvider {

  private static final Map<String, byte[]> stateStore = new HashMap<>();

  private final ObjectSerializer serializer;

  DaprInMemoryStateProvider(ObjectSerializer serializer) {
    super(null, null);
    this.serializer = serializer;
  }

  @Override
  <T> Mono<T> load(String actorType, ActorId actorId, String stateName, Class<T> clazz) {
    return Mono.fromSupplier(() -> {
      try {
        String stateId = this.buildId(actorType, actorId, stateName);
        if (!stateStore.containsKey(stateId)) {
          throw new IllegalStateException("State not found.");
        }

        return this.serializer.deserialize(this.stateStore.get(stateId), clazz);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Override
  Mono<Boolean> contains(String actorType, ActorId actorId, String stateName) {
    return Mono.fromSupplier(() -> stateStore.containsKey(this.buildId(actorType, actorId, stateName)));
  }

  @Override
  Mono<Void> apply(String actorType, ActorId actorId, ActorStateChange... stateChanges) {
    return Mono.fromRunnable(() -> {
      try {
        for (ActorStateChange stateChange : stateChanges) {
          String stateId = buildId(actorType, actorId, stateChange.getStateName());
          switch (stateChange.getChangeKind()) {
            case REMOVE:
              stateStore.remove(stateId);
              break;
            case ADD:
            case UPDATE:
              byte[] raw = this.serializer.serialize(stateChange.getValue());
              stateStore.put(stateId, raw);
              break;
          }
        }

      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  private static final String buildId(String actorType, ActorId actorId, String stateName) {
    return String.format("%s||%s||%s", actorType, actorId.toString(), stateName);
  }
}
