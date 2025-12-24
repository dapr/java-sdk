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

import io.dapr.testcontainers.wait.strategy.metadata.Metadata;

import java.util.function.Predicate;

/**
 * Factory class for creating Dapr-specific wait strategies.
 *
 * <p>This class provides static factory methods to create wait strategies
 * that poll the Dapr metadata endpoint to determine when specific conditions are met.
 * This is more reliable than log-based waiting strategies.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Wait for a subscription to be registered
 * DaprWait.forSubscription("pubsub", "my-topic")
 *     .withStartupTimeout(Duration.ofSeconds(30))
 *     .waitUntilReady(daprContainer);
 *
 * // Wait for any actors to be registered
 * DaprWait.forActors()
 *     .waitUntilReady(daprContainer);
 *
 * // Wait for a specific actor type
 * DaprWait.forActorType("MyActor")
 *     .waitUntilReady(daprContainer);
 * }</pre>
 *
 * @see <a href="https://docs.dapr.io/reference/api/metadata_api/">Dapr Metadata API</a>
 */
public final class DaprWait {

  private DaprWait() {
    // Utility class, no instantiation
  }

  /**
   * Creates a wait strategy that waits for a subscription to be registered.
   *
   * @param pubsubName the name of the pub/sub component (can be null to match any)
   * @param topic the topic name to wait for (can be null to match any)
   * @return a new subscription wait strategy
   */
  public static SubscriptionWaitStrategy forSubscription(String pubsubName, String topic) {
    return new SubscriptionWaitStrategy(pubsubName, topic);
  }

  /**
   * Creates a wait strategy that waits for any actors to be registered.
   *
   * @return a new actor wait strategy
   */
  public static ActorWaitStrategy forActors() {
    return new ActorWaitStrategy();
  }

  /**
   * Creates a wait strategy that waits for a specific actor type to be registered.
   *
   * @param actorType the actor type to wait for
   * @return a new actor wait strategy
   */
  public static ActorWaitStrategy forActorType(String actorType) {
    return new ActorWaitStrategy(actorType);
  }

  /**
   * Creates a wait strategy with a custom condition based on Dapr metadata.
   *
   * <p>Example:</p>
   * <pre>{@code
   * DaprWait.forCondition(
   *     metadata -> metadata.getComponents().size() >= 2,
   *     "at least 2 components to be loaded"
   * );
   * }</pre>
   *
   * @param predicate the condition to check against the metadata
   * @param description a human-readable description of the condition
   * @return a new custom wait strategy
   */
  public static DaprWaitStrategy forCondition(Predicate<Metadata> predicate, String description) {
    return DaprWaitStrategy.forCondition(predicate, description);
  }
}
