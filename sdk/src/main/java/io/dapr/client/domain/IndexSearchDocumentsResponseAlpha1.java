/*
 * Copyright 2026 The Dapr Authors
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

import java.util.List;

/**
 * Result of storing documents in a search index.
 *
 * <p>This is an alpha API. Its name and behavior can change in a later release.
 */
public class IndexSearchDocumentsResponseAlpha1 {

  private final IndexAck ack;
  private final List<FailedItem> failedItems;

  /**
   * Constructor for IndexSearchDocumentsResponseAlpha1.
   *
   * @param ack         How far the store got with the write.
   * @param failedItems Documents the store could not write.
   */
  public IndexSearchDocumentsResponseAlpha1(IndexAck ack, List<FailedItem> failedItems) {
    this.ack = ack;
    this.failedItems = List.copyOf(failedItems);
  }

  /**
   * Gets how far the store got with the write.
   *
   * @return the acknowledgement
   */
  public IndexAck getAck() {
    return ack;
  }

  /**
   * Gets the documents the store could not write. After {@link IndexAck#QUEUED}, this lists only the
   * failures known when the store accepted the write.
   *
   * @return the failed documents
   */
  public List<FailedItem> getFailedItems() {
    return failedItems;
  }
}
