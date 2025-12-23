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

package io.dapr.testcontainers.wait.strategy.metadata;

import java.util.List;
import java.util.Map;

/**
 * Represents a subscription entry from the Dapr metadata API response.
 */
public class Subscription {
  private String pubsubname;
  private String topic;
  private String deadLetterTopic;
  private Map<String, String> metadata;
  private List<Rule> rules;
  private String type;

  public Subscription() {
  }

  public String getPubsubname() {
    return pubsubname;
  }

  public void setPubsubname(String pubsubname) {
    this.pubsubname = pubsubname;
  }

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public String getDeadLetterTopic() {
    return deadLetterTopic;
  }

  public void setDeadLetterTopic(String deadLetterTopic) {
    this.deadLetterTopic = deadLetterTopic;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }

  public List<Rule> getRules() {
    return rules;
  }

  public void setRules(List<Rule> rules) {
    this.rules = rules;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  /**
   * Represents a routing rule for a subscription.
   */
  public static class Rule {
    private String match;
    private String path;

    public Rule() {
    }

    public String getMatch() {
      return match;
    }

    public void setMatch(String match) {
      this.match = match;
    }

    public String getPath() {
      return path;
    }

    public void setPath(String path) {
      this.path = path;
    }
  }
}
