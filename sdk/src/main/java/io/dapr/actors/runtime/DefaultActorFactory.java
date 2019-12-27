/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import io.dapr.actors.ActorId;

import java.lang.reflect.Constructor;

/**
 * Instantiates actors by calling their constructor with {@link ActorService} and {@link ActorId}.
 * @param <T> Actor Type to be created.
 */
class DefaultActorFactory<T extends AbstractActor> implements ActorFactory<T> {

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
    } catch (Exception e) {
      //TODO: Use ActorTrace.
      e.printStackTrace();
    }
    return null;
  }

}
