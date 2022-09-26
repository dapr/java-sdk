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

/**
 * Class representing the status of each event that was published using BulkPublishRequest.
 */
public class BulkPublishResponseEntry {
  /**
   * An ID unique across the Response object, identifying a particular entry in the BulkPublishRequest.
   */
  private String entryID;

  /**
   * The publishing status of this particular event.
   */
  private PublishStatus status;

  public enum PublishStatus {
    SUCCESS, FAILED
  }

  public String getEntryID() {
    return entryID;
  }

  public BulkPublishResponseEntry setEntryID(String entryID) {
    this.entryID = entryID;
    return this;
  }

  public PublishStatus getStatus() {
    return status;
  }

  public BulkPublishResponseEntry setStatus(PublishStatus status) {
    this.status = status;
    return this;
  }
}
