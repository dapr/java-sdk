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
 * Class representing the entry that failed to be published using BulkPublishRequest.
 */
public class BulkPublishResponseFailedEntry<T> {
  /**
   * The entry that failed to be published.
   */
  private BulkPublishEntry<T> entry;

  /**
   * Error message as to why the entry failed to publish.
   */
  private String errorMessage;

  public BulkPublishEntry<T> getEntry() {
    return entry;
  }

  public BulkPublishResponseFailedEntry setEntry(BulkPublishEntry<T> entry) {
    this.entry = entry;
    return this;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
