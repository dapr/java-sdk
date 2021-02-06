/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.client;

import reactor.core.publisher.Mono;

/**
 * Generic Client Adapter to be used regardless of the GRPC or the HTTP Client implementation required.
 */
interface DaprClient {

  /**
   * Invokes an Actor method on Dapr.
   *
   * @param actorType   Type of actor.
   * @param actorId     Actor Identifier.
   * @param methodName  Method name to invoke.
   * @param jsonPayload Serialized body.
   * @return Asynchronous result with the Actor's response.
   */
  Mono<byte[]> invoke(String actorType, String actorId, String methodName, byte[] jsonPayload);

}
