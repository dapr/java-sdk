/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */
package io.dapr.actors.client;

import reactor.core.publisher.Mono;

/**
 * Stub implementation of ActorClient to enable unit testing in apps.
 * <p>This class is needed because ActorClient is a class and not an interface.</p>
 */
public class ActorClientStub extends ActorClient {

  /**
   * Creates a new stub instance of ActorClient without external communication.
   */
  public ActorClientStub() {
    super(null, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<byte[]> invoke(String actorType, String actorId, String methodName, byte[] jsonPayload) {
    return Mono.empty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    super.close();
  }
}
