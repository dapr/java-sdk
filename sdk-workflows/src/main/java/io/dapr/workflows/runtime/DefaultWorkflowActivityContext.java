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

import com.microsoft.durabletask.TaskActivityContext;
import io.dapr.workflows.WorkflowActivityContext;

/**
 * Wrapper for Durable Task Framework {@link TaskActivityContext}.
 */
class DefaultWorkflowActivityContext implements WorkflowActivityContext {
  private final TaskActivityContext innerContext;

  /**
   * Constructor for WorkflowActivityContext.
   *
   * @param context TaskActivityContext
   * @throws IllegalArgumentException if context is null
   */
  public DefaultWorkflowActivityContext(TaskActivityContext context) throws IllegalArgumentException {
    if (context == null) {
      throw new IllegalArgumentException("Context cannot be null");
    }
    this.innerContext = context;
  }

  /**
   * Gets the name of the current activity.
   *
   * @return the name of the current activity
   */
  @Override
  public String getName() {
    return this.innerContext.getName();
  }

  /**
   * Gets the input of the current activity.
   *
   * @param <T>        the type of the input
   * @param targetType targetType of the input
   * @return the input of the current activity
   */
  @Override
  public <T> T getInput(Class<T> targetType) {
    return this.innerContext.getInput(targetType);
  }
}
