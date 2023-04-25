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

import java.util.Collections;
import java.util.Map;

/**
 * Class to represent a subscription topic along with its metadata.
 */
class DaprTopicSubscription {
  private final String pubsubName;
  private final String topic;
  private final String route;
  private final String deadLetterTopic;
  private final DaprTopicRoutes routes;
  private final Map<String, String> metadata;
  private final DaprTopicBulkSubscribe bulkSubscribe;

  /**
   * Create a subscription topic.
   * @param pubsubName The pubsub name to subscribe to.
   * @param topic The topic to subscribe to.
   * @param route Destination route for messages.
   * @param metadata Metadata for extended subscription functionality.
   */
  DaprTopicSubscription(String pubsubName, String topic, String route, Map<String, String> metadata) {
    this(pubsubName, topic, route, metadata, null);
  }

  /**
   * Create a subscription topic.
   * @param pubsubName The pubsub name to subscribe to.
   * @param topic The topic to subscribe to.
   * @param route Destination route for messages.
   * @param deadLetterTopic Name of topic to forward undeliverable messages.
   * @param metadata Metadata for extended subscription functionality.
   */
  DaprTopicSubscription(String pubsubName, String topic, String route, String deadLetterTopic,
      Map<String, String> metadata) {
    this(pubsubName, topic, route, deadLetterTopic, null, metadata, null);
  }

  /**
   * Create a subscription topic.
   * @param pubsubName The pubsub name to subscribe to.
   * @param topic The topic to subscribe to.
   * @param route Destination route for messages.
   * @param metadata Metadata for extended subscription functionality.
   * @param bulkSubscribe Bulk subscribe configuration.
   */
  DaprTopicSubscription(String pubsubName, String topic, String route,
      Map<String, String> metadata, DaprTopicBulkSubscribe bulkSubscribe) {
    this(pubsubName, topic, route, "", null, metadata, bulkSubscribe);
  }

  /**
   * Create a subscription topic.
   * @param pubsubName The pubsub name to subscribe to.
   * @param topic The topic to subscribe to.
   * @param route Destination route for messages.
   * @param deadLetterTopic Name of topic to forward undeliverable messages.
   * @param metadata Metadata for extended subscription functionality.
   * @param bulkSubscribe Bulk subscribe configuration.
   */
  DaprTopicSubscription(String pubsubName, String topic, String route, String deadLetterTopic,
      Map<String, String> metadata, DaprTopicBulkSubscribe bulkSubscribe) {
    this(pubsubName, topic, route, deadLetterTopic, null, metadata, bulkSubscribe);
  }

  /**
   * Create a subscription topic.
   * @param pubsubName The pubsub name to subscribe to.
   * @param topic The topic to subscribe to.
   * @param route Destination route for messages.
   * @param routes Destination routes with rules for messages.
   * @param metadata Metadata for extended subscription functionality.
   */
  DaprTopicSubscription(String pubsubName, String topic, String route, DaprTopicRoutes routes,
      Map<String, String> metadata) {
    this(pubsubName, topic, route, "", routes, metadata, null);
  }

  /**
   * Create a subscription topic.
   * @param pubsubName The pubsub name to subscribe to.
   * @param topic The topic to subscribe to.
   * @param route Destination route for messages.
   * @param deadLetterTopic Name of topic to forward undeliverable messages.
   * @param routes Destination routes with rules for messages.
   * @param metadata Metadata for extended subscription functionality.
   */
  DaprTopicSubscription(String pubsubName, String topic, String route, String deadLetterTopic, DaprTopicRoutes routes,
      Map<String, String> metadata) {
    this(pubsubName, topic, route, deadLetterTopic, routes, metadata, null);
  }

  /**
   * Create a subscription topic.
   *
   * @param pubsubName      The pubsub name to subscribe to.
   * @param topic           The topic to subscribe to.
   * @param route           Destination route for messages.
   * @param routes          Destination routes with rules for messages.
   * @param metadata        Metadata for extended subscription functionality.
   * @param bulkSubscribe   Bulk subscribe configuration.
   */
  DaprTopicSubscription(String pubsubName, String topic, String route,
      DaprTopicRoutes routes,
      Map<String, String> metadata,
      DaprTopicBulkSubscribe bulkSubscribe) {
    this(pubsubName, topic, route, "", routes, metadata, bulkSubscribe);
  }


  /**
   * Create a subscription topic.
   *
   * @param pubsubName      The pubsub name to subscribe to.
   * @param topic           The topic to subscribe to.
   * @param route           Destination route for messages.
   * @param deadLetterTopic Name of topic to forward undeliverable messages.
   * @param routes          Destination routes with rules for messages.
   * @param metadata        Metadata for extended subscription functionality.
   * @param bulkSubscribe   Bulk subscribe configuration.
   */
  DaprTopicSubscription(String pubsubName, String topic, String route, String deadLetterTopic,
      DaprTopicRoutes routes,
      Map<String, String> metadata,
      DaprTopicBulkSubscribe bulkSubscribe) {
    this.pubsubName = pubsubName;
    this.topic = topic;
    this.route = route;
    this.routes = routes;
    this.deadLetterTopic = deadLetterTopic;
    this.metadata = Collections.unmodifiableMap(metadata);
    this.bulkSubscribe = bulkSubscribe;
  }

  public String getPubsubName() {
    return pubsubName;
  }

  public String getTopic() {
    return topic;
  }

  public String getRoute() {
    return route;
  }

  public DaprTopicRoutes getRoutes() {
    return routes;
  }

  public String getDeadLetterTopic() {
    return deadLetterTopic;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public DaprTopicBulkSubscribe getBulkSubscribe() {
    return bulkSubscribe;
  }
}
