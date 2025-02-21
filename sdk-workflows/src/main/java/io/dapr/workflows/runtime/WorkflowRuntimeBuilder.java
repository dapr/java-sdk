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

import com.microsoft.durabletask.DurableTaskGrpcWorkerBuilder;
import io.dapr.config.Properties;
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

public class WorkflowRuntimeBuilder {
  private static final ClientInterceptor WORKFLOW_INTERCEPTOR = new ApiTokenClientInterceptor();
  private static volatile WorkflowRuntime instance;
  private final Logger logger;
  private final Set<String> workflows = new HashSet<>();
  private final Set<String> activities = new HashSet<>();
  private final Set<String> activitySet = Collections.synchronizedSet(new HashSet<>());
  private final Set<String> workflowSet = Collections.synchronizedSet(new HashSet<>());
  private final DurableTaskGrpcWorkerBuilder builder;

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
    ManagedChannel managedChannel = NetworkUtils.buildGrpcManagedChannel(properties, WORKFLOW_INTERCEPTOR);
    this.builder = new DurableTaskGrpcWorkerBuilder().grpcChannel(managedChannel);
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
        if (instance == null) {
          instance = new WorkflowRuntime(this.builder.build());
        }
      }
    }

    this.logger.info("List of registered workflows: {}", this.workflowSet);
    this.logger.info("List of registered activities: {}", this.activitySet);
    this.logger.info("Successfully built dapr workflow runtime");

    return instance;
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
    this.builder.addActivity(new WorkflowActivityClassWrapper<>(clazz));
    this.activitySet.add(clazz.getCanonicalName());
    this.activities.add(clazz.getSimpleName());

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
    Class<T> clazz = (Class<T>) instance.getClass();

    this.builder.addActivity(new WorkflowActivityInstanceWrapper<>(instance));
    this.activitySet.add(clazz.getCanonicalName());
    this.activities.add(clazz.getSimpleName());

    this.logger.info("Registered Activity: {}", clazz.getSimpleName());

    return this;
  }

}
