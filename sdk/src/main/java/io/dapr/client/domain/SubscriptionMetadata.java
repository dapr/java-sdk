/*
 * Copyright 2024 The Dapr Authors
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

package io.dapr.client.domain;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * SubscriptionMetadata describes the Subscription Metadata.
 */
public final class SubscriptionMetadata {

  private final String pubsubname;
  private final String topic;
  private final Map<String, String> metadata;
  private final List<RuleMetadata> rules;
  private final String deadLetterTopic;

  /**
   * Constructor for a SubscriptionMetadata.
   *
   * @param pubsubname component name
   * @param topic of the pubsub component
   * @param metadata of the pubsub component
   * @param rules subscription path rules
   * @param deadLetterTopic dead letter topic
   */
  public SubscriptionMetadata(String pubsubname, String topic, Map<String, String> metadata, List<RuleMetadata> rules,
      String deadLetterTopic) {
    this.pubsubname = pubsubname;
    this.topic = topic;
    this.metadata = metadata == null ? Collections.emptyMap() : Collections.unmodifiableMap(metadata);
    this.rules = rules == null ? Collections.emptyList() : Collections.unmodifiableList(rules);
    this.deadLetterTopic = deadLetterTopic;
  }

  public String getPubsubname() {
    return pubsubname;
  }

  public String getTopic() {
    return topic;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public List<RuleMetadata> getRules() {
    return rules;
  }

  public String getDeadLetterTopic() {
    return deadLetterTopic;
  }

}
