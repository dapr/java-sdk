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
 * A workflow schedule, which starts a workflow repeatedly on a cron schedule.
 *
 * <p>Each run is a new, detached workflow instance with the ID {@code <name>_<owner>_<fire time>}, where the owner is
 * a 6-character tag of the app that owns the schedule and the fire time is formatted as {@code yyyyMMdd'T'HHmmss'Z'}
 * in UTC. A run started by {@link DaprWorkflowClient#triggerWorkflowSchedule(String)} has the ID
 * {@code <name>_<owner>_<trigger time>_manual}, with the trigger time in milliseconds. So the runs of a schedule are
 * the instances whose ID starts with {@code <name>_<owner>_}, which
 * {@link DaprWorkflowClient#listInstanceIds(String, Integer, String)} can list.
 */
public final class WorkflowSchedule {

  private final WorkflowScheduleOuterClass.WorkflowSchedule schedule;

  WorkflowSchedule(WorkflowScheduleOuterClass.WorkflowSchedule schedule) {
    this.schedule = schedule;
  }

  /**
   * Gets the name of the schedule.
   *
   * @return the name of the schedule
   */
  public String getName() {
    return this.schedule.getName();
  }

  /**
   * Gets the name of the workflow each run starts.
   *
   * @return the workflow name
   */
  public String getWorkflowName() {
    return this.schedule.getWorkflowName();
  }

  /**
   * Gets when runs start, in the syntax of the Dapr Jobs API.
   *
   * @return the schedule expression
   */
  public String getSchedule() {
    return this.schedule.getSchedule();
  }

  /**
   * Gets the serialized input given to every run.
   *
   * @return the serialized input, or null when there is none
   */
  @Nullable
  public String getSerializedInput() {
    return this.schedule.hasInput() ? this.schedule.getInput().getValue() : null;
  }

  /**
   * Gets the time of the first run.
   *
   * @return the time of the first run, or null when none was set
   */
  @Nullable
  public Instant getDueTime() {
    return this.schedule.hasDueTime() ? DataConverter.getInstantFromTimestamp(this.schedule.getDueTime()) : null;
  }

  /**
   * Gets the time after which no run starts.
   *
   * @return the expiration time, or null when none was set
   */
  @Nullable
  public Instant getExpirationTime() {
    return this.schedule.hasExpirationTime()
        ? DataConverter.getInstantFromTimestamp(this.schedule.getExpirationTime()) : null;
  }

  /**
   * Gets the number of runs, scheduled or triggered, after which the schedule completes.
   *
   * @return the number of runs, or null when none was set
   */
  @Nullable
  public Integer getRepeats() {
    return this.schedule.hasRepeats() ? this.schedule.getRepeats() : null;
  }

  /**
   * Gets the ID of the app that hosts the workflow each run starts.
   *
   * @return the target app ID, or null when runs start on the app that owns the schedule
   */
  @Nullable
  public String getTargetAppId() {
    return this.schedule.hasTargetAppID() ? this.schedule.getTargetAppID() : null;
  }

  /**
   * Gets the namespace of the target app.
   *
   * @return the target app namespace, or null when none was set
   */
  @Nullable
  public String getTargetAppNamespace() {
    return this.schedule.hasTargetAppNamespace() ? this.schedule.getTargetAppNamespace() : null;
  }

  /**
   * Gets whether the schedule starts one run for all the times it missed while the app was down.
   *
   * <p>When false, the default, every missed time starts its own run, one after another.
   *
   * @return true if missed times start one run, false if each starts its own run
   */
  public boolean isSkipMissed() {
    return this.schedule.hasSkipMissed() && this.schedule.getSkipMissed();
  }

  /**
   * Gets how the schedule stands. The runtime fills it on get and list.
   *
   * @return the status of the schedule, or null only when the runtime returned none
   */
  @Nullable
  public WorkflowScheduleStatus getStatus() {
    return this.schedule.hasStatus() ? new WorkflowScheduleStatus(this.schedule.getStatus()) : null;
  }

}
