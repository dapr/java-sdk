/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.client;

import io.dapr.actors.ActorId;
import io.dapr.utils.TypeRef;
import reactor.core.publisher.Mono;

/**
 * Proxy to communicate to a given Actor instance in Dapr.
 */
public interface ActorProxy {

  /**
   * Returns the ActorId associated with the proxy object.
   *
   * @return An ActorId object.
   */
  ActorId getActorId();

  /**
   * Returns actor implementation type of the actor associated with the proxy object.
   *
   * @return Actor's type name.
   */
  String getActorType();

  /**
   * Invokes an Actor method on Dapr.
   *
   * @param methodName Method name to invoke.
   * @param type       The type of the return class.
   * @param <T>        The type to be returned.
   * @return Asynchronous result with the Actor's response.
   */
  <T> Mono<T> invokeMethod(String methodName, TypeRef<T> type);

  /**
   * Invokes an Actor method on Dapr.
   *
   * @param methodName Method name to invoke.
   * @param clazz      The type of the return class.
   * @param <T>        The type to be returned.
   * @return Asynchronous result with the Actor's response.
   */
  <T> Mono<T> invokeMethod(String methodName, Class<T> clazz);

  /**
   * Invokes an Actor method on Dapr.
   *
   * @param methodName Method name to invoke.
   * @param data       Object with the data.
   * @param type       The type of the return class.
   * @param <T>        The type to be returned.
   * @return Asynchronous result with the Actor's response.
   */
  <T> Mono<T> invokeMethod(String methodName, Object data, TypeRef<T> type);

  /**
   * Invokes an Actor method on Dapr.
   *
   * @param methodName Method name to invoke.
   * @param data       Object with the data.
   * @param clazz      The type of the return class.
   * @param <T>        The type to be returned.
   * @return Asynchronous result with the Actor's response.
   */
  <T> Mono<T> invokeMethod(String methodName, Object data, Class<T> clazz);

  /**
   * Invokes an Actor method on Dapr.
   *
   * @param methodName Method name to invoke.
   * @return Asynchronous result with the Actor's response.
   */
  Mono<Void> invokeMethod(String methodName);

  /**
   * Invokes an Actor method on Dapr.
   *
   * @param methodName Method name to invoke.
   * @param data       Object with the data.
   * @return Asynchronous result with the Actor's response.
   */
  Mono<Void> invokeMethod(String methodName, Object data);

}
