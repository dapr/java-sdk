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

import java.time.Duration;

/**
 * Controls when a write to a vector or search store returns.
 *
 * <p>Without options, a write returns once the store accepts it.
 * This is an alpha API. Its name and behavior can change in a later release.
 */
public final class IndexingOptions {

  private final boolean waitForCompletion;
  private final Duration waitTimeout;
  private final IndexingWaitTimeoutAction onWaitTimeout;

  private IndexingOptions(boolean waitForCompletion, Duration waitTimeout, IndexingWaitTimeoutAction onWaitTimeout) {
    this.waitForCompletion = waitForCompletion;
    this.waitTimeout = waitTimeout;
    this.onWaitTimeout = onWaitTimeout;
  }

  /**
   * Returns as soon as the store accepts the write.
   *
   * @return the indexing options
   */
  public static IndexingOptions returnOnAcceptance() {
    return new IndexingOptions(false, null, null);
  }

  /**
   * Waits until the store finishes the write, for at most the given time.
   *
   * <p>The timeout must be shorter than any deadline on the call.
   *
   * @param waitTimeout   how long to wait. Must be positive.
   * @param onWaitTimeout what to do when the wait times out. Required.
   * @return the indexing options
   */
  public static IndexingOptions waitForCompletion(Duration waitTimeout, IndexingWaitTimeoutAction onWaitTimeout) {
    return new IndexingOptions(true, waitTimeout, onWaitTimeout);
  }

  /**
   * Gets whether the write waits for indexing to finish.
   *
   * @return true to wait for completion, false to return on acceptance
   */
  public boolean isWaitForCompletion() {
    return waitForCompletion;
  }

  /**
   * Gets how long the write waits for indexing to finish.
   *
   * @return the wait timeout, or null when the write returns on acceptance
   */
  public Duration getWaitTimeout() {
    return waitTimeout;
  }

  /**
   * Gets what the write does when the wait times out.
   *
   * @return the action, or null when the write returns on acceptance
   */
  public IndexingWaitTimeoutAction getOnWaitTimeout() {
    return onWaitTimeout;
  }
}
