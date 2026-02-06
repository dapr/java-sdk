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

package io.dapr.workflows.runtime;

import io.dapr.durabletask.TaskOrchestration;
import io.dapr.workflows.Workflow;

/**
 * Wrapper for Durable Task Framework orchestration factory.
 */
class WorkflowInstanceWrapper<T extends Workflow> extends WorkflowVersionWrapper {
  private final T workflow;
  private final String name;

  public WorkflowInstanceWrapper(T instance) {
    this.name = instance.getClass().getCanonicalName();
    this.workflow = instance;
  }

  public WorkflowInstanceWrapper(String name, T instance, String versionName, Boolean isLatestVersion) {
    super(versionName, isLatestVersion);
    this.name = name;
    this.workflow = instance;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public TaskOrchestration create() {
    return ctx -> workflow.run(new DefaultWorkflowContext(ctx, workflow.getClass()));
  }
}
