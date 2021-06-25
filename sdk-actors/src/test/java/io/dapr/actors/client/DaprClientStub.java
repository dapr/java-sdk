/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.client;

import io.dapr.actors.runtime.ActorInvocationContext;
import reactor.core.publisher.Mono;

import java.util.Map;

public class DaprClientStub extends ActorClient implements DaprClient {

  @Override
  public Mono<byte[]> invoke(String actorType, String actorId, String methodName, byte[] jsonPayload) {
    return Mono.just(new byte[0]);
  }

  @Override
  public Mono<byte[]> invoke(String actorType,
                             String actorId,
                             String methodName,
                             byte[] jsonPayload,
                             ActorInvocationContext context) {
    return invoke(actorType, actorId, methodName, jsonPayload);
  }
}
