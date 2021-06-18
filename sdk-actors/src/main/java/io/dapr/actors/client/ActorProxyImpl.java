/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.client;

import io.dapr.actors.ActorId;
import io.dapr.actors.ActorMethod;
import io.dapr.actors.runtime.ActorRuntime;
import io.dapr.actors.runtime.ActorRuntimeContext;
import io.dapr.exceptions.DaprException;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.utils.TypeRef;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Optional;

/**
 * Implements a proxy client for an Actor's instance.
 */
class ActorProxyImpl implements ActorProxy, InvocationHandler {

  private static final String UNDEFINED_CLASS_NAME = "io.dapr.actors.Undefined";

  /**
   * Actor's identifier for this Actor instance.
   */
  private final ActorId actorId;

  /**
   * Actor's type for this Actor instance.
   */
  private final String actorType;

  /**
   * Serializer/deserialzier to exchange message for Actors.
   */
  private final DaprObjectSerializer serializer;

  /**
   * Client to talk to the Dapr's API.
   */
  private final ActorClient actorClient;

  private final ActorRuntime actorRuntime;

  /**
   * Creates a new instance of {@link ActorProxyImpl}.
   *
   * @param actorType  actor implementation type of the actor associated with the proxy object.
   * @param actorId    The actorId associated with the proxy
   * @param serializer Serializer and deserializer for method calls.
   * @param actorClient Dapr client for Actor APIs.
   */
  ActorProxyImpl(String actorType, ActorId actorId, DaprObjectSerializer serializer, ActorClient actorClient) {
    this(actorType, actorId, serializer, actorClient, ActorRuntime.getInstance());
  }

  /**
   * Creates a new instance of {@link ActorProxyImpl}.
   *
   * @param actorType  actor implementation type of the actor associated with the proxy object.
   * @param actorId    The actorId associated with the proxy
   * @param serializer Serializer and deserializer for method calls.
   * @param actorClient Dapr client for Actor APIs.
   */
  ActorProxyImpl(String actorType,
                 ActorId actorId,
                 DaprObjectSerializer serializer,
                 ActorClient actorClient,
                 ActorRuntime actorRuntime) {
    this.actorType = actorType;
    this.actorId = actorId;
    this.actorClient = actorClient;
    this.serializer = serializer;
    this.actorRuntime = actorRuntime;
  }

  /**
   * {@inheritDoc}
   */
  public ActorId getActorId() {
    return actorId;
  }

  /**
   * {@inheritDoc}
   */
  public String getActorType() {
    return actorType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeMethod(String methodName, Object data, TypeRef<T> type) {
    final Object reentrancyId = actorRuntime.getActorReentrancyId(actorId.toString(), actorType).block();

    if (reentrancyId != null) {
      final Optional rawOptional = (Optional) reentrancyId;
      if (rawOptional.isPresent() && rawOptional.get() instanceof String) {
        return this.actorClient.invoke(actorType, actorId.toString(), methodName, this.serialize(data),
            Collections.singletonMap("Dapr-Reentrancy-Id", (String) rawOptional.get()))
            .filter(s -> s.length > 0)
            .map(s -> deserialize(s, type));
      }
    }

    return this.actorClient.invoke(actorType, actorId.toString(), methodName, this.serialize(data))
          .filter(s -> s.length > 0)
          .map(s -> deserialize(s, type));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeMethod(String methodName, Object data, Class<T> clazz) {
    return this.invokeMethod(methodName, data, TypeRef.get(clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeMethod(String methodName, TypeRef<T> type) {
    return this.actorClient.invoke(actorType, actorId.toString(), methodName, null)
          .filter(s -> s.length > 0)
          .map(s -> deserialize(s, type));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeMethod(String methodName, Class<T> clazz) {
    return this.invokeMethod(methodName, TypeRef.get(clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeMethod(String methodName) {
    return this.actorClient.invoke(actorType, actorId.toString(), methodName, null).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeMethod(String methodName, Object data) {
    return this.actorClient.invoke(actorType, actorId.toString(), methodName, this.serialize(data)).then();
  }

  /**
   * Handles an invocation via reflection.
   *
   * @param proxy Interface or class being invoked.
   * @param method Method being invoked.
   * @param args Arguments to invoke method.
   * @return Response object for the invocation.
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) {
    if (method.getParameterCount() > 1) {
      throw new UnsupportedOperationException("Actor methods can only have zero or one arguments.");
    }

    ActorMethod actorMethodAnnotation = method.getDeclaredAnnotation(ActorMethod.class);
    String methodName = method.getName();
    if ((actorMethodAnnotation != null) && !actorMethodAnnotation.name().isEmpty()) {
      methodName = actorMethodAnnotation.name();
    }

    if (method.getParameterCount() == 0) {
      if (method.getReturnType().equals(Mono.class)) {
        if ((actorMethodAnnotation == null) || UNDEFINED_CLASS_NAME.equals(actorMethodAnnotation.returns().getName())) {
          return invokeMethod(methodName);
        }

        return invokeMethod(methodName, actorMethodAnnotation.returns());
      }

      return invokeMethod(methodName, method.getReturnType()).block();
    }

    if (method.getReturnType().equals(Mono.class)) {
      if ((actorMethodAnnotation == null) || UNDEFINED_CLASS_NAME.equals(actorMethodAnnotation.returns().getName())) {
        return invokeMethod(methodName, args[0]);
      }

      return invokeMethod(methodName, args[0], actorMethodAnnotation.returns());
    }

    return invokeMethod(methodName, args[0], method.getReturnType()).block();
  }

  /**
   * Extracts the response object from the Actor's method result.
   *
   * @param response response returned by API.
   * @param type     Expected response type.
   * @param <T>      Expected response type.
   * @return Response object or null.
   * @throws RuntimeException In case it cannot generate Object.
   */
  private <T> T deserialize(final byte[] response, TypeRef<T> type) {
    try {
      return this.serializer.deserialize(response, type);
    } catch (IOException e) {
      DaprException.wrap(e);
      return null;
    }
  }

  /**
   * Builds the request to invoke an API for Actors.
   *
   * @param request Request object for the original Actor's method.
   * @return Payload to be sent to Dapr's API.
   * @throws RuntimeException In case it cannot generate payload.
   */
  private byte[] serialize(final Object request) {
    try {
      return this.serializer.serialize(request);
    } catch (IOException e) {
      DaprException.wrap(e);
      return null;
    }
  }
}
