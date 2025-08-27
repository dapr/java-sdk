/*
 * Copyright 2024 The Dapr Authors
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

import io.dapr.config.Properties;
import io.dapr.durabletask.DurableTaskGrpcWorkerBuilder;
import io.dapr.utils.NetworkUtils;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.internal.ApiTokenClientInterceptor;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkflowRuntimeBuilder {
  private ClientInterceptor workflowApiTokenInterceptor;
  private static volatile WorkflowRuntime instance;
  private final Logger logger;
  private final Set<String> workflows = new HashSet<>();
  private final Set<String> activities = new HashSet<>();
  private final Set<String> activitySet = Collections.synchronizedSet(new HashSet<>());
  private final Set<String> workflowSet = Collections.synchronizedSet(new HashSet<>());
  private final DurableTaskGrpcWorkerBuilder builder;
  private final ManagedChannel managedChannel;
  private ExecutorService executorService;

  /**
   * Constructs the WorkflowRuntimeBuilder.
   */
  public WorkflowRuntimeBuilder() {
    this(new Properties());
  }

  /**
   * Constructs the WorkflowRuntimeBuilder.
   *
   * @param properties Properties to use.
   */
  public WorkflowRuntimeBuilder(Properties properties) {
    this(properties, LoggerFactory.getLogger(WorkflowRuntimeBuilder.class));
  }

  public WorkflowRuntimeBuilder(Logger logger) {
    this(new Properties(), logger);
  }

  private WorkflowRuntimeBuilder(Properties properties, Logger logger) {
    this.workflowApiTokenInterceptor = new ApiTokenClientInterceptor(properties);
    this.managedChannel = NetworkUtils.buildGrpcManagedChannel(properties, workflowApiTokenInterceptor);
    this.builder = new DurableTaskGrpcWorkerBuilder().grpcChannel(this.managedChannel);
    this.logger = logger;
  }

  /**
   * Returns a WorkflowRuntime object.
   *
   * @return A WorkflowRuntime object.
   */
  public WorkflowRuntime build() {
    if (instance == null) {
      synchronized (WorkflowRuntime.class) {
        this.executorService = this.executorService == null ? Executors.newCachedThreadPool() : this.executorService;
        if (instance == null) {
          instance = new WorkflowRuntime(
                  this.builder.withExecutorService(this.executorService).build(),
                  this.managedChannel, this.executorService);
        }
      }
    }

    this.logger.info("List of registered workflows: {}", this.workflowSet);
    this.logger.info("List of registered activities: {}", this.activitySet);
    this.logger.info("Successfully built dapr workflow runtime");

    return instance;
  }

  /**
   * Register Executor Service to use with workflow.
   *
   * @param executorService to be used.
   * @return {@link WorkflowRuntimeBuilder}.
   */
  public WorkflowRuntimeBuilder withExecutorService(ExecutorService executorService) {
    this.executorService = executorService;
    this.builder.withExecutorService(executorService);
    return this;
  }

  /**
   * Registers a Workflow object.
   *
   * @param <T>   any Workflow type
   * @param clazz the class being registered
   * @return the WorkflowRuntimeBuilder
   */
  public <T extends Workflow> WorkflowRuntimeBuilder registerWorkflow(Class<T> clazz) {
    this.builder.addOrchestration(new WorkflowClassWrapper<>(clazz));
    this.workflowSet.add(clazz.getCanonicalName());
    this.workflows.add(clazz.getSimpleName());

    this.logger.info("Registered Workflow: {}", clazz.getSimpleName());

    return this;
  }

  /**
   * Registers a Workflow object.
   *
   * @param <T>   any Workflow type
   * @param instance the workflow instance being registered
   * @return the WorkflowRuntimeBuilder
   */
  public <T extends Workflow> WorkflowRuntimeBuilder registerWorkflow(T instance) {
    Class<T> clazz = (Class<T>) instance.getClass();

    this.builder.addOrchestration(new WorkflowInstanceWrapper<>(instance));
    this.workflowSet.add(clazz.getCanonicalName());
    this.workflows.add(clazz.getSimpleName());

    this.logger.info("Registered Workflow: {}", clazz.getSimpleName());

    return this;
  }

  /**
   * Registers an Activity object.
   *
   * @param clazz the class being registered
   * @param <T>   any WorkflowActivity type
   * @return the WorkflowRuntimeBuilder
   */
  public <T extends WorkflowActivity> WorkflowRuntimeBuilder registerActivity(Class<T> clazz) {
    return registerActivity(clazz.getCanonicalName(), clazz);
  }

  /**
   * Registers an Activity object.
   *
   * @param <T>   any WorkflowActivity type
   * @param name Name of the activity to register.
   * @param clazz Class of the activity to register.
   * @return the WorkflowRuntimeBuilder
   */
  public <T extends WorkflowActivity> WorkflowRuntimeBuilder registerActivity(String name, Class<T> clazz) {
    this.builder.addActivity(new WorkflowActivityClassWrapper<>(name, clazz));
    this.activitySet.add(name);
    this.activities.add(name);

    this.logger.info("Registered Activity: {}", clazz.getSimpleName());

    return this;
  }

  /**
   * Registers an Activity object.
   *
   * @param <T>   any WorkflowActivity type
   * @param instance the class instance being registered
   * @return the WorkflowRuntimeBuilder
   */
  public <T extends WorkflowActivity> WorkflowRuntimeBuilder registerActivity(T instance) {
    return this.registerActivity(instance.getClass().getCanonicalName(), instance);
  }

  /**
   * Registers an Activity object.
   *
   * @param <T>   any WorkflowActivity type
   * @param name Name of the activity to register.
   * @param instance the class instance being registered
   * @return the WorkflowRuntimeBuilder
   */
  public <T extends WorkflowActivity> WorkflowRuntimeBuilder registerActivity(String name, T instance) {
    this.builder.addActivity(new WorkflowActivityInstanceWrapper<>(name, instance));
    this.activitySet.add(name);
    this.activities.add(name);

    this.logger.info("Registered Activity: {}", name);

    return this;
  }

}
