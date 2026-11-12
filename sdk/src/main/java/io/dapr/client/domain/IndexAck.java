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

/**
 * Acknowledgement of a write to a vector or search store.
 *
 * <p>This is an alpha API. Its name and behavior can change in a later release.
 */
public enum IndexAck {

  /**
   * The store accepted the write for background processing. This does not mean any item is indexed yet.
   */
  QUEUED,

  /**
   * The store finished the write. The response lists every item that failed.
   */
  COMPLETED
}
