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

package io.dapr.workflows;

import io.dapr.workflows.client.WorkflowFailureDetails;
import io.dapr.workflows.runtime.DefaultWorkflowContext;

import java.time.Duration;

public class WorkflowTaskRetryContext {

  private final DefaultWorkflowContext workflowContext;
  private final int lastAttemptNumber;
  private final WorkflowFailureDetails lastFailure;
  private final Duration totalRetryTime;

  /**
   * Constructor for WorkflowTaskRetryContext.
   *
   * @param workflowContext The workflow context
   * @param lastAttemptNumber The number of the previous attempt
   * @param lastFailure The failure details from the most recent failure
   * @param totalRetryTime The amount of time spent retrying
   */
  public WorkflowTaskRetryContext(
          DefaultWorkflowContext workflowContext,
          int lastAttemptNumber,
          WorkflowFailureDetails lastFailure,
          Duration totalRetryTime) {
    this.workflowContext = workflowContext;
    this.lastAttemptNumber = lastAttemptNumber;
    this.lastFailure = lastFailure;
    this.totalRetryTime = totalRetryTime;
  }

  /**
   * Gets the context of the current workflow.
   *
   * <p>The workflow context can be used in retry handlers to schedule timers (via the
   * {@link DefaultWorkflowContext#createTimer} methods) for implementing delays between retries. It can also be
   * used to implement time-based retry logic by using the {@link DefaultWorkflowContext#getCurrentInstant} method.
   *
   * @return the context of the parent workflow
   */
  public DefaultWorkflowContext getWorkflowContext() {
    return this.workflowContext;
  }

  /**
   * Gets the details of the previous task failure, including the exception type, message, and callstack.
   *
   * @return the details of the previous task failure
   */
  public WorkflowFailureDetails getLastFailure() {
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
