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
 * What a write does when waiting for indexing to finish takes longer than the wait timeout.
 *
 * <p>This is an alpha API. Its name and behavior can change in a later release.
 */
public enum IndexingWaitTimeoutAction {

  /**
   * Return {@link IndexAck#QUEUED} and let the store finish the write in the background.
   * Not valid for stores that cannot queue writes.
   */
  CONTINUE_ASYNC,

  /**
   * Fail the call with DEADLINE_EXCEEDED. The store may still finish the write.
   */
  FAIL_REQUEST
}
