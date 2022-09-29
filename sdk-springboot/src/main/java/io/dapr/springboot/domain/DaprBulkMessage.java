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

package io.dapr.springboot.domain;

import java.util.Map;

/**
 * Represents a bulk of messages received from the message bus.
 */
public class DaprBulkMessage {
  public DaprBulkMessageEntry<?>[] entries;
  public String topic;
  public Map<String, String> metadata;

  public DaprBulkMessage() {
  }

  /**
   * Create a DaprBulkMessage from entries, topic and metadata.
   * @param entries mapping from bulk message entry ID to a status.
   * @param topic pubSub topic.
   * @param metadata metadata for the bulk message.
   */
  public DaprBulkMessage(DaprBulkMessageEntry<?>[] entries, String topic, Map<String, String> metadata) {
    this.entries = entries;
    this.topic = topic;
    this.metadata = metadata;
  }

  public void setEntries(DaprBulkMessageEntry<?>[] entries) {
    this.entries = entries;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }
}