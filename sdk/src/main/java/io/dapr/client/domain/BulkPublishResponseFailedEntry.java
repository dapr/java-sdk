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

/**
 * Class representing the entry that failed to be published using BulkPublishRequest.
 */
public final class BulkPublishResponseFailedEntry<T> {
  /**
   * The entry that failed to be published.
   */
  private final BulkPublishEntry<T> entry;

  /**
   * Error message as to why the entry failed to publish.
   */
  private final String errorMessage;

  /**
   * Constructor for BulkPublishResponseFailedEntry.
   * @param entry        The entry that has failed.
   * @param errorMessage The error message for why the entry failed.
   */
  public BulkPublishResponseFailedEntry(BulkPublishEntry<T> entry, String errorMessage) {
    this.entry = entry;
    this.errorMessage = errorMessage;
  }

  public BulkPublishEntry<T> getEntry() {
    return entry;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
