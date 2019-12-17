/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.client;

import io.dapr.actors.ActorId;

/**
 *
 * Provides the interface for implementation of proxy access for actor service.
 *
 * @author Swen Schisler <swen.schisler@fourtytwosoft.io>
 */
public interface IActorProxy {

  /**
   * Gets {@link dapr.actors.ActorId} associated with the proxy object.
   *
   * @return ActorId {@link dapr.actors#ActorId} associated with the proxy
   * object.
   *
   */
  ActorId getActorId();

  /**
   * Gets actor implementation type of the actor associated with the proxy
   * object.
   *
   * @return Actor implementation type of the actor associated with the proxy
   * object.
   *
   */
  String getActorType();
}
