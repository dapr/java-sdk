/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import io.dapr.actors.ActorId;

/**
 * Implementation of the Actor Service that contains a state provider.
 */
class ActorServiceImpl implements ActorService {

  /**
   * Customizable factory for Actors.
   */
  private final ActorFactory actorFactory;

  /**
   * State provider for Actors.
   */
  private final DaprStateAsyncProvider stateProvider;

  /**
   * Information on the {@link Actor} type being serviced.
   */
  private final ActorTypeInformation actorTypeInformation;

  /**
   * Instantiates a stateful service for a given {@link Actor} type.
   * @param actorTypeInformation Information on the {@link Actor} type being serviced.
   * @param stateProvider State provider for Actors.
   * @param actorFactory Customizable factor for Actors.
   */
  public ActorServiceImpl(ActorTypeInformation actorTypeInformation, DaprStateAsyncProvider stateProvider, ActorFactory actorFactory) {
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
  ActorTypeInformation getActorTypeInformation() {
    return actorTypeInformation;
  }

  /**
   * Creates an {@link Actor} for this service.
   * @param actorId Identifier for the Actor to be created.
   * @return New {@link Actor} instance.
   */
  @Override
  public AbstractActor createActor(ActorId actorId) {
    return this.actorFactory.createActor(this, actorId);
  }
}
