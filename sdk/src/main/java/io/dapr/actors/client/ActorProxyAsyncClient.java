/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.dapr.actors.ActorId;
import reactor.core.publisher.Mono;

/**
 * Interface to invoke actor methods.
 */
interface ActorProxyAsyncClient {

  /**
   * Returns the ActorId associated with the proxy object.
   *
   * @return An ActorId object.
   */
  ActorId getActorId();



  /**
   * Returns actor implementation type of the actor associated with the proxy object.
   *
   * @return An String object.
   */
  String getActorType();


  /**
   * Invokes an Actor method on Dapr.
   *
   * @param methodName Method name to invoke.
   * @param clazz The type of the return class.
   * @return Asynchronous result with the Actor's response.
   */
  <T> Mono<T> invokeActorMethod(String methodName,  Class<T> clazz);

  /**
   * Invokes an Actor method on Dapr.
   *
   * @param methodName Method name to invoke.
   * @param data Object with the data.
   * @param clazz The type of the return class.
   * @return Asynchronous result with the Actor's response.
   */
  <T> Mono<T> invokeActorMethod(String methodName, Object data,  Class<T> clazz) throws JsonProcessingException;

  /**
   * Invokes an Actor method on Dapr.
   *
   * @param methodName Method name to invoke.
   * @return Asynchronous result with the Actor's response.
   */
  public Mono invokeActorMethod(String methodName) ;

  /**
   * Invokes an Actor method on Dapr.
   *
   * @param methodName Method name to invoke.
   * @param data Object with the data.
   * @return Asynchronous result with the Actor's response.
   */
  public Mono invokeActorMethod(String methodName, Object data) throws JsonProcessingException;



}
