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
public class DaprTopicSubscription {
  private final String pubsubName;
  private final String topic;
  private final String route;
  private final Map<String, String> metadata;

  /**
   * Create a subscription topic.
   * @param pubsubName The pubsub name to subscribe to.
   * @param topic The topic to subscribe to.
   * @param route Destination route for messages.
   * @param metadata Metdata for extended subscription functionality.
   */
  public DaprTopicSubscription(String pubsubName, String topic, String route, Map<String, String> metadata) {
    this.pubsubName = pubsubName;
    this.topic = topic;
    this.route = route;
    this.metadata = Collections.unmodifiableMap(metadata);
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

  public Map<String, String> getMetadata() {
    return metadata;
  }
}
