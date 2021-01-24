/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.client;

import io.dapr.actors.ActorId;
import io.dapr.actors.ActorUtils;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;

import java.lang.reflect.Proxy;

/**
 * Builder to generate an ActorProxy instance. Builder can be reused for multiple instances.
 */
public class ActorProxyBuilder<T> {

  /**
   * Actor's type.
   */
  private final String actorType;

  /**
   * Actor's type class.
   */
  private final Class<T> clazz;

  /**
   * Dapr's object serializer.
   */
  private DaprObjectSerializer objectSerializer;

  /**
   * Channel for communication with Dapr.
   */
  private final DaprChannel channel;

  /**
   * Instantiates a new builder for a given Actor type, using {@link DefaultObjectSerializer} by default.
   *
   * {@link DefaultObjectSerializer} is not recommended for production scenarios.
   *
   * @param actorTypeClass Actor's type class.
   * @param channel        Dapr's sidecar channel.
   */
  public ActorProxyBuilder(Class<T> actorTypeClass, DaprChannel channel) {
    this(ActorUtils.findActorTypeName(actorTypeClass), actorTypeClass, channel);
  }

  /**
   * Instantiates a new builder for a given Actor type, using {@link DefaultObjectSerializer} by default.
   *
   * {@link DefaultObjectSerializer} is not recommended for production scenarios.
   *
   * @param actorType      Actor's type.
   * @param actorTypeClass Actor's type class.
   * @param channel        Dapr's sidecar channel.
   */
  public ActorProxyBuilder(String actorType, Class<T> actorTypeClass, DaprChannel channel) {
    if ((actorType == null) || actorType.isEmpty()) {
      throw new IllegalArgumentException("ActorType is required.");
    }
    if (actorTypeClass == null) {
      throw new IllegalArgumentException("ActorTypeClass is required.");
    }
    if (channel == null) {
      throw new IllegalArgumentException("Channel is required.");
    }

    this.actorType = actorType;
    this.objectSerializer = new DefaultObjectSerializer();
    this.clazz = actorTypeClass;
    this.channel = channel;
  }

  /**
   * Instantiates a new builder for a given Actor type, using {@link DefaultObjectSerializer}.
   *
   * @param objectSerializer Serializer for objects sent/received.
   * @return This instance.
   */
  public ActorProxyBuilder<T> withObjectSerializer(DaprObjectSerializer objectSerializer) {
    if (objectSerializer == null) {
      throw new IllegalArgumentException("Serializer is required.");
    }

    this.objectSerializer = objectSerializer;
    return this;
  }

  /**
   * Instantiates a new ActorProxy.
   *
   * @param actorId Actor's identifier.
   * @return New instance of ActorProxy.
   */
  public T build(ActorId actorId) {
    if (actorId == null) {
      throw new IllegalArgumentException("Cannot instantiate an Actor without Id.");
    }

    ActorProxyImpl proxy = new ActorProxyImpl(
            this.actorType,
            actorId,
            this.objectSerializer,
            this.channel.getDaprClient());

    if (this.clazz.equals(ActorProxy.class)) {
      // If users want to use the not strongly typed API, we respect that here.
      return (T) proxy;
    }

    return (T) Proxy.newProxyInstance(
            ActorProxyImpl.class.getClassLoader(),
            new Class[]{this.clazz},
            proxy);
  }

}
