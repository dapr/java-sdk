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

/**
 * Represents the type of a workflow history event.
 */
public enum WorkflowHistoryEventType {
  /**
   * Unknown or unmapped event type.
   */
  UNKNOWN,

  /**
   * The workflow execution started.
   */
  EXECUTION_STARTED,

  /**
   * The workflow execution completed.
   */
  EXECUTION_COMPLETED,

  /**
   * The workflow execution was terminated.
   */
  EXECUTION_TERMINATED,

  /**
   * An activity task was scheduled.
   */
  TASK_SCHEDULED,

  /**
   * An activity task completed successfully.
   */
  TASK_COMPLETED,

  /**
   * An activity task failed.
   */
  TASK_FAILED,

  /**
   * A child workflow instance was created.
   */
  CHILD_WORKFLOW_INSTANCE_CREATED,

  /**
   * A child workflow instance completed.
   */
  CHILD_WORKFLOW_INSTANCE_COMPLETED,

  /**
   * A child workflow instance failed.
   */
  CHILD_WORKFLOW_INSTANCE_FAILED,

  /**
   * A timer was created.
   */
  TIMER_CREATED,

  /**
   * A timer fired.
   */
  TIMER_FIRED,

  /**
   * The workflow started processing a work item.
   */
  WORKFLOW_STARTED,

  /**
   * The workflow completed processing a work item.
   */
  WORKFLOW_COMPLETED,

  /**
   * An event was sent to another instance.
   */
  EVENT_SENT,

  /**
   * An external event was raised.
   */
  EVENT_RAISED,

  /**
   * The workflow continued as new.
   */
  CONTINUE_AS_NEW,

  /**
   * The workflow execution was suspended.
   */
  EXECUTION_SUSPENDED,

  /**
   * The workflow execution was resumed.
   */
  EXECUTION_RESUMED,

  /**
   * The workflow execution stalled.
   */
  EXECUTION_STALLED
}
