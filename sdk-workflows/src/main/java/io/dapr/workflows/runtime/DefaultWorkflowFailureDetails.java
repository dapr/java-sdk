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

package io.dapr.workflows.runtime;

import io.dapr.durabletask.FailureDetails;
import io.dapr.workflows.client.WorkflowFailureDetails;

/**
 * Represents a workflow failure details.
 */
public class DefaultWorkflowFailureDetails implements WorkflowFailureDetails {

  private final FailureDetails workflowFailureDetails;

  /**
   * Class constructor.
   *
   * @param failureDetails failure Details
   */
  public DefaultWorkflowFailureDetails(FailureDetails failureDetails) {
    this.workflowFailureDetails = failureDetails;
  }

  /**
   * Gets the error type, which is the namespace-qualified exception type name.
   *
   * @return the error type, which is the namespace-qualified exception type name
   */
  @Override
  public String getErrorType() {
    return workflowFailureDetails.getErrorType();
  }

  /**
   * Gets the error message.
   *
   * @return the error message
   */
  @Override
  public String getErrorMessage() {
    return workflowFailureDetails.getErrorMessage();
  }

  /**
   * Gets the stack trace.
   *
   * @return the stack trace
   */
  @Override
  public String getStackTrace() {
    return workflowFailureDetails.getStackTrace();
  }

  /**
   * Checks whether the failure was caused by the provided exception class.
   *
   * @param exceptionClass the exception class to check
   * @return {@code true} if the failure was caused by the provided exception class
   */
  @Override
  public boolean isCausedBy(Class<? extends Exception> exceptionClass) {
    return workflowFailureDetails.isCausedBy(exceptionClass);
  }

  @Override
  public String toString() {
    return workflowFailureDetails.toString();
  }

}
