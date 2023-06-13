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

import com.microsoft.durabletask.Task;
import com.microsoft.durabletask.TaskOrchestrationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLogger;
import java.time.Duration;
import java.time.Instant;

/**
 * Dapr workflow context implementation.
 */
public class DaprWorkflowContextImpl implements WorkflowContext {
  private final TaskOrchestrationContext innerContext;
  private final Logger logger;

  /**
   * Constructor for DaprWorkflowContextImpl.
   *
   * @param context TaskOrchestrationContext
   * @throws IllegalArgumentException if context is null
   */
  public DaprWorkflowContextImpl(TaskOrchestrationContext context) throws IllegalArgumentException {
    this(context, LoggerFactory.getLogger(WorkflowContext.class));
  }

  /**
   * Constructor for DaprWorkflowContextImpl.
   *
   * @param context TaskOrchestrationContext
   * @param logger Logger
   * @throws IllegalArgumentException if context or logger is null
   */
  public DaprWorkflowContextImpl(TaskOrchestrationContext context, Logger logger) throws IllegalArgumentException {
    if (context == null) {
      throw new IllegalArgumentException("Context cannot be null");
    } else {
      this.innerContext = context;
    }

    if (logger == null) {
      throw new IllegalArgumentException("Logger cannot be null");
    } else {
      this.logger = logger;
    }
  }

  /**
   * {@inheritDoc}
   */
  public Logger getLogger() {
    if (this.innerContext.getIsReplaying()) {
      return NOPLogger.NOP_LOGGER;
    }
    return this.logger;
  }

  /**
   * {@inheritDoc}
   */
  public String getName() {
    return this.innerContext.getName();
  }

  /**
   * {@inheritDoc}
   */
  public String getInstanceId() {
    return this.innerContext.getInstanceId();
  }

  /**
   * {@inheritDoc}
   */
  public Instant getCurrentInstant() {
    return this.innerContext.getCurrentInstant();
  }

  /**
   * {@inheritDoc}
   */
  public void complete(Object output) {
    this.innerContext.complete(output);
  }

  /**
   * {@inheritDoc}
   */
  public Task<Void> waitForExternalEvent(String eventName, Duration timeout) {
    return this.innerContext.waitForExternalEvent(eventName, timeout);
  }

  @Override
  public boolean getIsReplaying() {
    return this.innerContext.getIsReplaying();
  }
}
