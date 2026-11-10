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

import io.dapr.exceptions.DaprException;

/**
 * An item that a write to a vector or search store could not store or delete.
 *
 * <p>This is an alpha API. Its name and behavior can change in a later release.
 */
public class FailedItem {

  private final String id;
  private final DaprException error;

  /**
   * Constructor for FailedItem.
   *
   * @param id    ID of the item that failed.
   * @param error why the item failed.
   */
  public FailedItem(String id, DaprException error) {
    this.id = id;
    this.error = error;
  }

  /**
   * Gets the ID of the item that failed.
   *
   * @return the item ID
   */
  public String getId() {
    return id;
  }

  /**
   * Gets why the item failed, with the gRPC status code, message and error details from the store.
   *
   * @return the error
   */
  public DaprException getError() {
    return error;
  }
}
