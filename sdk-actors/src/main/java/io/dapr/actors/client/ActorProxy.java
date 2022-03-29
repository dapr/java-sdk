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
