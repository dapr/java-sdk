/*
 * Copyright 2021 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
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
   * Client for communication with Dapr's Actor APIs.
   */
  private final ActorClient actorClient;

  /**
   * Instantiates a new builder for a given Actor type, using {@link DefaultObjectSerializer} by default.
   *
   * {@link DefaultObjectSerializer} is not recommended for production scenarios.
   *
   * @param actorTypeClass Actor's type class.
   * @param actorClient    Dapr's sidecar client for Actor APIs.
   */
  public ActorProxyBuilder(Class<T> actorTypeClass, ActorClient actorClient) {
    this(ActorUtils.findActorTypeName(actorTypeClass), actorTypeClass, actorClient);
  }

  /**
   * Instantiates a new builder for a given Actor type, using {@link DefaultObjectSerializer} by default.
   *
   * {@link DefaultObjectSerializer} is not recommended for production scenarios.
   *
   * @param actorType      Actor's type.
   * @param actorTypeClass Actor's type class.
   * @param actorClient    Dapr's sidecar client for Actor APIs.
   */
  public ActorProxyBuilder(String actorType, Class<T> actorTypeClass, ActorClient actorClient) {
    if ((actorType == null) || actorType.isEmpty()) {
      throw new IllegalArgumentException("ActorType is required.");
    }
    if (actorTypeClass == null) {
      throw new IllegalArgumentException("ActorTypeClass is required.");
    }
    if (actorClient == null) {
      throw new IllegalArgumentException("ActorClient is required.");
    }

    this.actorType = actorType;
    this.objectSerializer = new DefaultObjectSerializer();
    this.clazz = actorTypeClass;
    this.actorClient = actorClient;
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
            this.actorClient);

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
