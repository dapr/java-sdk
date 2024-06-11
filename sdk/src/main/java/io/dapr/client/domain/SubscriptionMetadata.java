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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
    this.rules = rules == null ? Collections.emptyList() : Collections.unmodifiableList(rules);
  }

  public String getTopic() {
    return topic;
  }

  public String getPubsubname() {
    return pubsubname;
  }


  public String getDeadLetterTopic() {
    return deadLetterTopic;
  }


  public List<RuleMetadata> getRules() {
    return rules;
  }

  @Override
  public int hashCode() {
    return Objects.hash(topic, pubsubname, deadLetterTopic, rules);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    SubscriptionMetadata other = (SubscriptionMetadata) obj;
    return Objects.equals(topic, other.topic) && Objects.equals(pubsubname, other.pubsubname)
        && Objects.equals(deadLetterTopic, other.deadLetterTopic) && Objects.equals(rules, other.rules);
  }

  

}
