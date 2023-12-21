/*
 * Copyright 2022 The Dapr Authors
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

package io.dapr.springboot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class DaprSubscriptionBuilder {

  private final String pubsubName;
  private final String topic;
  private final List<TopicRule> rules;
  private String deadLetterTopic;
  private String defaultPath;
  private Map<String, String> metadata;

  private DaprTopicBulkSubscribe bulkSubscribe;

  /**
   * Create a subscription topic.
   *
   * @param pubsubName The pubsub name to subscribe to.
   * @param topic      The topic to subscribe to.
   */
  DaprSubscriptionBuilder(String pubsubName, String topic) {
    this.pubsubName = pubsubName;
    this.topic = topic;
    this.rules = new ArrayList<>();
    this.deadLetterTopic = null;
    this.defaultPath = null;
    this.metadata = Collections.emptyMap();
  }

  /**
   * Sets the default path for the subscription.
   *
   * @param path The default path.
   * @return this instance.
   */
  DaprSubscriptionBuilder setDefaultPath(String path) {
    if (defaultPath != null) {
      if (!defaultPath.equals(path)) {
        throw new RuntimeException(
            String.format(
                "a default route is already set for topic %s on pubsub %s (current: '%s', supplied: '%s')",
                this.topic, this.pubsubName, this.defaultPath, path));
      }
    }
    defaultPath = path;
    return this;
  }

  /**
   * Sets the dead letter topic for the subscription.
   *
   * @param deadLetterTopic Name of dead letter topic.
   * @return this instance.
   */
  DaprSubscriptionBuilder setDeadLetterTopic(String deadLetterTopic) {
    if (this.deadLetterTopic != null) {
      if (!this.deadLetterTopic.equals(deadLetterTopic)) {
        throw new RuntimeException(
            String.format(
                "a default dead letter topic is already set for topic %s on pubsub %s (current: '%s', supplied: '%s')",
                this.topic, this.pubsubName, this.deadLetterTopic, deadLetterTopic));
      }
    }
    this.deadLetterTopic = deadLetterTopic;
    return this;
  }

  /**
   * Adds a rule to the subscription.
   *
   * @param path     The path to route to.
   * @param match    The CEL expression the event must match.
   * @param priority The priority of the rule.
   * @return this instance.
   */
  public DaprSubscriptionBuilder addRule(String path, String match, int priority) {
    if (rules.stream().anyMatch(e -> e.getPriority() == priority)) {
      throw new RuntimeException(
          String.format(
              "a rule priority of %d is already used for topic %s on pubsub %s",
              priority, this.topic, this.pubsubName));
    }
    rules.add(new TopicRule(path, match, priority));
    return this;
  }

  /**
   * Sets the metadata for the subscription.
   *
   * @param metadata The metadata.
   * @return this instance.
   */
  public DaprSubscriptionBuilder setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
    return this;
  }

  /**
   * Sets the bulkSubscribe configuration for the subscription.
   *
   * @param bulkSubscribe The bulk subscribe configuration.
   * @return this instance.
   */
  public DaprSubscriptionBuilder setBulkSubscribe(DaprTopicBulkSubscribe bulkSubscribe) {
    this.bulkSubscribe = bulkSubscribe;
    return this;
  }

  /**
   * Builds the DaprTopicSubscription that is returned by the application to Dapr.
   *
   * @return The DaprTopicSubscription.
   */
  public DaprTopicSubscription build() {
    String route = null;
    DaprTopicRoutes routes = null;

    if (!rules.isEmpty()) {
      Collections.sort(rules, Comparator.comparingInt(TopicRule::getPriority));
      List<DaprTopicRule> topicRules = rules.stream()
          .map(e -> new DaprTopicRule(e.match, e.path)).collect(Collectors.toList());
      routes = new DaprTopicRoutes(topicRules, defaultPath);
    } else {
      route = defaultPath;
    }

    return new DaprTopicSubscription(this.pubsubName, this.topic, route, this.deadLetterTopic,
        routes, metadata,
        bulkSubscribe);
  }

  private static class TopicRule {

    private final String path;
    private final String match;
    private final int priority;

    public TopicRule(String path, String match, int priority) {
      this.path = path;
      this.match = match;
      this.priority = priority;
    }

    public int getPriority() {
      return priority;
    }
  }
}
