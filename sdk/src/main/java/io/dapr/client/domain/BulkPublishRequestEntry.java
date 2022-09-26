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

import java.util.Map;

/**
 * Class representing an entry in the BulkPublishRequest.
 *
 * @param <T> Type of the event that is part of the request.
 */
public class BulkPublishRequestEntry<T> {
  private String entryID;
  private T event;
  private String contentType;
  private Map<String, String> metadata;

  /**
   * Default constructor for the BulkPublishRequestEntry object.
   */
  public BulkPublishRequestEntry() {
  }

  /**
   * Constructor for the BulkPublishRequestEntry object.
   *
   * @param entryID         A request scoped ID uniquely identifying this entry in the BulkPublishRequest.
   * @param event           Event to be published.
   * @param contentType Content Type of the event to be published in MIME format.
   * @param metadata        Metadata for the event.
   */
  public BulkPublishRequestEntry(String entryID, T event, String contentType, Map<String, String> metadata) {
    this.entryID = entryID;
    this.event = event;
    this.contentType = contentType;
    this.metadata = metadata;
  }

  public String getEntryID() {
    return entryID;
  }

  public void setEntryID(String entryID) {
    this.entryID = entryID;
  }

  public T getEvent() {
    return event;
  }

  public void setEvent(T event) {
    this.event = event;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }
}
