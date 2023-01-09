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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class representing the response returned on bulk publishing events.
 */
public class BulkPublishResponse<T> {

  private String errorCode;
  private List<BulkPublishResponseFailedEntry<T>> failedEntries;

  /**
   * Default constructor for the class.
   */
  public BulkPublishResponse() {
  }

  /**
   * Constructor for the BulkPublishResponse object.
   *
   * @param errorCode Dapr errorCode if any.
   * @param failedEntries  The List of BulkPublishResponseEntries representing the list of
   *                       events that failed to be published.
   */
  public BulkPublishResponse(String errorCode, List<BulkPublishResponseFailedEntry<T>> failedEntries) {
    this.errorCode = errorCode;
    this.failedEntries = failedEntries == null ? Collections.unmodifiableList(new ArrayList<>()) :
        Collections.unmodifiableList(failedEntries);
  }

  public String getErrorCode() {
    return errorCode;
  }

  public BulkPublishResponse<T> setErrorCode(String errorCode) {
    this.errorCode = errorCode;
    return this;
  }

  public List<BulkPublishResponseFailedEntry<T>> getFailedEntries() {
    return failedEntries;
  }

  public BulkPublishResponse<T> setFailedEntries(List<BulkPublishResponseFailedEntry<T>> failedEntries) {
    this.failedEntries = failedEntries == null ? new ArrayList<>() : Collections.unmodifiableList(failedEntries);
    return this;
  }
}
