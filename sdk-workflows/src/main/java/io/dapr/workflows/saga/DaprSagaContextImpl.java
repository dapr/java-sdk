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

package io.dapr.workflows.saga;

import io.dapr.workflows.WorkflowContext;

/**
 * Dapr Saga Context implementation.
 */
public class DaprSagaContextImpl implements SagaContext {

  private final Saga saga;
  private final WorkflowContext workflowContext;

  /**
   * Constructor to build up instance.
   * 
   * @param saga Saga instance.
   * @param workflowContext Workflow context.
   * @throws IllegalArgumentException if saga or workflowContext is null.
   */
  public DaprSagaContextImpl(Saga saga, WorkflowContext workflowContext) {
    if (saga == null) {
      throw new IllegalArgumentException("Saga should not be null");
    }
    if (workflowContext == null) {
      throw new IllegalArgumentException("workflowContext should not be null");
    }

    this.saga = saga;
    this.workflowContext = workflowContext;
  }

  @Override
  public void registerCompensation(String activityClassName, Object activityInput) {
    this.saga.registerCompensation(activityClassName, activityInput);
  }

  @Override
  public void compensate() {
    this.saga.compensate(workflowContext);
  }
}
