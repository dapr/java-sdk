/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.client;

import io.dapr.actors.ActorId;

/**
 *
 * @author Swen Schisler <swen.schisler@fourtytwosoft.io>
 */
public interface IActorProxyFactory {

  /**
   * Creates a proxy to the actor object that implements an actor interface.
   *
   * <typeparam name="TActorInterface">
   * The actor interface implemented by the remote actor object. The returned
   * proxy object will implement this interface.
   * </typeparam>
   *
   * @param actorId Actor Id of the proxy actor object. Methods called on this
   * proxy will result in requests being sent to the actor with this id.
   * @param actorType Type of actor implementation.
   * @return An actor proxy object that implements IActorProxy and
   * TActorInterface.
   */
  //TODO TActorInterface is not defined anywhere, must be synthesized, how to handle in JAVA
  /**
   * TActorInterface CreateActorProxy<TActorInterface>( ActorId actorId, String
   * actorType ) where TActorInterface : IActor ;
   */
  /**
   * Creates an Actor Proxy for making calls without Remoting.
   *
   * @param actorId Actor Id.
   * @param actorType Type of actor.
   * @return Actor proxy to interact with remote actor object.
   */
  ActorProxy Create(ActorId actorId, String actorType);
}
