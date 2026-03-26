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
 * limitations under the License.
 */

package io.dapr.it.actors;

import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.wait.strategy.DaprWait;

import java.time.Duration;

/**
 * Shared bootstrap helpers for actor integration tests.
 */
public final class ActorTestBootstrap {

  private static final Duration DEFAULT_ACTOR_WAIT_TIMEOUT = Duration.ofMinutes(2);

  private ActorTestBootstrap() {
  }

  /**
   * Expose host app port and wait for the given actor type to be registered in Dapr metadata.
   *
   * @param daprContainer sidecar container
   * @param actorType actor type name to wait for
   */
  public static void exposeHostPortAndWaitForActorType(DaprContainer daprContainer, String actorType) {
    org.testcontainers.Testcontainers.exposeHostPorts(daprContainer.getAppPort());
    DaprWait.forActorType(actorType)
        .withStartupTimeout(DEFAULT_ACTOR_WAIT_TIMEOUT)
        .waitUntilReady(daprContainer);
  }

  /**
   * Expose host app port and wait until at least one actor is registered.
   *
   * @param daprContainer sidecar container
   */
  public static void exposeHostPortAndWaitForAnyActor(DaprContainer daprContainer) {
    org.testcontainers.Testcontainers.exposeHostPorts(daprContainer.getAppPort());
    DaprWait.forActors()
        .withStartupTimeout(DEFAULT_ACTOR_WAIT_TIMEOUT)
        .waitUntilReady(daprContainer);
  }
}
