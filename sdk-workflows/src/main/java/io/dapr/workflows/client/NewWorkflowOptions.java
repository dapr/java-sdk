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

import java.time.Instant;

/**
 * Options for starting a new instance of a workflow.
 */
public class NewWorkflowOptions {

  private String version;
  private String instanceId;
  private Object input;
  private Instant startTime;
  private boolean enforceUniqueInstanceId;

  /**
   * Sets the version of the workflow to start.
   *
   * @param version the user-defined version of workflow
   * @return this {@link NewWorkflowOptions} object
   */
  public NewWorkflowOptions setVersion(String version) {
    this.version = version;
    return this;
  }

  /**
   * Sets the instance ID of the workflow to start.
   *
   * <p>If no instance ID is configured, the workflow will be created with a randomly generated instance ID.
   *
   * @param instanceId the ID of the new workflow
   * @return this {@link NewWorkflowOptions} object
   */
  public NewWorkflowOptions setInstanceId(String instanceId) {
    this.instanceId = instanceId;
    return this;
  }

  /**
   * Sets the input of the workflow to start.
   *
   * @param input the input of the new workflow
   * @return this {@link NewWorkflowOptions} object
   */
  public NewWorkflowOptions setInput(Object input) {
    this.input = input;
    return this;
  }

  /**
   * Sets the start time of the new workflow.
   *
   * <p>By default, new workflow instances start executing immediately. This
   * method can be used to start them at a specific time in the future. If set,
   * Dapr will not wait for the workflow to "start" which can improve
   * throughput of creating many workflows.
   *
   * @param startTime the start time of the new workflow
   * @return this {@link NewWorkflowOptions} object
   */
  public NewWorkflowOptions setStartTime(Instant startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * Sets whether the instance ID of the new workflow must be unique.
   *
   * <p>When enabled, scheduling fails with a {@link WorkflowInstanceAlreadyExistsException}
   * if a workflow instance with the same ID already exists, regardless of whether that
   * instance is still running or has already completed. By default, scheduling a workflow
   * with the instance ID of a completed instance restarts that instance.
   *
   * <p>Requires a Dapr runtime that supports this option.
   *
   * @param enforceUniqueInstanceId whether to reject instance IDs that already exist
   * @return this {@link NewWorkflowOptions} object
   */
  public NewWorkflowOptions setEnforceUniqueInstanceId(boolean enforceUniqueInstanceId) {
    this.enforceUniqueInstanceId = enforceUniqueInstanceId;
    return this;
  }

  /**
   * Gets the user-specified version of the new workflow.
   *
   * @return the user-specified version of the new workflow.
   */
  public String getVersion() {
    return this.version;
  }

  /**
   * Gets the instance ID of the new workflow.
   *
   * @return the instance ID of the new workflow.
   */
  public String getInstanceId() {
    return this.instanceId;
  }

  /**
   * Gets the input of the new workflow.
   *
   * @return the input of the new workflow.
   */
  public Object getInput() {
    return this.input;
  }

  /**
   * Gets the configured start time of the new workflow instance.
   *
   * @return the configured start time of the new workflow instance.
   */
  public Instant getStartTime() {
    return this.startTime;
  }

  /**
   * Gets whether the instance ID of the new workflow must be unique.
   *
   * @return true if instance IDs that already exist are rejected, false otherwise
   */
  public boolean isEnforceUniqueInstanceId() {
    return this.enforceUniqueInstanceId;
  }

}
