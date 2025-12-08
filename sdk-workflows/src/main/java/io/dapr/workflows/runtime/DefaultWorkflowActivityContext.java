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

import io.dapr.durabletask.TaskActivityContext;
import io.dapr.workflows.WorkflowActivityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper for Durable Task Framework {@link TaskActivityContext}.
 */
class DefaultWorkflowActivityContext implements WorkflowActivityContext {
  private final TaskActivityContext innerContext;
  private final Logger logger;

  /**
   * Constructor for WorkflowActivityContext.
   *
   * @param context TaskActivityContext
   * @throws IllegalArgumentException if context is null
   */
  public DefaultWorkflowActivityContext(TaskActivityContext context) throws IllegalArgumentException {
    this(context, WorkflowActivityContext.class);
  }

  /**
   * Constructor for WorkflowActivityContext.
   *
   * @param context TaskActivityContext
   * @param clazz   Class to use for logger
   * @throws IllegalArgumentException if context is null
   */
  public DefaultWorkflowActivityContext(TaskActivityContext context, Class<?> clazz) throws IllegalArgumentException {
    this(context, LoggerFactory.getLogger(clazz));
  }

  /**
   * Constructor for WorkflowActivityContext.
   *
   * @param context TaskActivityContext
   * @throws IllegalArgumentException if context is null
   */
  public DefaultWorkflowActivityContext(TaskActivityContext context, Logger logger) throws IllegalArgumentException {
    if (context == null) {
      throw new IllegalArgumentException("Context cannot be null");
    }

    if (logger == null) {
      throw new IllegalArgumentException("Logger cannot be null");
    }

    this.innerContext = context;
    this.logger = logger;
  }

  /**
   * Gets the logger for the current activity.
   *
   * @return the logger for the current activity
   */
  @Override
  public Logger getLogger() {
    return this.logger;
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

  @Override
  public String getTaskExecutionId() {
    return this.innerContext.getTaskExecutionId();
  }
}
