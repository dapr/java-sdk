/*
 * Copyright 2021 The Dapr Authors
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Internal Singleton to handle Dapr configuration.
 */
class DaprRuntime {

  /**
   * The singleton instance.
   */
  private static volatile DaprRuntime instance;

  /**
   * Map of subscription builders.
   */
  private final Map<DaprTopicKey, DaprSubscriptionBuilder> subscriptionBuilders = new HashMap<>();

  /**
   * DaprRuntime should be used as a singleton, using {@link DaprRuntime#getInstance()}. The
   * constructor's default scope is available for unit tests only.
   */
  private DaprRuntime() {
  }

  /**
   * Returns an DaprRuntime object.
   *
   * @return An DaprRuntime object.
   */
  public static DaprRuntime getInstance() {
    if (instance == null) {
      synchronized (DaprRuntime.class) {
        if (instance == null) {
          instance = new DaprRuntime();
        }
      }
    }

    return instance;
  }

  /**
   * Adds a topic to the list of subscribed topics.
   *
   * @param pubSubName PubSub name to subscribe to.
   * @param topicName  Name of the topic being subscribed to.
   * @param match      Match expression for this route.
   * @param priority   Priority for this match relative to others.
   * @param route      Destination route for requests.
   * @param metadata   Metadata for extended subscription functionality.
   */
  public synchronized void addSubscribedTopic(String pubSubName,
                                              String topicName,
                                              String match,
                                              int priority,
                                              String route,
                                              Map<String, String> metadata) {
    this.addSubscribedTopic(pubSubName, topicName, match, priority, route, metadata, null);
  }

  /**
   * Adds a topic to the list of subscribed topics.
   *
   * @param pubSubName    PubSub name to subscribe to.
   * @param topicName     Name of the topic being subscribed to.
   * @param match         Match expression for this route.
   * @param priority      Priority for this match relative to others.
   * @param route         Destination route for requests.
   * @param metadata      Metadata for extended subscription functionality.
   * @param bulkSubscribe Bulk subscribe configuration.
   */
  public synchronized void addSubscribedTopic(String pubSubName,
                                              String topicName,
                                              String match,
                                              int priority,
                                              String route,
                                              Map<String, String> metadata,
                                              DaprTopicBulkSubscribe bulkSubscribe) {
    this.addSubscribedTopic(pubSubName, topicName, match, priority, route, null,
        metadata, bulkSubscribe);
  }

  /**
   * Adds a topic to the list of subscribed topics.
   *
   * @param pubSubName      PubSub name to subscribe to.
   * @param topicName       Name of the topic being subscribed to.
   * @param match           Match expression for this route.
   * @param priority        Priority for this match relative to others.
   * @param route           Destination route for requests.
   * @param deadLetterTopic Name of topic to forward undeliverable messages.
   * @param metadata        Metadata for extended subscription functionality.
   */
  public synchronized void addSubscribedTopic(String pubSubName,
                                              String topicName,
                                              String match,
                                              int priority,
                                              String route,
                                              String deadLetterTopic,
                                              Map<String, String> metadata) {
    this.addSubscribedTopic(pubSubName, topicName, match, priority, route, deadLetterTopic,
        metadata, null);
  }

  /**
   * Adds a topic to the list of subscribed topics.
   *
   * @param pubSubName      PubSub name to subscribe to.
   * @param topicName       Name of the topic being subscribed to.
   * @param match           Match expression for this route.
   * @param priority        Priority for this match relative to others.
   * @param route           Destination route for requests.
   * @param deadLetterTopic Name of topic to forward undeliverable messages.
   * @param metadata        Metadata for extended subscription functionality.
   * @param bulkSubscribe   Bulk subscribe configuration.
   */
  public synchronized void addSubscribedTopic(String pubSubName,
                                              String topicName,
                                              String match,
                                              int priority,
                                              String route,
                                              String deadLetterTopic,
                                              Map<String, String> metadata,
                                              DaprTopicBulkSubscribe bulkSubscribe) {
    DaprTopicKey topicKey = new DaprTopicKey(pubSubName, topicName);

    DaprSubscriptionBuilder builder = subscriptionBuilders.get(topicKey);
    if (builder == null) {
      builder = new DaprSubscriptionBuilder(pubSubName, topicName);
      subscriptionBuilders.put(topicKey, builder);
    }

    if (match.length() > 0) {
      builder.addRule(route, match, priority);
    } else {
      builder.setDefaultPath(route);
    }

    if (metadata != null && !metadata.isEmpty()) {
      builder.setMetadata(metadata);
    }

    if (deadLetterTopic != null && !deadLetterTopic.isEmpty()) {
      builder.setDeadLetterTopic(deadLetterTopic);
    }

    if (bulkSubscribe != null) {
      builder.setBulkSubscribe(bulkSubscribe);
    }
  }

  public synchronized DaprTopicSubscription[] listSubscribedTopics() {
    List<DaprTopicSubscription> values = subscriptionBuilders.values().stream()
        .map(b -> b.build()).collect(Collectors.toList());
    return values.toArray(new DaprTopicSubscription[0]);
  }
}
