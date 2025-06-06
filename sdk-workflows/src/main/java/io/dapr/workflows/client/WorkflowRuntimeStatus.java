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
 * Enum describing the runtime status of a workflow.
 */
public enum WorkflowRuntimeStatus {
  /**
   * The workflow started running.
   */
  RUNNING,

  /**
   * The workflow completed normally.
   */
  COMPLETED,

  /**
   * The workflow is continued as new.
   */
  CONTINUED_AS_NEW,

  /**
   * The workflow completed with an unhandled exception.
   */
  FAILED,

  /**
   * The workflow was abruptly cancelled via a management API call.
   */
  CANCELED,

  /**
   * The workflow was abruptly terminated via a management API call.
   */
  TERMINATED,

  /**
   * The workflow was scheduled but hasn't started running.
   */
  PENDING,

  /**
   * The workflow was suspended.
   */
  SUSPENDED

}
