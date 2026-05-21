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
 * Holds the status and data of a named activity invocation observed in
 * propagated workflow history. {@code completed} or {@code failed} reflect
 * the terminal state of the scheduling.
 */
public final class ActivityResult {
  private final String name;
  private final boolean completed;
  private final boolean failed;
  private final StringValue input;
  private final StringValue output;
  private final TaskFailureDetails error;

  ActivityResult(String name,
                 boolean completed,
                 boolean failed,
                 StringValue input,
                 StringValue output,
                 TaskFailureDetails error) {
    this.name = name;
    this.completed = completed;
    this.failed = failed;
    this.input = input;
    this.output = output;
    this.error = error;
  }

  /**
   * Gets the activity name.
   *
   * @return the activity name
   */
  public String getName() {
    return this.name;
  }

  /**
   * Returns true if the activity completed successfully.
   *
   * @return whether the activity completed
   */
  public boolean isCompleted() {
    return this.completed;
  }

  /**
   * Returns true if the activity failed.
   *
   * @return whether the activity failed
   */
  public boolean isFailed() {
    return this.failed;
  }

  /**
   * Gets the activity input as recorded in the scheduling event, or null if
   * no input was provided.
   *
   * @return the input wrapper, or null
   */
  public StringValue getInput() {
    return this.input;
  }

  /**
   * Gets the activity output from the matching completion event, or null if
   * the activity has not completed successfully.
   *
   * @return the output wrapper, or null
   */
  public StringValue getOutput() {
    return this.output;
  }

  /**
   * Gets the failure details from the matching failure event, or null if the
   * activity has not failed.
   *
   * @return the failure details, or null
   */
  public TaskFailureDetails getError() {
    return this.error;
  }
}
