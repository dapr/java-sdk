/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.durabletask;

import java.time.Duration;

/**
 * Context data that's provided to {@link RetryHandler} implementations.
 */
public final class RetryContext {
  private final TaskOrchestrationContext orchestrationContext;
  private final int lastAttemptNumber;
  private final FailureDetails lastFailure;
  private final Duration totalRetryTime;

  RetryContext(
      TaskOrchestrationContext orchestrationContext,
      int lastAttemptNumber,
      FailureDetails lastFailure,
      Duration totalRetryTime) {
    this.orchestrationContext = orchestrationContext;
    this.lastAttemptNumber = lastAttemptNumber;
    this.lastFailure = lastFailure;
    this.totalRetryTime = totalRetryTime;
  }

  /**
   * Gets the context of the current orchestration.
   *
   * <p>The orchestration context can be used in retry handlers to schedule timers (via the
   * {@link TaskOrchestrationContext#createTimer} methods) for implementing delays between retries. It can also be
   * used to implement time-based retry logic by using the {@link TaskOrchestrationContext#getCurrentInstant} method.
   * </p>
   *
   * @return the context of the parent orchestration
   */
  public TaskOrchestrationContext getOrchestrationContext() {
    return this.orchestrationContext;
  }

  /**
   * Gets the details of the previous task failure, including the exception type, message, and callstack.
   *
   * @return the details of the previous task failure
   */
  public FailureDetails getLastFailure() {
    return this.lastFailure;
  }

  /**
   * Gets the previous retry attempt number. This number starts at 1 and increments each time the retry handler
   * is invoked for a particular task failure.
   *
   * @return the previous retry attempt number
   */
  public int getLastAttemptNumber() {
    return this.lastAttemptNumber;
  }

  /**
   * Gets the total amount of time spent in a retry loop for the current task.
   *
   * @return the total amount of time spent in a retry loop for the current task
   */
  public Duration getTotalRetryTime() {
    return this.totalRetryTime;
  }
}
