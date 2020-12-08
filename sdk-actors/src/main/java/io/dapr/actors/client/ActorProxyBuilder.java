/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.client;

import io.dapr.actors.ActorId;
import io.dapr.actors.ActorUtils;
import io.dapr.client.DaprHttpBuilder;
import io.dapr.config.Properties;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.v1.DaprGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.Closeable;
import java.lang.reflect.Proxy;

import static io.dapr.exceptions.DaprException.throwIllegalArgumentException;

/**
 * Builder to generate an ActorProxy instance. Builder can be reused for multiple instances.
 */
public class ActorProxyBuilder<T> implements Closeable {

  /**
   * Determine if this builder will create GRPC clients instead of HTTP clients.
   */
  private final boolean useGrpc;

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
    if ((actorType == null) || actorType.isEmpty()) {
      throwIllegalArgumentException("ActorType is required.");
    }
    if (actorTypeClass == null) {
      throwIllegalArgumentException("ActorTypeClass is required.");
    }

    this.useGrpc = Properties.USE_GRPC.get();
    this.actorType = actorType;
    this.objectSerializer = new DefaultObjectSerializer();
    this.clazz = actorTypeClass;
    this.daprHttpBuilder = new DaprHttpBuilder();
    this.channel = buildManagedChannel();
  }

  /**
   * Instantiates a new builder for a given Actor type, using {@link DefaultObjectSerializer}.
   *
   * @param objectSerializer Serializer for objects sent/received.
   * @return This instance.
   */
  public ActorProxyBuilder<T> withObjectSerializer(DaprObjectSerializer objectSerializer) {
    if (objectSerializer == null) {
      throwIllegalArgumentException("Serializer is required.");
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
      throwIllegalArgumentException("Cannot instantiate an Actor without Id.");
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
    if (this.useGrpc) {
      return new DaprGrpcClient(DaprGrpc.newFutureStub(this.channel));
    }

    return new DaprHttpClient(daprHttpBuilder.build());
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
   * @return GRPC managed channel or null.
   */
  private static ManagedChannel buildManagedChannel() {
    if (!Properties.USE_GRPC.get()) {
      return null;
    }

    int port = Properties.GRPC_PORT.get();
    if (port <= 0) {
      throwIllegalArgumentException("Invalid port.");
    }

    return ManagedChannelBuilder.forAddress(Properties.SIDECAR_IP.get(), port).usePlaintext().build();
  }
}
