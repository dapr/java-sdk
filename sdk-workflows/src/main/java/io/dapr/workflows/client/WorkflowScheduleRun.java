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

import io.dapr.durabletask.implementation.protobuf.WorkflowScheduleOuterClass;
import io.dapr.workflows.task.exception.WorkflowFailureDetails;
import io.dapr.workflows.task.serialization.DataConverter;

import javax.annotation.Nullable;

import java.time.Instant;

/**
 * One run of a workflow schedule.
 *
 * <p>Only the instance ID is set when the run's workflow instance could not be read, for example because it was
 * purged. Use {@link DaprWorkflowClient#getWorkflowState(String, boolean)} with the instance ID for the full state.
 */
public final class WorkflowScheduleRun {

  private final WorkflowScheduleOuterClass.WorkflowScheduleRun run;

  WorkflowScheduleRun(WorkflowScheduleOuterClass.WorkflowScheduleRun run) {
    this.run = run;
  }

  /**
   * Gets the instance ID of the run's workflow.
   *
   * @return the instance ID
   */
  public String getInstanceId() {
    return this.run.getInstanceId();
  }

  /**
   * Gets the runtime status of the run's workflow.
   *
   * @return the runtime status, or null when the instance could not be read
   */
  @Nullable
  public WorkflowRuntimeStatus getRuntimeStatus() {
    if (!this.run.hasRuntimeStatus()) {
      return null;
    }
    return WorkflowRuntimeStatus.fromProtobuf(this.run.getRuntimeStatus());
  }

  /**
   * Gets when the run's workflow instance was created.
   *
   * @return the creation time, or null when the instance could not be read
   */
  @Nullable
  public Instant getCreatedAt() {
    return this.run.hasCreatedTimestamp() ? DataConverter.getInstantFromTimestamp(this.run.getCreatedTimestamp())
        : null;
  }

  /**
   * Gets when the run's workflow instance was last updated.
   *
   * @return the last updated time, or null when the instance could not be read
   */
  @Nullable
  public Instant getLastUpdatedAt() {
    return this.run.hasLastUpdatedTimestamp()
        ? DataConverter.getInstantFromTimestamp(this.run.getLastUpdatedTimestamp()) : null;
  }

  /**
   * Gets the serialized output of the run's workflow.
   *
   * @return the serialized output, or null when there is none
   */
  @Nullable
  public String getSerializedOutput() {
    return this.run.hasOutput() ? this.run.getOutput().getValue() : null;
  }

  /**
   * Gets the failure details of the run's workflow.
   *
   * @return the failure details, or null when the run did not fail
   */
  @Nullable
  public WorkflowFailureDetails getFailureDetails() {
    return this.run.hasFailureDetails() ? new WorkflowFailureDetails(this.run.getFailureDetails()) : null;
  }

}
