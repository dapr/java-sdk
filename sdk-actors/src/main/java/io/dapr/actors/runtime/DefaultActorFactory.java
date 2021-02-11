/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import io.dapr.actors.ActorId;
import io.dapr.actors.ActorTrace;

import java.lang.reflect.Constructor;

/**
 * Instantiates actors by calling their constructor with {@link ActorRuntimeContext} and {@link ActorId}.
 *
 * @param <T> Actor Type to be created.
 */
class DefaultActorFactory<T extends AbstractActor> implements ActorFactory<T> {

  /**
   * Tracing errors, warnings and info logs.
   */
  private static final ActorTrace ACTOR_TRACE = new ActorTrace();

  /**
   * {@inheritDoc}
   */
  @Override
  public T createActor(ActorRuntimeContext<T> actorRuntimeContext, ActorId actorId) {
    try {
      if (actorRuntimeContext == null) {
        return null;
      }

      Constructor<T> constructor = actorRuntimeContext
          .getActorTypeInformation()
          .getImplementationClass()
          .getConstructor(ActorRuntimeContext.class, ActorId.class);
      return constructor.newInstance(actorRuntimeContext, actorId);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      ACTOR_TRACE.writeError(
            actorRuntimeContext.getActorTypeInformation().getName(),
            actorId.toString(),
            "Failed to create actor instance.");
      throw new RuntimeException(e);
    }
  }

}
