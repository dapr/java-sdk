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

import java.time.Instant;

/**
 * Options for starting a new instance of an orchestration.
 */
public final class NewOrchestrationInstanceOptions {
  private String version;
  private String instanceId;
  private Object input;
  private Instant startTime;
  private String appID; // Target app ID for cross-app workflow routing

  /**
   * Default constructor for the {@link NewOrchestrationInstanceOptions} class.
   */
  public NewOrchestrationInstanceOptions() {
  }

  /**
   * Sets the version of the orchestration to start.
   *
   * @param version the user-defined version of the orchestration
   * @return this {@link NewOrchestrationInstanceOptions} object
   */
  public NewOrchestrationInstanceOptions setVersion(String version) {
    this.version = version;
    return this;
  }

  /**
   * Sets the instance ID of the orchestration to start.
   * If no instance ID is configured, the orchestration will be created with a randomly generated instance ID.
   *
   * @param instanceId the ID of the new orchestration instance
   * @return this {@link NewOrchestrationInstanceOptions} object
   */
  public NewOrchestrationInstanceOptions setInstanceId(String instanceId) {
    this.instanceId = instanceId;
    return this;
  }

  /**
   * Sets the input of the orchestration to start.
   * There are no restrictions on the type of inputs that can be used except that they must be serializable using
   * the {@link DataConverter} that was configured for the {@link DurableTaskClient} at creation time.
   *
   * @param input the input of the new orchestration instance
   * @return this {@link NewOrchestrationInstanceOptions} object
   */
  public NewOrchestrationInstanceOptions setInput(Object input) {
    this.input = input;
    return this;
  }

  /**
   * Sets the start time of the new orchestration instance.
   * By default, new orchestration instances start executing immediately. This method can be used
   * to start them at a specific time in the future.
   *
   * @param startTime the start time of the new orchestration instance
   * @return this {@link NewOrchestrationInstanceOptions} object
   */
  public NewOrchestrationInstanceOptions setStartTime(Instant startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * Sets the target app ID for cross-app workflow routing.
   *
   * @param appID the target app ID for cross-app routing
   * @return this {@link NewOrchestrationInstanceOptions} object
   */
  public NewOrchestrationInstanceOptions setAppID(String appID) {
    this.appID = appID;
    return this;
  }

  /**
   * Gets the user-specified version of the new orchestration.
   *
   * @return the user-specified version of the new orchestration.
   */
  public String getVersion() {
    return this.version;
  }

  /**
   * Gets the instance ID of the new orchestration.
   *
   * @return the instance ID of the new orchestration.
   */
  public String getInstanceId() {
    return this.instanceId;
  }

  /**
   * Gets the input of the new orchestration.
   *
   * @return the input of the new orchestration.
   */
  public Object getInput() {
    return this.input;
  }

  /**
   * Gets the configured start time of the new orchestration instance.
   *
   * @return the configured start time of the new orchestration instance.
   */
  public Instant getStartTime() {
    return this.startTime;
  }

  /**
   * Gets the configured target app ID for cross-app workflow routing.
   *
   * @return the configured target app ID
   */
  public String getAppID() {
    return this.appID;
  }

  /**
   * Checks if an app ID is configured for cross-app routing.
   *
   * @return true if an app ID is configured, false otherwise
   */
  public boolean hasAppID() {
    return this.appID != null && !this.appID.isEmpty();
  }
}
