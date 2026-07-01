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
import io.dapr.durabletask.TaskActivityFactory;
import io.dapr.durabletask.orchestration.TaskOrchestrationFactory;
import io.dapr.utils.NetworkUtils;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.internal.ApiTokenClientInterceptor;
import io.dapr.workflows.internal.GrpcChannelKeepalive;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
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
  private final Properties properties;
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
    this.properties = properties;
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
          GrpcChannelKeepalive keepalive = null;
          if (this.properties.getValue(Properties.WORKFLOWS_RUNTIME_APP_KEEP_ALIVE_ENABLED)) {
            keepalive = new GrpcChannelKeepalive(this.managedChannel, "dapr-workflow-runtime-keepalive",
                this.properties.getValue(Properties.WORKFLOWS_APP_KEEP_ALIVE_INTERVAL_SECONDS));
          }
          instance = new WorkflowRuntime(
              this.builder.withExecutorService(this.executorService).build(),
              this.managedChannel, this.executorService, keepalive);
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
   * Disables the stateful-history optimization.
   *
   * <p>By default the worker advertises {@code WORKER_CAPABILITY_STATEFUL_HISTORY} and caches each
   * instance's committed history per work-item stream, so the sidecar can send only the new events
   * (the delta) each turn instead of the full history. A cache miss is always recovered safely via
   * the GetInstanceHistory RPC, so disabling this only affects per-turn bandwidth, never
   * correctness. When disabled, the worker always receives the full history.</p>
   *
   * @param disableStatefulHistory whether to disable the stateful-history optimization
   * @return {@link WorkflowRuntimeBuilder}.
   */
  public WorkflowRuntimeBuilder withStatefulHistoryDisabled(boolean disableStatefulHistory) {
    this.builder.disableStatefulHistory(disableStatefulHistory);
    return this;
  }

  /**
   * Sets the sliding time-to-live for cached instance histories. An instance's entry is reclaimed
   * once it has gone idle (no turn) for longer than this. If not specified, a default of one hour
   * is used. Ignored when the stateful-history optimization is disabled.
   *
   * @param historyCacheTtl the sliding time-to-live for a cached instance history
   * @return {@link WorkflowRuntimeBuilder}.
   */
  public WorkflowRuntimeBuilder withHistoryCacheTtl(Duration historyCacheTtl) {
    this.builder.historyCacheTtl(historyCacheTtl);
    return this;
  }

  /**
   * Sets the maximum number of per-instance histories retained on a single work-item stream;
   * least-recently-used entries are evicted beyond it. A non-positive value uses the built-in
   * default. Ignored when the stateful-history optimization is disabled.
   *
   * @param historyCacheMaxInstances the instance-count cap for the history cache
   * @return {@link WorkflowRuntimeBuilder}.
   */
  public WorkflowRuntimeBuilder withHistoryCacheMaxInstances(int historyCacheMaxInstances) {
    this.builder.historyCacheMaxInstances(historyCacheMaxInstances);
    return this;
  }

  /**
   * Sets the byte budget for cached histories on a single work-item stream; least-recently-used
   * entries are evicted beyond it. A non-positive value means unlimited (bounded only by the
   * instance-count cap and the TTL). Ignored when the stateful-history optimization is disabled.
   *
   * @param historyCacheMaxBytes the byte budget for the history cache
   * @return {@link WorkflowRuntimeBuilder}.
   */
  public WorkflowRuntimeBuilder withHistoryCacheMaxBytes(long historyCacheMaxBytes) {
    this.builder.historyCacheMaxBytes(historyCacheMaxBytes);
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
    return this.registerWorkflow(clazz.getCanonicalName(), clazz, null, null);
  }

  /**
   * Registers a Workflow object.
   *
   * @param <T>   any Workflow type
   * @param name  the name of the workflow to register
   * @param clazz the class being registered
   * @return the WorkflowRuntimeBuilder
   */
  public <T extends Workflow> WorkflowRuntimeBuilder registerWorkflow(String name, Class<T> clazz) {
    return this.registerWorkflow(name, clazz, null, null);
  }

  /**
   * Registers a Workflow object.
   *
   * @param <T>             any Workflow type
   * @param name            the name of the workflow to register
   * @param clazz           the class being registered
   * @param versionName     the version name of the workflow
   * @param isLatestVersion whether the workflow is the latest version
   * @return the WorkflowRuntimeBuilder
   */
  public <T extends Workflow> WorkflowRuntimeBuilder registerWorkflow(String name,
                                                                      Class<T> clazz,
                                                                      String versionName,
                                                                      Boolean isLatestVersion) {

    if (StringUtils.isEmpty(name)) {
      throw new IllegalArgumentException("Workflow name cannot be empty");
    }

    this.builder.addOrchestration(new WorkflowClassWrapper<>(name, clazz, versionName, isLatestVersion));
    this.workflowSet.add(name);
    this.workflows.add(name);

    if (StringUtils.isEmpty(versionName)) {
      this.logger.info("Registered Workflow: {}", clazz.getSimpleName());
    } else {
      this.logger.info("Registered Workflow Version: {} {} - isLatest  {}",
          clazz.getSimpleName(), versionName, isLatestVersion);
    }

    return this;
  }

  /**
   * Registers a Workflow object.
   *
   * @param <T>      any Workflow type
   * @param instance the workflow instance being registered
   * @return the WorkflowRuntimeBuilder
   */
  public <T extends Workflow> WorkflowRuntimeBuilder registerWorkflow(T instance) {
    var name = instance.getClass().getCanonicalName();
    this.registerWorkflow(name, instance, null, null);
    return this;
  }

  /**
   * Registers a Workflow object.
   *
   * @param <T>             any Workflow type
   * @param name            the name of the workflow to register
   * @param instance        the workflow instance being registered
   * @param versionName     the version name of the workflow
   * @param isLatestVersion whether the workflow is the latest version
   * @return the WorkflowRuntimeBuilder
   */
  public <T extends Workflow> WorkflowRuntimeBuilder registerWorkflow(String name,
                                                                      T instance,
                                                                      String versionName,
                                                                      Boolean isLatestVersion) {
    if (StringUtils.isEmpty(name)) {
      throw new IllegalArgumentException("Workflow name cannot be empty");
    }

    this.builder.addOrchestration(new WorkflowInstanceWrapper<>(name, instance, versionName, isLatestVersion));
    this.workflowSet.add(name);
    this.workflows.add(name);

    if (StringUtils.isEmpty(versionName)) {
      this.logger.info("Registered Workflow {}: {}", name, instance.getClass());
    } else {
      this.logger.info("Registered Workflow Version {}: {} {} - isLatest {}",
          name, instance.getClass().getSimpleName(), versionName, isLatestVersion);
    }

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
   * @param name  Name of the activity to register.
   * @param clazz Class of the activity to register.
   * @return the WorkflowRuntimeBuilder
   */
  public <T extends WorkflowActivity> WorkflowRuntimeBuilder registerActivity(String name, Class<T> clazz) {
    if (StringUtils.isEmpty(name)) {
      throw new IllegalArgumentException("Activity name cannot be empty");
    }

    this.builder.addActivity(new WorkflowActivityClassWrapper<>(name, clazz));
    this.activitySet.add(name);
    this.activities.add(name);

    this.logger.info("Registered Activity: {}", clazz.getSimpleName());

    return this;
  }

  /**
   * Registers an Activity object.
   *
   * @param <T>      any WorkflowActivity type
   * @param instance the class instance being registered
   * @return the WorkflowRuntimeBuilder
   */
  public <T extends WorkflowActivity> WorkflowRuntimeBuilder registerActivity(T instance) {
    return this.registerActivity(instance.getClass().getCanonicalName(), instance);
  }

  /**
   * Registers an Activity object.
   *
   * @param <T>      any WorkflowActivity type
   * @param name     Name of the activity to register.
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

  /**
   * Registers a Task Activity using a {@link TaskActivityFactory}.
   *
   * <p>This method allows advanced use cases where activities are created
   * dynamically or require custom instantiation logic instead of relying
   * on class-based or instance-based registration.
   *
   * @param activityName        the logical name of the activity to register
   * @param taskActivityFactory the factory responsible for creating the activity
   * @return the {@link WorkflowRuntimeBuilder}
   */
  public WorkflowRuntimeBuilder registerTaskActivityFactory(
      String activityName,
      TaskActivityFactory taskActivityFactory) {

    this.builder.addActivity(taskActivityFactory);
    this.activitySet.add(activityName);
    this.activities.add(activityName);

    this.logger.info("Registered Activity: {}", activityName);

    return this;
  }

  /**
   * Registers a Task Orchestration using a {@link TaskOrchestrationFactory}.
   *
   * <p>This method is intended for advanced scenarios where orchestrations
   * are created programmatically or require custom construction logic,
   * rather than being registered via workflow classes or instances.
   *
   * @param orchestrationName        the logical name of the orchestration to register
   * @param taskOrchestrationFactory the factory responsible for creating the orchestration
   * @return the {@link WorkflowRuntimeBuilder}
   */
  public WorkflowRuntimeBuilder registerTaskOrchestrationFactory(
      String orchestrationName,
      TaskOrchestrationFactory taskOrchestrationFactory) {

    this.builder.addOrchestration(taskOrchestrationFactory);
    this.workflows.add(orchestrationName);
    this.workflowSet.add(orchestrationName);

    this.logger.info("Registered Workflow: {}", orchestrationName);

    return this;
  }
}
