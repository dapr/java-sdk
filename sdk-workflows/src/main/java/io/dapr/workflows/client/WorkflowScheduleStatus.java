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
import io.dapr.workflows.task.serialization.DataConverter;

import javax.annotation.Nullable;

import java.time.Instant;

/**
 * How a workflow schedule stands, as read when the schedule was fetched.
 */
public final class WorkflowScheduleStatus {

  private final WorkflowScheduleOuterClass.WorkflowScheduleStatus status;

  WorkflowScheduleStatus(WorkflowScheduleOuterClass.WorkflowScheduleStatus status) {
    this.status = status;
  }

  /**
   * Gets the state of the schedule.
   *
   * @return the state of the schedule
   * @throws IllegalArgumentException if the runtime returned a state this SDK does not know
   */
  public WorkflowScheduleState getState() {
    switch (this.status.getState()) {
      case WORKFLOW_SCHEDULE_STATE_ACTIVE:
        return WorkflowScheduleState.ACTIVE;
      case WORKFLOW_SCHEDULE_STATE_COMPLETED:
        return WorkflowScheduleState.COMPLETED;
      case WORKFLOW_SCHEDULE_STATE_FAILED:
        return WorkflowScheduleState.FAILED;
      default:
        throw new IllegalArgumentException("Unknown workflow schedule state: " + this.status.getState());
    }
  }

  /**
   * Gets when the schedule was created.
   *
   * @return the creation time
   */
  public Instant getCreatedAt() {
    return DataConverter.getInstantFromTimestamp(this.status.getCreatedAt());
  }

  /**
   * Gets when the next run starts, unless it is triggered sooner.
   *
   * @return the time of the next run, or null once the schedule is not active
   */
  @Nullable
  public Instant getNextRunTime() {
    return this.status.hasNextRunTime() ? DataConverter.getInstantFromTimestamp(this.status.getNextRunTime()) : null;
  }

  /**
   * Gets the time the last run was started for: its fire time, or when it was triggered.
   *
   * @return the time of the last run, or null before the first run
   */
  @Nullable
  public Instant getLastRunTime() {
    return this.status.hasLastRunTime() ? DataConverter.getInstantFromTimestamp(this.status.getLastRunTime()) : null;
  }

  /**
   * Gets the number of runs started so far.
   *
   * @return the number of runs
   */
  public int getRunCount() {
    return this.status.getRunCount();
  }

  /**
   * Gets how the last run stands, read from its workflow instance when the schedule was fetched.
   *
   * @return the last run, or null before the first run
   */
  @Nullable
  public WorkflowScheduleRun getLastRun() {
    return this.status.hasLastRun() ? new WorkflowScheduleRun(this.status.getLastRun()) : null;
  }

  /**
   * Gets why the schedule failed.
   *
   * @return the failure message when the state is {@link WorkflowScheduleState#FAILED}, otherwise null
   */
  @Nullable
  public String getFailureMessage() {
    return this.status.hasFailureMessage() ? this.status.getFailureMessage().getValue() : null;
  }

}
