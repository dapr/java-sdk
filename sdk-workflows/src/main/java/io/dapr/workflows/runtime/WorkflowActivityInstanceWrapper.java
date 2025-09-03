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

import io.dapr.durabletask.TaskActivity;
import io.dapr.durabletask.TaskActivityFactory;
import io.dapr.workflows.WorkflowActivity;

/**
 * Wrapper for Durable Task Framework task activity factory.
 */
public class WorkflowActivityInstanceWrapper<T extends WorkflowActivity> implements TaskActivityFactory {
  private final T activity;
  private final String name;

  /**
   * Constructor for WorkflowActivityWrapper.
   *
   * @param name     Name of the activity to wrap.
   * @param instance Instance of the activity to wrap.
   */
  public WorkflowActivityInstanceWrapper(String name, T instance) {
    this.name = name;
    this.activity = instance;
  }

  /**
   * Constructor for WorkflowActivityWrapper.
   *
   * @param instance Instance of the activity to wrap.
   */
  public WorkflowActivityInstanceWrapper(T instance) {
    this(instance.getClass().getCanonicalName(), instance);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public TaskActivity create() {
    return ctx -> activity.run(new DefaultWorkflowActivityContext(ctx, activity.getClass()));
  }
}
