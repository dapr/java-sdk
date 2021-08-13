/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.springboot;

import java.util.Objects;

public class DaprTopicKey {
  private final String pubsubName;
  private final String topic;

  public DaprTopicKey(String pubsubName, String topic) {
    this.pubsubName = pubsubName;
    this.topic = topic;
  }

  public String getPubsubName() {
    return pubsubName;
  }

  public String getTopic() {
    return topic;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DaprTopicKey that = (DaprTopicKey) o;
    return pubsubName.equals(that.pubsubName) && topic.equals(that.topic);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pubsubName, topic);
  }
}
