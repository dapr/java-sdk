/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import io.dapr.actors.ActorId;

/**
 * Creates an actor of a given type.
 *
 * @param <T> Actor Type to be created.
 */
@FunctionalInterface
public interface ActorFactory<T extends AbstractActor> {

  /**
   * Creates an Actor.
   *
   * @param actorRuntimeContext Actor type's context in the runtime.
   * @param actorId             Actor Id.
   * @return Actor or null it failed.
   */
  T createActor(ActorRuntimeContext<T> actorRuntimeContext, ActorId actorId);
}
