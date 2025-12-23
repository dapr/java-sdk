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
import io.dapr.testcontainers.wait.strategy.metadata.Subscription;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SubscriptionWaitStrategyTest {

  @Test
  void shouldMatchExactSubscription() {
    SubscriptionWaitStrategy strategy = new SubscriptionWaitStrategy("pubsub", "orders");

    Metadata metadata = createMetadataWithSubscription("pubsub", "orders");

    assertTrue(strategy.isConditionMet(metadata));
  }

  @Test
  void shouldNotMatchWhenPubsubDiffers() {
    SubscriptionWaitStrategy strategy = new SubscriptionWaitStrategy("pubsub", "orders");

    Metadata metadata = createMetadataWithSubscription("other-pubsub", "orders");

    assertFalse(strategy.isConditionMet(metadata));
  }

  @Test
  void shouldNotMatchWhenTopicDiffers() {
    SubscriptionWaitStrategy strategy = new SubscriptionWaitStrategy("pubsub", "orders");

    Metadata metadata = createMetadataWithSubscription("pubsub", "other-topic");

    assertFalse(strategy.isConditionMet(metadata));
  }

  @Test
  void shouldNotMatchWhenNoSubscriptions() {
    SubscriptionWaitStrategy strategy = new SubscriptionWaitStrategy("pubsub", "orders");

    Metadata metadata = new Metadata();
    metadata.setSubscriptions(Collections.emptyList());

    assertFalse(strategy.isConditionMet(metadata));
  }

  @Test
  void shouldMatchAnyTopicWhenTopicIsNull() {
    SubscriptionWaitStrategy strategy = new SubscriptionWaitStrategy("pubsub", null);

    Metadata metadata = createMetadataWithSubscription("pubsub", "any-topic");

    assertTrue(strategy.isConditionMet(metadata));
  }

  @Test
  void shouldMatchAnyPubsubWhenPubsubIsNull() {
    SubscriptionWaitStrategy strategy = new SubscriptionWaitStrategy(null, "orders");

    Metadata metadata = createMetadataWithSubscription("any-pubsub", "orders");

    assertTrue(strategy.isConditionMet(metadata));
  }

  @Test
  void shouldMatchAnySubscriptionWhenBothAreNull() {
    SubscriptionWaitStrategy strategy = new SubscriptionWaitStrategy(null, null);

    Metadata metadata = createMetadataWithSubscription("any-pubsub", "any-topic");

    assertTrue(strategy.isConditionMet(metadata));
  }

  @Test
  void shouldFindMatchAmongMultipleSubscriptions() {
    SubscriptionWaitStrategy strategy = new SubscriptionWaitStrategy("pubsub", "orders");

    Subscription sub1 = createSubscription("other-pubsub", "other-topic");
    Subscription sub2 = createSubscription("pubsub", "orders");
    Subscription sub3 = createSubscription("another-pubsub", "another-topic");

    Metadata metadata = new Metadata();
    metadata.setSubscriptions(Arrays.asList(sub1, sub2, sub3));

    assertTrue(strategy.isConditionMet(metadata));
  }

  @Test
  void shouldProvideCorrectDescription() {
    SubscriptionWaitStrategy strategy = new SubscriptionWaitStrategy("pubsub", "orders");
    assertEquals("subscription for pubsub 'pubsub' and topic 'orders'", strategy.getConditionDescription());

    SubscriptionWaitStrategy pubsubOnly = new SubscriptionWaitStrategy("pubsub", null);
    assertEquals("subscription for pubsub 'pubsub'", pubsubOnly.getConditionDescription());

    SubscriptionWaitStrategy topicOnly = new SubscriptionWaitStrategy(null, "orders");
    assertEquals("subscription for topic 'orders'", topicOnly.getConditionDescription());

    SubscriptionWaitStrategy any = new SubscriptionWaitStrategy(null, null);
    assertEquals("any subscription", any.getConditionDescription());
  }

  private Metadata createMetadataWithSubscription(String pubsubName, String topic) {
    Metadata metadata = new Metadata();
    metadata.setSubscriptions(Collections.singletonList(createSubscription(pubsubName, topic)));
    return metadata;
  }

  private Subscription createSubscription(String pubsubName, String topic) {
    Subscription subscription = new Subscription();
    subscription.setPubsubname(pubsubName);
    subscription.setTopic(topic);
    return subscription;
  }
}
