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

package io.dapr.workflows.saga;

import io.dapr.workflows.WorkflowExecutionOptions;

/**
 * Information for a compensation activity.
 */
class CompensationInformation {
  private final String compensationActivityClassName;
  private final Object compensationActivityInput;
  private final WorkflowExecutionOptions options;

  /**
   * Constructor for a compensation information.
   * 
   * @param compensationActivityClassName Class name of the activity to do
   *                                        compensation.
   * @param compensationActivityInput     Input of the activity to do
   *                                        compensation.
   * @param executionOptions              Execution options to set retry strategy
   */
  public CompensationInformation(String compensationActivityClassName,
                                 Object compensationActivityInput, WorkflowExecutionOptions executionOptions) {
    this.compensationActivityClassName = compensationActivityClassName;
    this.compensationActivityInput = compensationActivityInput;
    this.options = executionOptions;
  }

  /**
   * Gets the class name of the activity.
   * 
   * @return the class name of the activity.
   */
  public String getCompensationActivityClassName() {
    return compensationActivityClassName;
  }

  /**
   * Gets the input of the activity.
   * 
   * @return the input of the activity.
   */
  public Object getCompensationActivityInput() {
    return compensationActivityInput;
  }

  /**
   * get task options.
   * 
   * @return task options, null if not set
   */
  public WorkflowExecutionOptions getExecutionOptions() {
    return options;
  }
}
