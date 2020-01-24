package io.dapr.actors.client;

import io.dapr.actors.ActorId;
import io.dapr.client.DaprHttpBuilder;
import io.dapr.serializer.DaprObjectSerializer;

/**
 * Builder to generate an ActorProxy instance. Builder can be reused for multiple instances.
 */
public class ActorProxyBuilder {

  /**
   * Builder for Dapr's raw http client.
   */
  private final DaprHttpBuilder daprHttpBuilder = new DaprHttpBuilder();

  /**
   * Actor's type.
   */
  private final String actorType;

  /**
   * Dapr's object serializer.
   */
  private final DaprObjectSerializer objectSerializer;

  /**
   * Instantiates a new builder for a given Actor type.
   *
   * @param actorType        Actor's type.
   * @param objectSerializer Serializer for objects sent/received.
   */
  public ActorProxyBuilder(String actorType, DaprObjectSerializer objectSerializer) {
    if ((actorType == null) || actorType.isEmpty()) {
      throw new IllegalArgumentException("ActorType is required.");
    }
    if (objectSerializer == null) {
      throw new IllegalArgumentException("Serializer is required.");
    }

    this.actorType = actorType;
    this.objectSerializer = objectSerializer;
  }

  /**
   * Instantiates a new ActorProxy.
   *
   * @param actorId Actor's identifier.
   * @return New instance of ActorProxy.
   */
  public ActorProxy build(ActorId actorId) {
    if (actorId == null) {
      throw new IllegalArgumentException("Cannot instantiate an Actor without Id.");
    }

    return new ActorProxyImpl(
          this.actorType,
          actorId,
          this.objectSerializer,
          new DaprHttpClient(this.daprHttpBuilder.build()));
  }

}
