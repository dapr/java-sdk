package io.dapr.actors.client;

import io.dapr.actors.ActorId;
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
   * @param clazz      The type of the return class.
   * @param <T>        The type to be returned.
   * @return Asynchronous result with the Actor's response.
   */
  <T> Mono<T> invokeActorMethod(String methodName, Class<T> clazz);

  /**
   * Invokes an Actor method on Dapr.
   *
   * @param methodName Method name to invoke.
   * @param data       Object with the data.
   * @param clazz      The type of the return class.
   * @param <T>        The type to be returned.
   * @return Asynchronous result with the Actor's response.
   */
  <T> Mono<T> invokeActorMethod(String methodName, Object data, Class<T> clazz);

  /**
   * Invokes an Actor method on Dapr.
   *
   * @param methodName Method name to invoke.
   * @return Asynchronous result with the Actor's response.
   */
  Mono<Void> invokeActorMethod(String methodName);

  /**
   * Invokes an Actor method on Dapr.
   *
   * @param methodName Method name to invoke.
   * @param data       Object with the data.
   * @return Asynchronous result with the Actor's response.
   */
  Mono<Void> invokeActorMethod(String methodName, Object data);

}
