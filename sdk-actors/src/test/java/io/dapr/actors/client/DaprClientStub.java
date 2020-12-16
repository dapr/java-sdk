/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.client;

import reactor.core.publisher.Mono;

public class DaprClientStub implements DaprClient {

  @Override
  public Mono<byte[]> invoke(String actorType, String actorId, String methodName, byte[] jsonPayload) {
    return Mono.just(new byte[0]);
  }

}
