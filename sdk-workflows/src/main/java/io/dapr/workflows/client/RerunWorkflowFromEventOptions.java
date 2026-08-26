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

import javax.annotation.Nullable;

/**
 * Options for the {@link DaprWorkflowClient#rerunWorkflowFromEvent(String, int, RerunWorkflowFromEventOptions)}
 * operation.
 */
public final class RerunWorkflowFromEventOptions {

  @Nullable
  private String newInstanceId;
  @Nullable
  private Object input;
  private boolean overwriteInput;

  /**
   * Sets the instance ID to use for the new workflow instance. When not set, a random ID is generated.
   *
   * @param newInstanceId the new instance ID
   * @return this {@link RerunWorkflowFromEventOptions} object
   */
  public RerunWorkflowFromEventOptions setNewInstanceId(String newInstanceId) {
    this.newInstanceId = newInstanceId;
    return this;
  }

  /**
   * Sets the input applied at the next activity event of the rerun instance. When set,
   * {@link #setOverwriteInput(boolean)} must also be set to true.
   *
   * @param input the input to apply
   * @return this {@link RerunWorkflowFromEventOptions} object
   */
  public RerunWorkflowFromEventOptions setInput(Object input) {
    this.input = input;
    return this;
  }

  /**
   * Sets whether the input at the rerun point is overwritten with {@link #setInput(Object)}.
   *
   * @param overwriteInput true to overwrite the input
   * @return this {@link RerunWorkflowFromEventOptions} object
   */
  public RerunWorkflowFromEventOptions setOverwriteInput(boolean overwriteInput) {
    this.overwriteInput = overwriteInput;
    return this;
  }

  /**
   * Gets the new instance ID.
   *
   * @return the new instance ID, or null if not set
   */
  @Nullable
  public String getNewInstanceId() {
    return this.newInstanceId;
  }

  /**
   * Gets the input to apply at the rerun point.
   *
   * @return the input, or null if not set
   */
  @Nullable
  public Object getInput() {
    return this.input;
  }

  /**
   * Gets whether the input at the rerun point is overwritten.
   *
   * @return true if the input is overwritten
   */
  public boolean isOverwriteInput() {
    return this.overwriteInput;
  }
}
