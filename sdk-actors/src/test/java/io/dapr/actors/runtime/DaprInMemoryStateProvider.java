/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import io.dapr.actors.ActorId;
import io.dapr.client.DaprClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

public class DaprInMemoryStateProvider extends DaprStateAsyncProvider {

  private static final Map<String, byte[]> stateStore = new HashMap<>();

  private final ActorStateSerializer serializer;

  DaprInMemoryStateProvider(ActorStateSerializer serializer) {
    super(null, serializer);
    this.serializer = serializer;
  }

  @Override
  <T> Mono<T> load(String actorType, ActorId actorId, String stateName, Class<T> clazz) {
    try {
      String stateId = this.buildId(actorType, actorId, stateName);
      if (!stateStore.containsKey(stateId)) {
        return Mono.error(new IllegalStateException("State not found."));
      }

      return Mono.just(this.serializer.deserialize(this.stateStore.get(stateId), clazz));
    } catch (Exception e) {
      return Mono.error(e);
    }
  }

  @Override
  Mono<Boolean> contains(String actorType, ActorId actorId, String stateName) {
    return Mono.just(stateStore.containsKey(this.buildId(actorType, actorId, stateName)));
  }

  @Override
  Mono<Void> apply(String actorType, ActorId actorId, ActorStateChange... stateChanges) {
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
      return Mono.error(e);
    }
    return Mono.empty();
  }

  private static final String buildId(String actorType, ActorId actorId, String stateName) {
    return String.format("%s||%s||%s", actorType, actorId.toString(), stateName);
  }
}
