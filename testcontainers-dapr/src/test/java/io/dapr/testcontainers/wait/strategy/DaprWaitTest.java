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
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DaprWaitTest {

  @Test
  void forSubscriptionShouldCreateSubscriptionWaitStrategy() {
    DaprWaitStrategy strategy = DaprWait.forSubscription("pubsub", "orders");

    assertInstanceOf(SubscriptionWaitStrategy.class, strategy);
  }

  @Test
  void forPubSubShouldCreateSubscriptionWaitStrategyWithNullTopic() {
    SubscriptionWaitStrategy strategy = DaprWait.forPubSub("pubsub");

    assertNotNull(strategy);
    assertEquals("subscription for pubsub 'pubsub'", strategy.getConditionDescription());
  }

  @Test
  void forTopicShouldCreateSubscriptionWaitStrategyWithNullPubsub() {
    SubscriptionWaitStrategy strategy = DaprWait.forTopic("orders");

    assertNotNull(strategy);
    assertEquals("subscription for topic 'orders'", strategy.getConditionDescription());
  }

  @Test
  void forActorsShouldCreateActorWaitStrategyForAnyActor() {
    ActorWaitStrategy strategy = DaprWait.forActors();

    assertNotNull(strategy);
    assertEquals("any registered actors", strategy.getConditionDescription());
  }

  @Test
  void forActorTypeShouldCreateActorWaitStrategyForSpecificType() {
    ActorWaitStrategy strategy = DaprWait.forActorType("MyActor");

    assertNotNull(strategy);
    assertEquals("actor type 'MyActor'", strategy.getConditionDescription());
  }

  @Test
  void forConditionShouldCreateCustomWaitStrategy() {
    DaprWaitStrategy strategy = DaprWait.forCondition(
        metadata -> metadata.getComponents().size() >= 2,
        "at least 2 components"
    );

    assertNotNull(strategy);
    assertEquals("at least 2 components", strategy.getConditionDescription());

    // Test with metadata that has 2 components
    Metadata metadataWith2Components = new Metadata();
    Component comp1 = new Component();
    comp1.setName("comp1");
    Component comp2 = new Component();
    comp2.setName("comp2");
    metadataWith2Components.setComponents(Arrays.asList(comp1, comp2));

    assertTrue(strategy.isConditionMet(metadataWith2Components));

    // Test with metadata that has 1 component
    Metadata metadataWith1Component = new Metadata();
    metadataWith1Component.setComponents(Arrays.asList(comp1));

    assertFalse(strategy.isConditionMet(metadataWith1Component));
  }

  @Test
  void strategyShouldSupportFluentConfiguration() {
    // Note: withPollInterval must be called before withStartupTimeout
    // because withStartupTimeout returns WaitStrategy (parent type)
    DaprWaitStrategy strategy = DaprWait.forSubscription("pubsub", "orders")
        .withPollInterval(Duration.ofMillis(250));
    strategy.withStartupTimeout(Duration.ofSeconds(60));

    assertNotNull(strategy);
  }
}
