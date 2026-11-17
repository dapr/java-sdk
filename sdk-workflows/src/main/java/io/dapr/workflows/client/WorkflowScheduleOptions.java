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

import java.time.Instant;

/**
 * Options for creating a workflow schedule.
 */
public class WorkflowScheduleOptions {

  private Object input;
  private Instant dueTime;
  private Instant expirationTime;
  private Integer repeats;
  private String targetAppId;
  private String targetAppNamespace;
  private boolean overwrite;
  private boolean skipMissed;

  /**
   * Sets the input given to every run of the schedule.
   *
   * @param input the input of every run. Must be serializable.
   * @return this {@link WorkflowScheduleOptions} object
   */
  public WorkflowScheduleOptions setInput(Object input) {
    this.input = input;
    return this;
  }

  /**
   * Sets the time of the first run.
   *
   * <p>By default, the first run is at the first time the schedule matches after it is created.
   *
   * @param dueTime the time of the first run
   * @return this {@link WorkflowScheduleOptions} object
   */
  public WorkflowScheduleOptions setDueTime(Instant dueTime) {
    this.dueTime = dueTime;
    return this;
  }

  /**
   * Sets the time after which no run starts. The schedule completes once its next run would be after it.
   *
   * @param expirationTime the time after which no run starts
   * @return this {@link WorkflowScheduleOptions} object
   */
  public WorkflowScheduleOptions setExpirationTime(Instant expirationTime) {
    this.expirationTime = expirationTime;
    return this;
  }

  /**
   * Sets the number of runs, scheduled or triggered, after which the schedule completes.
   *
   * @param repeats the number of runs, at least 1
   * @return this {@link WorkflowScheduleOptions} object
   * @throws IllegalArgumentException if repeats is less than 1
   */
  public WorkflowScheduleOptions setRepeats(int repeats) {
    if (repeats < 1) {
      throw new IllegalArgumentException("repeats must be at least 1.");
    }
    this.repeats = repeats;
    return this;
  }

  /**
   * Sets the ID of the app that hosts the workflow each run starts.
   *
   * <p>By default, runs start on the app that owns the schedule.
   *
   * @param targetAppId the ID of the app that hosts the workflow
   * @return this {@link WorkflowScheduleOptions} object
   */
  public WorkflowScheduleOptions setTargetAppId(String targetAppId) {
    this.targetAppId = targetAppId;
    return this;
  }

  /**
   * Sets the namespace of the target app. Requires a target app ID.
   *
   * @param targetAppNamespace the namespace of the target app
   * @return this {@link WorkflowScheduleOptions} object
   */
  public WorkflowScheduleOptions setTargetAppNamespace(String targetAppNamespace) {
    this.targetAppNamespace = targetAppNamespace;
    return this;
  }

  /**
   * Sets whether to replace an existing schedule of the same name.
   *
   * <p>Runs the replaced schedule already started are not affected. Without it, creating a schedule whose name is
   * taken fails with the gRPC status ALREADY_EXISTS.
   *
   * @param overwrite whether to replace an existing schedule of the same name
   * @return this {@link WorkflowScheduleOptions} object
   */
  public WorkflowScheduleOptions setOverwrite(boolean overwrite) {
    this.overwrite = overwrite;
    return this;
  }

  /**
   * Sets whether the schedule starts one run, rather than one run each, for the times it missed while the app was
   * down.
   *
   * <p>By default, a schedule catches up durably: every time it missed starts its own run, one after another, as
   * with Dapr jobs and actor reminders. When set, it starts one run for the missed times and carries on from the
   * next time after now.
   *
   * @param skipMissed whether to start one run for all missed times
   * @return this {@link WorkflowScheduleOptions} object
   */
  public WorkflowScheduleOptions setSkipMissed(boolean skipMissed) {
    this.skipMissed = skipMissed;
    return this;
  }

  /**
   * Gets the input given to every run.
   *
   * @return the input, or null when none was set
   */
  public Object getInput() {
    return this.input;
  }

  /**
   * Gets the time of the first run.
   *
   * @return the time of the first run, or null when none was set
   */
  public Instant getDueTime() {
    return this.dueTime;
  }

  /**
   * Gets the time after which no run starts.
   *
   * @return the expiration time, or null when none was set
   */
  public Instant getExpirationTime() {
    return this.expirationTime;
  }

  /**
   * Gets the number of runs, scheduled or triggered, after which the schedule completes.
   *
   * @return the number of runs, or null when none was set
   */
  public Integer getRepeats() {
    return this.repeats;
  }

  /**
   * Gets the ID of the app that hosts the workflow each run starts.
   *
   * @return the target app ID, or null when none was set
   */
  public String getTargetAppId() {
    return this.targetAppId;
  }

  /**
   * Gets the namespace of the target app.
   *
   * @return the target app namespace, or null when none was set
   */
  public String getTargetAppNamespace() {
    return this.targetAppNamespace;
  }

  /**
   * Gets whether to replace an existing schedule of the same name.
   *
   * @return true if an existing schedule is replaced, false otherwise
   */
  public boolean isOverwrite() {
    return this.overwrite;
  }

  /**
   * Gets whether the schedule starts one run for all the times it missed while the app was down.
   *
   * @return true if missed times start one run, false if each starts its own run
   */
  public boolean isSkipMissed() {
    return this.skipMissed;
  }

}
