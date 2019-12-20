/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import io.dapr.actors.ActorId;

/**
 * Implementation of the Actor Service that contains a state provider.
 */
class ActorServiceImpl<T extends AbstractActor> implements ActorService<T> {

  /**
   * Customizable factory for Actors.
   */
  private final ActorFactory<T> actorFactory;

  /**
   * State provider for Actors.
   */
  private final DaprStateAsyncProvider stateProvider;

  /**
   * Information on the {@link Actor} type being serviced.
   */
  private final ActorTypeInformation<T> actorTypeInformation;

  /**
   * Instantiates a stateful service for a given {@link Actor} type.
   * @param actorTypeInformation Information on the {@link Actor} type being serviced.
   * @param stateProvider State provider for Actors.
   * @param actorFactory Customizable factor for Actors.
   */
  public ActorServiceImpl(ActorTypeInformation<T> actorTypeInformation, DaprStateAsyncProvider stateProvider, ActorFactory<T> actorFactory) {
    this.actorTypeInformation = actorTypeInformation;
    this.actorFactory = actorFactory;
    this.stateProvider = stateProvider;
  }

  /**
   * Gets the state provider for {@link Actor}.
   * @return State provider.
   */
  DaprStateAsyncProvider getStateProvider() {
    return stateProvider;
  }

  /**
   * Gets the information on the {@link Actor} Type.
   * @return Information on the {@link Actor} Type.
   */
  ActorTypeInformation<T> getActorTypeInformation() {
    return actorTypeInformation;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T createActor(ActorId actorId) {
    return this.actorFactory.createActor(this, actorId);
  }
}
