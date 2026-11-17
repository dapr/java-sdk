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

package io.dapr.workflows.client;

/**
 * Enum describing the state of a workflow schedule.
 */
public enum WorkflowScheduleState {
  /**
   * The schedule is waiting for its next run.
   */
  ACTIVE,

  /**
   * The schedule has no runs left, because of its repeats or expiration time.
   */
  COMPLETED,

  /**
   * The schedule stopped with an error, given by {@link WorkflowScheduleStatus#getFailureMessage()}.
   */
  FAILED

}
