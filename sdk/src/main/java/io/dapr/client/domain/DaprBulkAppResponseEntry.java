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

/**
 * Maps an entry from bulk subscribe messages to a response status.
 */
public class DaprBulkAppResponseEntry {
  private String entryID;
  private DaprBulkAppResponseStatus status;

  /**
   * Instantiate a DaprBulkAppResponseEntry.
   */
  public DaprBulkAppResponseEntry() {
  }

  /**
   * Instantiate a DaprBulkAppResponseEntry.
   * @param entryID entry ID of the event.
   * @param status status of the event processing in application.
   */
  public DaprBulkAppResponseEntry(String entryID, DaprBulkAppResponseStatus status) {
    this.entryID = entryID;
    this.status = status;
  }

  public String getEntryID() {
    return entryID;
  }

  public void setEntryID(String entryID) {
    this.entryID = entryID;
  }

  public DaprBulkAppResponseStatus getStatus() {
    return status;
  }

  public void setStatus(DaprBulkAppResponseStatus status) {
    this.status = status;
  }
}