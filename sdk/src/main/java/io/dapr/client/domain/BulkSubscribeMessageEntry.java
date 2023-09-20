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
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single event from a {@link BulkSubscribeMessage}.
 * @param <T> Type of event.
 */
public final class BulkSubscribeMessageEntry<T> {
  private final String entryId;
  private final T event;
  private final String contentType;
  private final Map<String, String> metadata;

  /**
   * Instantiate a BulkPubSubMessageEntry.
   * @param entryId unique identifier for the event.
   * @param event pubSub event.
   * @param contentType content type of the event.
   * @param metadata metadata for the event.
   */
  @JsonCreator
  public BulkSubscribeMessageEntry(
          @JsonProperty("entryId") String entryId,
          @JsonProperty("event") T event,
          @JsonProperty("contentType") String contentType,
          @JsonProperty("metadata") Map<String, String> metadata) {
    this.entryId = entryId;
    this.event = event;
    this.contentType = contentType;

    if (metadata == null) {
      metadata = new HashMap<>();
    }
    this.metadata = Collections.unmodifiableMap(metadata);
  }

  public String getEntryId() {
    return entryId;
  }

  public T getEvent() {
    return event;
  }

  public String getContentType() {
    return contentType;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }
}
