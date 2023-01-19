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

  /**
   * List of {@link BulkPublishResponseFailedEntry} objects that have failed to publish.
   */
  private final List<BulkPublishResponseFailedEntry<T>> failedEntries;

  /**
   * Default constructor for class.
   */
  public BulkPublishResponse() {
    this.failedEntries = Collections.unmodifiableList(new ArrayList<>());
  }

  /**
   * Constructor for the BulkPublishResponse object.
   *
   * @param failedEntries  The List of BulkPublishResponseEntries representing the list of
   *                       events that failed to be published.
   */
  public BulkPublishResponse(List<BulkPublishResponseFailedEntry<T>> failedEntries) {
    this.failedEntries = failedEntries == null ? Collections.unmodifiableList(new ArrayList<>()) :
        Collections.unmodifiableList(failedEntries);
  }

  public List<BulkPublishResponseFailedEntry<T>> getFailedEntries() {
    return failedEntries;
  }
}
