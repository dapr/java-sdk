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

package io.dapr.durabletask;

import com.google.protobuf.StringValue;
import io.dapr.durabletask.implementation.protobuf.Orchestration.TaskFailureDetails;

/**
 * Holds the status and data of a named child workflow invocation observed in
 * propagated workflow history.
 */
public final class ChildWorkflowResult {
  private final String name;
  private final boolean completed;
  private final boolean failed;
  private final StringValue output;
  private final TaskFailureDetails error;

  ChildWorkflowResult(String name,
                      boolean completed,
                      boolean failed,
                      StringValue output,
                      TaskFailureDetails error) {
    this.name = name;
    this.completed = completed;
    this.failed = failed;
    this.output = output;
    this.error = error;
  }

  /**
   * Gets the child workflow name.
   *
   * @return the child workflow name
   */
  public String getName() {
    return this.name;
  }

  /**
   * Returns true if the child workflow completed successfully.
   *
   * @return whether the child workflow completed
   */
  public boolean isCompleted() {
    return this.completed;
  }

  /**
   * Returns true if the child workflow failed.
   *
   * @return whether the child workflow failed
   */
  public boolean isFailed() {
    return this.failed;
  }

  /**
   * Gets the child workflow output from the matching completion event, or
   * null if the child workflow has not completed successfully.
   *
   * @return the output wrapper, or null
   */
  public StringValue getOutput() {
    return this.output;
  }

  /**
   * Gets the failure details from the matching failure event, or null if the
   * child workflow has not failed.
   *
   * @return the failure details, or null
   */
  public TaskFailureDetails getError() {
    return this.error;
  }
}
