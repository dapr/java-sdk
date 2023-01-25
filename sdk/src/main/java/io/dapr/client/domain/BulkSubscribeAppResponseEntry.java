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

/**
 * Maps a {@link BulkSubscribeMessageEntry} to a {@link BulkSubscribeAppResponseStatus}.
 */
public final class BulkSubscribeAppResponseEntry {

  private final String entryId;
  private final BulkSubscribeAppResponseStatus status;

  /**
   * Instantiate a BulkSubscribeAppResponseEntry.
   * @param entryId entry ID of the event.
   * @param status status of the event processing in application.
   */
  @JsonCreator
  public BulkSubscribeAppResponseEntry(
          @JsonProperty("entryId") String entryId,
          @JsonProperty("status") BulkSubscribeAppResponseStatus status) {
    this.entryId = entryId;
    this.status = status;
  }

  public String getEntryId() {
    return entryId;
  }

  public BulkSubscribeAppResponseStatus getStatus() {
    return status;
  }
}
