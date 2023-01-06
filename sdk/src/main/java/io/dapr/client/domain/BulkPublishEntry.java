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

package io.dapr.client.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class representing an entry in the BulkPublishRequest or BulkPublishResponse.
 *
 * @param <T> Type of the event that is part of the request.
 */
public class BulkPublishEntry<T> {
  /**
   * The ID uniquely identifying this particular request entry across the request and scoped for this request only.
   */
  private String entryId;

  /**
   * The event to be published.
   */
  private T event;

  /**
   * The content type of the event to be published. Uses MIME style content-type values.
   */
  private String contentType;

  /**
   * The metadata set for this particular event.
   * Any particular values in this metadata overrides the request metadata present in BulkPublishRequest.
   */
  private Map<String, String> metadata;

  /**
   * Default constructor for the BulkPublishRequestEntry object.
   */
  public BulkPublishEntry() {
  }

  /**
   * Constructor for the BulkPublishRequestEntry object.
   *
   * @param entryId     A request scoped ID uniquely identifying this entry in the BulkPublishRequest.
   * @param event       Event to be published.
   * @param contentType Content Type of the event to be published in MIME format.
   * @param metadata    Metadata for the event.
   */
  public BulkPublishEntry(String entryId, T event, String contentType, Map<String, String> metadata) {
    this.entryId = entryId;
    this.event = event;
    this.contentType = contentType;
    this.metadata = metadata == null ? Collections.unmodifiableMap(new HashMap<>()) :
        Collections.unmodifiableMap(metadata);
  }

  public String getEntryId() {
    return entryId;
  }

  public BulkPublishEntry<T> setEntryId(String entryId) {
    this.entryId = entryId;
    return this;
  }

  public T getEvent() {
    return event;
  }

  public BulkPublishEntry<T> setEvent(T event) {
    this.event = event;
    return this;
  }

  public String getContentType() {
    return contentType;
  }

  public BulkPublishEntry<T> setContentType(String contentType) {
    this.contentType = contentType;
    return this;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public BulkPublishEntry<T> setMetadata(Map<String, String> metadata) {
    this.metadata = metadata == null ? null : Collections.unmodifiableMap(metadata);
    return this;
  }
}
