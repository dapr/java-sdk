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

/**
 * Exception that gets thrown when awaiting a {@link Task} for an activity or sub-orchestration that fails with an
 * unhandled exception.
 * <p>Detailed information associated with a particular task failure can be retrieved
 * using the {@link #getErrorDetails()} method.</p>
 */
public class TaskFailedException extends RuntimeException {
  private final FailureDetails details;
  private final String taskName;
  private final int taskId;

  TaskFailedException(String taskName, int taskId, FailureDetails details) {
    this(getExceptionMessage(taskName, taskId, details), taskName, taskId, details);
  }

  TaskFailedException(String message, String taskName, int taskId, FailureDetails details) {
    super(message);
    this.taskName = taskName;
    this.taskId = taskId;
    this.details = details;
  }

  /**
   * Gets the ID of the failed task.
   *
   * <p>Each durable task (activities, timers, sub-orchestrations, etc.) scheduled by a task orchestrator has an
   * auto-incrementing ID associated with it. This ID is used to distinguish tasks from one another, even if, for
   * example, they are tasks that call the same activity. This ID can therefore be used to more easily correlate a
   * specific task failure to a specific task.</p>
   *
   * @return the ID of the failed task
   */
  public int getTaskId() {
    return this.taskId;
  }

  /**
   * Gets the name of the failed task.
   *
   * @return the name of the failed task
   */
  public String getTaskName() {
    return this.taskName;
  }

  /**
   * Gets the details of the task failure, including exception information.
   *
   * @return the details of the task failure
   */
  public FailureDetails getErrorDetails() {
    return this.details;
  }

  private static String getExceptionMessage(String taskName, int taskId, FailureDetails details) {
    return String.format("Task '%s' (#%d) failed with an unhandled exception: %s",
        taskName,
        taskId,
        details.getErrorMessage());
  }
}
