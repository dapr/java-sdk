/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.springboot;

import java.util.Collections;
import java.util.Map;

/**
 * Class to represent a subscription topic along with its metadata.
 */
public class DaprTopicSubscription {
  private final String topic;
  private final String route;
  private final Map<String, String> metadata;

  /**
   * Create a subscription topic.
   * @param topic The topic to subscribe to.
   * @param route Destination route for messages.
   * @param metadata Metdata for extended subscription functionality.
   */
  public DaprTopicSubscription(String topic, String route, Map<String, String> metadata) {
    this.topic = topic;
    this.route = route;
    this.metadata = Collections.unmodifiableMap(metadata);
  }

  public String getTopic() {
    return topic;
  }

  public String getRoute() {
    return route;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }
}
