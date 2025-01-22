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

package io.dapr.spring.workflows.config;

import com.microsoft.durabletask.DurableTaskGrpcWorkerBuilder;
import io.dapr.config.Properties;
import io.dapr.utils.NetworkUtils;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.internal.ApiTokenClientInterceptor;
import io.dapr.workflows.runtime.WorkflowRuntime;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
public class SpringBootWorkflowRuntimeBuilder {

  private static final ClientInterceptor WORKFLOW_INTERCEPTOR = new ApiTokenClientInterceptor();
  private static volatile WorkflowRuntime instance;
  private final Logger logger;
  private final Set<String> workflows = new HashSet<>();
  private final Set<String> activities = new HashSet<>();
  private final Set<String> activitySet = Collections.synchronizedSet(new HashSet<>());
  private final Set<String> workflowSet = Collections.synchronizedSet(new HashSet<>());
  private final DurableTaskGrpcWorkerBuilder builder;

  /**
   * Constructs the SpringBootWorkflowRuntimeBuilder.
   *
   */
  public SpringBootWorkflowRuntimeBuilder() {
    this(new Properties());
  }

  /**
   * Constructs the SpringBootWorkflowRuntimeBuilder.
   *
   * @param properties Properties to use.
   */
  public SpringBootWorkflowRuntimeBuilder(Properties properties) {
    this(properties, LoggerFactory.getLogger(SpringBootWorkflowRuntimeBuilder.class));
  }

  /**
   * Constructs the SpringBootWorkflowRuntimeBuilder.
   *
   * @param logger application logger.
   */
  public SpringBootWorkflowRuntimeBuilder(Logger logger) {
    this(new Properties(), logger);
  }

  /**
   * Constructs the SpringBootWorkflowRuntimeBuilder.
   *
   * @param properties Properties to use.
   * @param logger application logger.
   */
  private SpringBootWorkflowRuntimeBuilder(Properties properties,
                                           Logger logger) {
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
   * @param <T> any Workflow bean
   * @param workflowBean bean to use
   * @return the SpringBootWorkflowRuntimeBuilder
   */
  public <T extends Workflow> SpringBootWorkflowRuntimeBuilder registerWorkflow(T workflowBean) {
    this.builder.addOrchestration(new SpringBootWorkflowWrapper<>(workflowBean));
    this.workflowSet.add(workflowBean.getClass().getCanonicalName());
    this.workflows.add(workflowBean.getClass().getSimpleName());

    this.logger.info("Registered Workflow: " +  workflowBean.getClass().getSimpleName());

    return this;
  }

  /**
   * Registers an Activity bean.
   *
   * @param <T> any WorkflowActivity type
   * @param activityBean bean to use
   * @return the SpringBootWorkflowRuntimeBuilder
   */
  public <T extends WorkflowActivity> SpringBootWorkflowRuntimeBuilder registerActivity(T activityBean) {
    this.builder.addActivity(new SpringBootWorkflowActivityWrapper<>(activityBean));
    this.activitySet.add(activityBean.getClass().getCanonicalName());
    this.activities.add(activityBean.getClass().getSimpleName());

    this.logger.info("Registered Activity: " +  activityBean.getClass().getSimpleName());

    return this;
  }

}
