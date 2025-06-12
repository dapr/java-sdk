/*
 * Copyright 2023 The Dapr Authors
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a bulk of messages received from the message bus.
 */
public final class BulkSubscribeMessage<T> {
  private final List<BulkSubscribeMessageEntry<T>> entries;
  private final String topic;
  private final Map<String, String> metadata;

  /**
   * Instantiate a BulkSubscribeMessage.
   * @param entries maps a {@link BulkSubscribeMessageEntry} to a {@link BulkSubscribeAppResponseStatus}.
   * @param topic pubSub topic.
   * @param metadata metadata for the {@link BulkSubscribeMessage}
   */
  @JsonCreator
  public BulkSubscribeMessage(
          @JsonProperty("entries") List<BulkSubscribeMessageEntry<T>> entries,
          @JsonProperty("topic") String topic,
          @JsonProperty("metadata") Map<String, String> metadata) {
    this.entries = Collections.unmodifiableList(entries);
    this.topic = topic;
    this.metadata = Collections.unmodifiableMap(metadata);
  }

  public List<BulkSubscribeMessageEntry<T>> getEntries() {
    return entries;
  }

  public String getTopic() {
    return topic;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }
}
