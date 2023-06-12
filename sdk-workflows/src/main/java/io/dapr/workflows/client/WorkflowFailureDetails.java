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

package io.dapr.workflows.client;

import com.microsoft.durabletask.FailureDetails;

/**
 * Represents a workflow failure details.
 */
public class WorkflowFailureDetails {

  FailureDetails workflowFailureDetails;

  /**
   * Class constructor.
   * @param failureDetails failure Details
   */
  public WorkflowFailureDetails(FailureDetails failureDetails) {
    this.workflowFailureDetails = failureDetails;
  }

  /**
   * Gets the error type, which is the namespace-qualified exception type name.
   *
   * @return the error type, which is the namespace-qualified exception type name
   */
  public String getErrorType() {
    return workflowFailureDetails.getErrorType();
  }

  /**
   * Gets the error message.
   *
   * @return the error message
   */
  public String getErrorMessage() {
    return workflowFailureDetails.getErrorMessage();
  }

  /**
   * Gets the stack trace.
   *
   * @return the stack trace
   */
  public String getStackTrace() {
    return workflowFailureDetails.getStackTrace();
  }

  @Override
  public String toString() {
    return workflowFailureDetails.toString();
  }
}
