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
   * Information on the {@link Actor} type being serviced.
   */
  private final ActorTypeInformation<T> actorTypeInformation;

  /**
   * Instantiates the default factory for Actors of a given type.
   * @param actorTypeInformation Information of the actor type for this instance.
   */
  DefaultActorFactory(ActorTypeInformation<T> actorTypeInformation) {
    this.actorTypeInformation = actorTypeInformation;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T createActor(ActorService actorService, ActorId actorId) {
    try {
      if (this.actorTypeInformation == null) {
        return null;
      }

      Constructor<T> constructor = this
        .actorTypeInformation
        .getImplementationClass()
        .getConstructor(ActorService.class, ActorId.class);
      return constructor.newInstance(actorService, actorId);
    } catch (ReflectiveOperationException e) {
      e.printStackTrace();
    }
    return null;
  }

}
