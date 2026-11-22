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

package io.dapr.springboot.examples.simpleworkflow;

import io.dapr.workflows.client.WorkflowRuntimeStatus;

public class OnboardingStatus {
  private String instanceId;
  private WorkflowRuntimeStatus runtimeStatus;
  private OnboardingResult result;
  private String error;

  public OnboardingStatus() {
  }

  /**
   * Creates a new OnboardingStatus.
   *
   * @param instanceId    id of the workflow instance
   * @param runtimeStatus current runtime status of the workflow instance
   * @param result        output of the workflow, set once it completed
   * @param error         failure message, set if the workflow failed
   */
  public OnboardingStatus(String instanceId, WorkflowRuntimeStatus runtimeStatus, OnboardingResult result,
                          String error) {
    this.instanceId = instanceId;
    this.runtimeStatus = runtimeStatus;
    this.result = result;
    this.error = error;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }

  public WorkflowRuntimeStatus getRuntimeStatus() {
    return runtimeStatus;
  }

  public void setRuntimeStatus(WorkflowRuntimeStatus runtimeStatus) {
    this.runtimeStatus = runtimeStatus;
  }

  public OnboardingResult getResult() {
    return result;
  }

  public void setResult(OnboardingResult result) {
    this.result = result;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }
}
