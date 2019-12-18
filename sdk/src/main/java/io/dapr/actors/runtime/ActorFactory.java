/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import io.dapr.actors.ActorId;

/**
 * Creates an actor of a given type.
 * @param <T> Actor Type to be created.
 */
@FunctionalInterface
public interface ActorFactory<T extends AbstractActor> {

  /**
   * Creates an Actor.
   * @param actorService Actor Service.
   * @param actorId Actor Id.
   * @return Actor or null it failed.
   */
  T createActor(ActorService actorService, ActorId actorId);
}
