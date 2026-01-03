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

/**
 * Wait strategy that waits for a specific subscription to be registered with Dapr.
 */
public class SubscriptionWaitStrategy extends AbstractDaprWaitStrategy {

  private final String pubsubName;
  private final String topic;

  /**
   * Creates a wait strategy for a specific subscription.
   *
   * @param pubsubName the name of the pub/sub component
   * @param topic the topic name to wait for
   */
  public SubscriptionWaitStrategy(String pubsubName, String topic) {
    this.pubsubName = pubsubName;
    this.topic = topic;
  }

  @Override
  protected boolean isConditionMet(Metadata metadata) {
    if (metadata == null) {
      return false;
    }
    return metadata.getSubscriptions().stream()
        .anyMatch(this::matchesSubscription);
  }

  private boolean matchesSubscription(Subscription subscription) {
    if (subscription == null) {
      return false;
    }
    boolean pubsubMatches = pubsubName == null || pubsubName.equals(subscription.getPubsubname());
    boolean topicMatches = topic == null || topic.equals(subscription.getTopic());
    return pubsubMatches && topicMatches;
  }

  @Override
  protected String getConditionDescription() {
    if (pubsubName != null && topic != null) {
      return String.format("subscription for pubsub '%s' and topic '%s'", pubsubName, topic);
    } else if (pubsubName != null) {
      return String.format("subscription for pubsub '%s'", pubsubName);
    } else if (topic != null) {
      return String.format("subscription for topic '%s'", topic);
    } else {
      return "any subscription";
    }
  }
}
