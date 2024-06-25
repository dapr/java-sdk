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

/**
 * Response from the application containing status for each entry from the bulk publish message.
 */
public final class BulkSubscribeAppResponse {
  private final List<BulkSubscribeAppResponseEntry> statuses;

  /**
   * Instantiate a BulkSubscribeAppResponse.
   * @param statuses list of statuses.
   */
  @JsonCreator
  public BulkSubscribeAppResponse(
          @JsonProperty("statuses") List<BulkSubscribeAppResponseEntry> statuses) {
    this.statuses = Collections.unmodifiableList(statuses);
  }

  public List<BulkSubscribeAppResponseEntry> getStatuses() {
    return statuses;
  }
}
