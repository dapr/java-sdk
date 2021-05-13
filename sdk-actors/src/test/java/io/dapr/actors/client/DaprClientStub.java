/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.client;

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
                             Map<String, String> headers) {
    return invoke(actorType, actorId, methodName, jsonPayload);
  }
}
