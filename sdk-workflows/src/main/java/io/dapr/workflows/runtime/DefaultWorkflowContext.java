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

import io.dapr.durabletask.CompositeTaskFailedException;
import io.dapr.durabletask.RetryHandler;
import io.dapr.durabletask.RetryPolicy;
import io.dapr.durabletask.Task;
import io.dapr.durabletask.TaskCanceledException;
import io.dapr.durabletask.TaskOptions;
import io.dapr.durabletask.TaskOrchestrationContext;
import io.dapr.workflows.WorkflowContext;
import io.dapr.workflows.WorkflowTaskOptions;
import io.dapr.workflows.WorkflowTaskRetryContext;
import io.dapr.workflows.WorkflowTaskRetryHandler;
import io.dapr.workflows.WorkflowTaskRetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLogger;

import javax.annotation.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class DefaultWorkflowContext implements WorkflowContext {
  private final TaskOrchestrationContext innerContext;
  private final Logger logger;

  /**
   * Constructor for DaprWorkflowContextImpl.
   *
   * @param context TaskOrchestrationContext
   * @param clazz   Class to use for logger
   * @throws IllegalArgumentException if context is null
   */
  public DefaultWorkflowContext(TaskOrchestrationContext context, Class<?> clazz) throws IllegalArgumentException {
    this(context, LoggerFactory.getLogger(clazz));
  }

  /**
   * Constructor for DaprWorkflowContextImpl.
   *
   * @param context TaskOrchestrationContext
   * @param logger  Logger
   * @throws IllegalArgumentException if context or logger is null
   */
  public DefaultWorkflowContext(TaskOrchestrationContext context, Logger logger)
          throws IllegalArgumentException {
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
  @Override
  public <V> Task<V> waitForExternalEvent(String name, Duration timeout, Class<V> dataType)
          throws TaskCanceledException {
    return this.innerContext.waitForExternalEvent(name, timeout, dataType);
  }

  /**
   * Waits for an event to be raised named {@code name} and returns a {@link Task}
   * that completes when the event is
   * received or is canceled when {@code timeout} expires.
   *
   * <p>See {@link #waitForExternalEvent(String, Duration, Class)} for a full
   * description.
   *
   * @param name    the case-insensitive name of the event to wait for
   * @param timeout the amount of time to wait before canceling the returned
   *                {@code Task}
   * @return a new {@link Task} that completes when the external event is received
   *     or when {@code timeout} expires
   * @throws TaskCanceledException if the specified {@code timeout} value expires
   *                               before the event is received
   */
  @Override
  public Task<Void> waitForExternalEvent(String name, Duration timeout) throws TaskCanceledException {
    return this.innerContext.waitForExternalEvent(name, timeout, Void.class);
  }

  /**
   * Waits for an event to be raised named {@code name} and returns a {@link Task}
   * that completes when the event is
   * received.
   *
   * <p>See {@link #waitForExternalEvent(String, Duration, Class)} for a full
   * description.
   *
   * @param name the case-insensitive name of the event to wait for
   * @return a new {@link Task} that completes when the external event is received
   */
  @Override
  public Task<Void> waitForExternalEvent(String name) throws TaskCanceledException {
    return this.innerContext.waitForExternalEvent(name, null, Void.class);
  }

  @Override
  public boolean isReplaying() {
    return this.innerContext.getIsReplaying();
  }

  /**
   * {@inheritDoc}
   */
  public <V> Task<V> callActivity(String name, Object input, WorkflowTaskOptions options, Class<V> returnType) {
    TaskOptions taskOptions = toTaskOptions(options);

    return this.innerContext.callActivity(name, input, taskOptions, returnType);
  }

  /**
   * {@inheritDoc}
   */
  public <V> Task<List<V>> allOf(List<Task<V>> tasks) throws CompositeTaskFailedException {
    return this.innerContext.allOf(tasks);
  }

  /**
   * {@inheritDoc}
   */
  public Task<Task<?>> anyOf(List<Task<?>> tasks) {
    return this.innerContext.anyOf(tasks);
  }

  /**
   * {@inheritDoc}
   */
  public Task<Void> createTimer(Duration duration) {
    return this.innerContext.createTimer(duration);
  }

  @Override
  public Task<Void> createTimer(ZonedDateTime zonedDateTime) {
    return this.innerContext.createTimer(zonedDateTime);
  }

  /**
   * {@inheritDoc}
   */
  public <T> T getInput(Class<T> targetType) {
    return this.innerContext.getInput(targetType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <V> Task<V> callChildWorkflow(String name, @Nullable Object input, @Nullable String instanceID,
                                       @Nullable WorkflowTaskOptions options, Class<V> returnType) {
    TaskOptions taskOptions = toTaskOptions(options);

    return this.innerContext.callSubOrchestrator(name, input, instanceID, taskOptions, returnType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void continueAsNew(Object input) {
    this.innerContext.continueAsNew(input);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void continueAsNew(Object input, boolean preserveUnprocessedEvents) {
    this.innerContext.continueAsNew(input, preserveUnprocessedEvents);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UUID newUuid() {
    return this.innerContext.newUUID();
  }

  private TaskOptions toTaskOptions(WorkflowTaskOptions options) {
    if (options == null) {
      return null;
    }

    RetryPolicy retryPolicy = toRetryPolicy(options.getRetryPolicy());
    RetryHandler retryHandler = toRetryHandler(options.getRetryHandler());

    return TaskOptions.builder()
            .retryPolicy(retryPolicy)
            .retryHandler(retryHandler)
            .appID(options.getAppId())
            .build();
  }

  /**
   * Converts a {@link WorkflowTaskRetryPolicy} to a {@link RetryPolicy}.
   *
   * @param workflowTaskRetryPolicy The {@link WorkflowTaskRetryPolicy} being converted
   * @return A {@link RetryPolicy}
   */
  private RetryPolicy toRetryPolicy(WorkflowTaskRetryPolicy workflowTaskRetryPolicy) {
    if (workflowTaskRetryPolicy == null) {
      return null;
    }

    RetryPolicy retryPolicy = new RetryPolicy(
            workflowTaskRetryPolicy.getMaxNumberOfAttempts(),
            workflowTaskRetryPolicy.getFirstRetryInterval()
    );

    retryPolicy.setBackoffCoefficient(workflowTaskRetryPolicy.getBackoffCoefficient());
    if (workflowTaskRetryPolicy.getRetryTimeout() != null) {
      retryPolicy.setRetryTimeout(workflowTaskRetryPolicy.getRetryTimeout());
    }

    return retryPolicy;
  }

  /**
   * Converts a {@link WorkflowTaskRetryHandler} to a {@link RetryHandler}.
   *
   * @param workflowTaskRetryHandler The {@link WorkflowTaskRetryHandler} being converted
   * @return A {@link RetryHandler}
   */
  private RetryHandler toRetryHandler(WorkflowTaskRetryHandler workflowTaskRetryHandler) {
    if (workflowTaskRetryHandler == null) {
      return null;
    }

    return retryContext -> {
      WorkflowTaskRetryContext workflowRetryContext = new WorkflowTaskRetryContext(
              this,
              retryContext.getLastAttemptNumber(),
              new DefaultWorkflowFailureDetails(retryContext.getLastFailure()),
              retryContext.getTotalRetryTime()
      );

      return workflowTaskRetryHandler.handle(workflowRetryContext);
    };
  }

  /**
   * Set custom status to a workflow execution.
   *
   * @param status to set to the execution
   */
  public void setCustomStatus(Object status) {
    innerContext.setCustomStatus(status);
  }
}
