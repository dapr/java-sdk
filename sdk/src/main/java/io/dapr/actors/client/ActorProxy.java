/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.client;

import io.dapr.actors.ActorId;
import io.dapr.actors.communication.IActorMessageBodyFactory;
import io.dapr.actors.communication.client.ActorNonRemotingClient;
import io.dapr.actors.communication.client.ActorRemotingClient;

/**
 * Provides the base implementation for the proxy to the remote actor objects
 * implementing {@link #IActor} interfaces. The proxy object can be used used
 * for client-to-actor and actor-to-actor communication.
 *
 * @author Swen Schisler <swen.schisler@fourtytwosoft.io>
 */
public class ActorProxy implements IActorProxy {

  static ActorProxyFactory DefaultProxyFactory = new ActorProxyFactory();
  private ActorRemotingClient actorRemotingClient;
  private ActorNonRemotingClient actorNonRemotingClient;

  private ActorId actorId;
  private IActorMessageBodyFactory ActorMessageBodyFactory;

  /**
   *
   * Initializes a new instance of the {@link #ActorProxy} class. This
   * constructor is protected so that it can be used by generated class which
   * derives from ActorProxy when making Remoting calls. This constructor is
   * also marked as internal so that it can be called by ActorProxyFactory when
   * making non-remoting calls.
   *
   */
  protected ActorProxy() {
  }

  public ActorId getActorId() {
    return actorId;
  }

  public void setActorId(ActorId actorId) {
    this.actorId = actorId;
  }

  public IActorMessageBodyFactory getActorMessageBodyFactory() {
    return ActorMessageBodyFactory;
  }

  public void setActorMessageBodyFactory(IActorMessageBodyFactory ActorMessageBodyFactory) {
    this.ActorMessageBodyFactory = ActorMessageBodyFactory;
  }

  /**
   * Creates a proxy to the actor object that implements an actor interface.
   *
   * @param TActorInterface The actor interface implemented by the remote actor
   * object. The returned proxy object will implement this interface.
   *
   * @param actorId The actor ID of the proxy actor object. Methods called on
   * this proxy will result in requests being sent to the actor with this ID.
   * @param actorType Type of actor implementation.
   *
   * @return Proxy to the actor object.
   */
  public static TActorInterface Create(ActorId actorId, String actorType) {
    where TActorInterface : IActor
  }

  {
    return DefaultProxyFactory.CreateActorProxy(actorId, actorType);
  }

  @Override
  public String getActorType() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

}
