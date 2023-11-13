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

package io.dapr.workflows;

import com.microsoft.durabletask.CompositeTaskFailedException;
import com.microsoft.durabletask.Task;
import com.microsoft.durabletask.TaskCanceledException;
import com.microsoft.durabletask.TaskOptions;
import com.microsoft.durabletask.TaskOrchestrationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLogger;

import javax.annotation.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

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
   * @param logger  Logger
   * @throws IllegalArgumentException if context or logger is null
   */
  public DaprWorkflowContextImpl(TaskOrchestrationContext context, Logger logger) throws IllegalArgumentException {
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
   * Waits for an event to be raised named {@code name} and returns a {@link Task} that completes when the event is
   * received or is canceled when {@code timeout} expires.
   *
   * <p>See {@link #waitForExternalEvent(String, Duration, Class)} for a full description.
   *
   * @param name    the case-insensitive name of the event to wait for
   * @param timeout the amount of time to wait before canceling the returned {@code Task}
   * @return a new {@link Task} that completes when the external event is received or when {@code timeout} expires
   * @throws TaskCanceledException if the specified {@code timeout} value expires before the event is received
   */
  @Override
  public <V> Task<Void> waitForExternalEvent(String name, Duration timeout) throws TaskCanceledException {
    return this.innerContext.waitForExternalEvent(name, timeout, Void.class);
  }

  /**
   * Waits for an event to be raised named {@code name} and returns a {@link Task} that completes when the event is
   * received.
   *
   * <p>See {@link #waitForExternalEvent(String, Duration, Class)} for a full description.
   *
   * @param name the case-insensitive name of the event to wait for
   * @return a new {@link Task} that completes when the external event is received
   */
  @Override
  public <V> Task<Void> waitForExternalEvent(String name) throws TaskCanceledException {
    return this.innerContext.waitForExternalEvent(name, null, Void.class);
  }

  @Override
  public boolean isReplaying() {
    return this.innerContext.getIsReplaying();
  }

  /**
   * {@inheritDoc}
   */
  public <V> Task<V> callActivity(String name, Object input, TaskOptions options, Class<V> returnType) {
    return this.innerContext.callActivity(name, input, options, returnType);
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
  public <V> Task<V> callSubWorkflow(String name, @Nullable Object input, @Nullable String instanceID,
                                     @Nullable TaskOptions options, Class<V> returnType) {

    return this.innerContext.callSubOrchestrator(name, input, instanceID, options, returnType);
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
}
