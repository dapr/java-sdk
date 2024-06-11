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

package io.dapr.client.domain;

import java.util.List;

/**
 * SubscriptionMetadata describes the Subscription Metadata.
 */
public final class SubscriptionMetadata {
  private String topic;
  private String pubsubname;
  private String deadLetterTopic;
  private List<RuleMetadata> rules;

  public SubscriptionMetadata() {
  }

  /**
   * Constructor for a SubscriptionMetadata.
   *
   * @param topic of the pubsub component
   * @param pubsubname component name
   * @param deadLetterTopic dead letter topic  
   * @param rules subscription path rules
   */
  public SubscriptionMetadata(String topic, String pubsubname, String deadLetterTopic, List<RuleMetadata> rules) {
    this.topic = topic;
    this.pubsubname = pubsubname;
    this.deadLetterTopic = deadLetterTopic;
    this.rules = rules;
  }

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public String getPubsubname() {
    return pubsubname;
  }

  public void setPubsubname(String pubsubname) {
    this.pubsubname = pubsubname;
  }

  public String getDeadLetterTopic() {
    return deadLetterTopic;
  }

  public void setDeadLetterTopic(String deadLetterTopic) {
    this.deadLetterTopic = deadLetterTopic;
  }

  public List<RuleMetadata> getRules() {
    return rules;
  }

  public void setRules(List<RuleMetadata> rules) {
    this.rules = rules;
  }

}
