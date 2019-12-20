/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import io.dapr.actors.ActorId;
import reactor.core.publisher.Mono;

// TODO
class ActorStateManager<T extends AbstractActor> {

  private final String actorTypeName;

  private final ActorId actorId;

  ActorStateManager(String actorTypeName, ActorId actorId) {
    this.actorTypeName = actorTypeName;
    this.actorId = actorId;
  }

  Mono<Void> SaveState() {
    return Mono.empty();
  }
}
