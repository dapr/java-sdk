/*
 * Copyright 2026 The Dapr Authors
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

package io.dapr.it.actors;

import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorClient;
import io.dapr.actors.client.ActorProxy;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.client.resiliency.ResiliencyOptions;
import io.dapr.exceptions.DaprException;
import io.dapr.it.containers.BaseContainerIT;
import io.dapr.testcontainers.DaprContainer;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static io.dapr.it.Retry.callWithRetry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * When a caller cancels an actor call while it runs, daprd releases the actor's turn and sends the next call, but
 * the app keeps running the cancelled one. The SDK must hold the turn until the actor code finishes.
 */
abstract class AbstractActorCancelledCallIT extends BaseContainerIT {

  private static final int CALLS = 5;

  protected static DaprContainer dapr;

  @Test
  public void cancelledCallsDoNotOverlap() throws Exception {
    ActorId actorId = new ActorId(UUID.randomUUID().toString());
    // The timeout becomes a gRPC deadline, so daprd cancels each call 500ms in while holdTurn runs for 2s.
    ActorClient cancellingClient = newActorClient(dapr, new ResiliencyOptions().setTimeout(Duration.ofMillis(500)));
    ActorProxy cancelling = newProxy(cancellingClient, actorId);
    ActorProxy proxy = newProxy(newActorClient(dapr), actorId);

    // Activate the actor first so every holdTurn call reaches the app quickly.
    proxy.invokeMethod("getCompletedTurns", Integer.class).block();

    // Each call starts after daprd released the previous one, so without a turn lock in the app they overlap.
    for (int i = 0; i < CALLS; i++) {
      // Every call must be cancelled, otherwise the calls run one after another and the test proves nothing.
      DaprException e = assertThrows(DaprException.class, () -> cancelling.invokeMethod("holdTurn").block());
      assertEquals("DEADLINE_EXCEEDED", e.getErrorCode());
      Thread.sleep(100);
    }

    callWithRetry(() -> assertEquals(CALLS, proxy.invokeMethod("getCompletedTurns", Integer.class).block()), 30000);
    assertEquals(1, proxy.invokeMethod("getMaxConcurrentTurns", Integer.class).block());
  }

  private static ActorProxy newProxy(ActorClient client, ActorId actorId) {
    return new ActorProxyBuilder<>("MyActorTest", ActorProxy.class, client).build(actorId);
  }
}
