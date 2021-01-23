/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.client;

import io.dapr.actors.ActorId;
import io.dapr.actors.ActorUtils;
import io.dapr.client.DaprApiProtocol;
import io.dapr.client.DaprHttpBuilder;
import io.dapr.config.Properties;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.v1.DaprGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.Closeable;
import java.lang.reflect.Proxy;

/**
 * Builder to generate an ActorProxy instance. Builder can be reused for multiple instances.
 */
public class ActorProxyBuilder<T> implements Closeable {

  /**
   * Determine if this builder will create GRPC clients instead of HTTP clients.
   */
  private final DaprApiProtocol apiProtocol;

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
   * Builds Dapr HTTP client.
   */
  private DaprHttpBuilder daprHttpBuilder;

  /**
   * Channel for communication with Dapr.
   */
  private final ManagedChannel channel;

  /**
   * Instantiates a new builder for a given Actor type, using {@link DefaultObjectSerializer} by default.
   *
   * {@link DefaultObjectSerializer} is not recommended for production scenarios.
   *
   * @param actorTypeClass Actor's type class.
   */
  public ActorProxyBuilder(Class<T> actorTypeClass) {
    this(ActorUtils.findActorTypeName(actorTypeClass), actorTypeClass);
  }

  /**
   * Instantiates a new builder for a given Actor type, using {@link DefaultObjectSerializer} by default.
   *
   * {@link DefaultObjectSerializer} is not recommended for production scenarios.
   *
   * @param actorType      Actor's type.
   * @param actorTypeClass Actor's type class.
   */
  public ActorProxyBuilder(String actorType, Class<T> actorTypeClass) {
    this(actorType, actorTypeClass, Properties.API_PROTOCOL.get());
  }

  /**
   * Instantiates a new builder for a given Actor type, using {@link DefaultObjectSerializer} by default.
   *
   * {@link DefaultObjectSerializer} is not recommended for production scenarios.
   *
   * @param actorType      Actor's type.
   * @param actorTypeClass Actor's type class.
   * @param apiProtocol    Dapr's API protocol.
   */
  private ActorProxyBuilder(String actorType, Class<T> actorTypeClass, DaprApiProtocol apiProtocol) {
    if ((actorType == null) || actorType.isEmpty()) {
      throw new IllegalArgumentException("ActorType is required.");
    }
    if (actorTypeClass == null) {
      throw new IllegalArgumentException("ActorTypeClass is required.");
    }

    this.apiProtocol = apiProtocol;
    this.actorType = actorType;
    this.objectSerializer = new DefaultObjectSerializer();
    this.clazz = actorTypeClass;
    this.daprHttpBuilder = new DaprHttpBuilder();
    this.channel = buildManagedChannel(apiProtocol);
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
            buildDaprClient());

    if (this.clazz.equals(ActorProxy.class)) {
      // If users want to use the not strongly typed API, we respect that here.
      return (T) proxy;
    }

    return (T) Proxy.newProxyInstance(
            ActorProxyImpl.class.getClassLoader(),
            new Class[]{this.clazz},
            proxy);
  }

  /**
   * Build an instance of the Client based on the provided setup.
   *
   * @return an instance of the setup Client
   * @throws java.lang.IllegalStateException if any required field is missing
   */
  private DaprClient buildDaprClient() {
    switch (this.apiProtocol) {
      case GRPC: return new DaprGrpcClient(DaprGrpc.newFutureStub(this.channel));
      case HTTP: return new DaprHttpClient(daprHttpBuilder.build());
      default: throw new IllegalStateException("Unsupported protocol: " + this.apiProtocol.name());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    if (channel != null && !channel.isShutdown()) {
      channel.shutdown();
    }
  }

  /**
   * Creates a GRPC managed channel (or null, if not applicable).
   *
   * @param apiProtocol Dapr's API protocol.
   * @return GRPC managed channel or null.
   */
  private static ManagedChannel buildManagedChannel(DaprApiProtocol apiProtocol) {
    if (apiProtocol != DaprApiProtocol.GRPC) {
      return null;
    }

    int port = Properties.GRPC_PORT.get();
    if (port <= 0) {
      throw new IllegalArgumentException("Invalid port.");
    }

    return ManagedChannelBuilder.forAddress(Properties.SIDECAR_IP.get(), port).usePlaintext().build();
  }
}
