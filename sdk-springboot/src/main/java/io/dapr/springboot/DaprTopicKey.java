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

import java.util.Objects;

class DaprTopicKey {
  private final String pubsubName;
  private final String topic;

  DaprTopicKey(String pubsubName, String topic) {
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
