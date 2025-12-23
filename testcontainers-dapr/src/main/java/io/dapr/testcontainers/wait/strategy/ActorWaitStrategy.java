/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.testcontainers.wait.strategy;

import io.dapr.testcontainers.wait.strategy.metadata.Actor;
import io.dapr.testcontainers.wait.strategy.metadata.Metadata;

/**
 * Wait strategy that waits for actors to be registered with Dapr.
 */
public class ActorWaitStrategy extends DaprWaitStrategy {

  private final String actorType;

  /**
   * Creates a wait strategy that waits for any actor to be registered.
   */
  public ActorWaitStrategy() {
    this.actorType = null;
  }

  /**
   * Creates a wait strategy that waits for a specific actor type to be registered.
   *
   * @param actorType the actor type to wait for
   */
  public ActorWaitStrategy(String actorType) {
    this.actorType = actorType;
  }

  @Override
  protected boolean isConditionMet(Metadata metadata) {
    if (actorType == null) {
      return !metadata.getActors().isEmpty();
    }
    return metadata.getActors().stream()
        .anyMatch(this::matchesActorType);
  }

  private boolean matchesActorType(Actor actor) {
    return actorType.equals(actor.getType());
  }

  @Override
  protected String getConditionDescription() {
    if (actorType != null) {
      return String.format("actor type '%s'", actorType);
    }
    return "any registered actors";
  }
}
