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

import io.dapr.testcontainers.wait.strategy.metadata.Component;
import io.dapr.testcontainers.wait.strategy.metadata.Metadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DaprWaitTest {

  @Test
  @DisplayName("forSubscription should create SubscriptionWaitStrategy")
  void forSubscriptionShouldCreateSubscriptionWaitStrategy() {
    DaprWaitStrategy strategy = DaprWait.forSubscription("pubsub", "orders");

    assertInstanceOf(SubscriptionWaitStrategy.class, strategy);
  }

  @Test
  @DisplayName("forSubscription with null topic should match any topic")
  void forSubscriptionWithNullTopicShouldMatchAnyTopic() {
    SubscriptionWaitStrategy strategy = DaprWait.forSubscription("pubsub", null);

    assertNotNull(strategy);
    assertEquals("subscription for pubsub 'pubsub'", strategy.getConditionDescription());
  }

  @Test
  @DisplayName("forSubscription with null pubsub should match any pubsub")
  void forSubscriptionWithNullPubsubShouldMatchAnyPubsub() {
    SubscriptionWaitStrategy strategy = DaprWait.forSubscription(null, "orders");

    assertNotNull(strategy);
    assertEquals("subscription for topic 'orders'", strategy.getConditionDescription());
  }

  @Test
  @DisplayName("forActors should create ActorWaitStrategy for any actor")
  void forActorsShouldCreateActorWaitStrategyForAnyActor() {
    ActorWaitStrategy strategy = DaprWait.forActors();

    assertNotNull(strategy);
    assertEquals("any registered actors", strategy.getConditionDescription());
  }

  @Test
  @DisplayName("forActorType should create ActorWaitStrategy for specific type")
  void forActorTypeShouldCreateActorWaitStrategyForSpecificType() {
    ActorWaitStrategy strategy = DaprWait.forActorType("MyActor");

    assertNotNull(strategy);
    assertEquals("actor type 'MyActor'", strategy.getConditionDescription());
  }

  @Test
  @DisplayName("forCondition should create custom wait strategy with predicate")
  void forConditionShouldCreateCustomWaitStrategy() {
    DaprWaitStrategy strategy = DaprWait.forCondition(
        metadata -> metadata.getComponents().size() >= 2,
        "at least 2 components"
    );

    assertNotNull(strategy);
    assertEquals("at least 2 components", strategy.getConditionDescription());

    Metadata metadataWith2Components = new Metadata();
    Component comp1 = new Component();
    comp1.setName("comp1");
    Component comp2 = new Component();
    comp2.setName("comp2");
    metadataWith2Components.setComponents(Arrays.asList(comp1, comp2));

    Metadata metadataWith1Component = new Metadata();
    metadataWith1Component.setComponents(Arrays.asList(comp1));

    assertTrue(strategy.isConditionMet(metadataWith2Components));
    assertFalse(strategy.isConditionMet(metadataWith1Component));
  }

  @Test
  @DisplayName("Strategy should support fluent configuration with poll interval and timeout")
  void strategyShouldSupportFluentConfiguration() {
    DaprWaitStrategy strategy = DaprWait.forSubscription("pubsub", "orders")
        .withPollInterval(Duration.ofMillis(250));
    strategy.withStartupTimeout(Duration.ofSeconds(60));

    assertNotNull(strategy);
  }
}
