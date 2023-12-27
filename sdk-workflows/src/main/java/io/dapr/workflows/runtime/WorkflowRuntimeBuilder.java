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

import com.microsoft.durabletask.DurableTaskGrpcWorkerBuilder;
import io.dapr.utils.NetworkUtils;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.internal.ApiTokenClientInterceptor;
import io.grpc.ClientInterceptor;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WorkflowRuntimeBuilder {
  private static volatile WorkflowRuntime instance;
  private DurableTaskGrpcWorkerBuilder builder;
  private Logger logger;
  private String logLevel;
  private ArrayList<String> workflows = new ArrayList<String>();
  private ArrayList<String> activities = new ArrayList<String>();
  private static ClientInterceptor WORKFLOW_INTERCEPTOR = new ApiTokenClientInterceptor();

  /**
   * Constructs the WorkflowRuntimeBuilder.
   */
  public WorkflowRuntimeBuilder() {
    this.builder = new DurableTaskGrpcWorkerBuilder().grpcChannel(
                          NetworkUtils.buildGrpcManagedChannel(WORKFLOW_INTERCEPTOR));
    this.logger = Logger.getLogger(WorkflowRuntimeBuilder.class.getName());
    this.logLevel = System.getenv("DAPR_LOG_LEVEL");
    if (this.logLevel == null || this.logLevel.isEmpty()) {
      this.logger.log(Level.INFO, "Setting the Dapr workflow runtime log level to INFO");
      this.logger.log(Level.INFO, "To change the log level, set the DAPR_LOG_LEVEL environment variable to one of the"
          +  " following: SEVERE, WARNING, INFO, DEBUG, CONFIG, FINE, FINER, FINEST");
      this.logger.setLevel(Level.INFO); 
    } else {
      // try to convert environment variabel to a specific log level.
      switch (this.logLevel.toUpperCase()) {
        case "SEVERE":
          this.logger.setLevel(Level.SEVERE);
          this.logger.log(Level.INFO, "Setting the Dapr workflow runtime log level to SEVERE");
          break;
        case "WARNING":
          this.logger.setLevel(Level.WARNING);
          this.logger.log(Level.INFO, "Setting the Dapr workflow runtime log level to WARNING");
          break;
        case "INFO":
          this.logger.setLevel(Level.INFO);
          this.logger.log(Level.INFO, "Setting the Dapr workflow runtime log level to INFO");
          break;
        case "DEBUG":
          // DEBUG is not a Java regonized log level enumeration, so it might make sense to use "CONFIG" as the
          // replacement level and still allow the user to set the variable to DEBUG and we convert it here
          this.logger.setLevel(Level.CONFIG);
          this.logger.log(Level.INFO, "Setting the Dapr workflow runtime log level to DEBUG/CONFIG");
          break;
        case "CONFIG":
          this.logger.setLevel(Level.CONFIG);
          this.logger.log(Level.INFO, "Setting the Dapr workflow runtime log level to DEBUG/CONFIG");
          break;
        case "FINE":
          this.logger.setLevel(Level.FINE);
          this.logger.log(Level.INFO, "Setting the Dapr workflow runtime log level to FINE");
          break;
        case "FINER":
          this.logger.setLevel(Level.FINER);
          this.logger.log(Level.INFO, "Setting the Dapr workflow runtime log level to FINER");
          break;
        case "FINEST":
          this.logger.setLevel(Level.FINEST);
          this.logger.log(Level.INFO, "Setting the Dapr workflow runtime log level to FINEST");
          break;
        default:
          this.logger.log(Level.INFO, "Environment variable: DAPR_LOG_LEVEL was not set to a recognized value."
              + " Defaulting log level to INFO");
          this.logger.log(Level.INFO, "Available levels for the DAPR_LOG_LEVEL are: SEVERE, WARNING, INFO,"
              + " DEBUG, CONFIG, FINE, FINER, FINEST");
          break;
      }
    }
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
    this.logger.log(Level.INFO, "Successfully built dapr workflow runtime");
    this.logger.log(Level.INFO, "List of registered workflows: " + workflows);
    this.logger.log(Level.INFO, "List of registered activities: " + activities);
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
    this.builder = this.builder.addOrchestration(
        new OrchestratorWrapper<>(clazz)
    );
    this.logger.log(Level.INFO, "Registered Workflow: " +  clazz.getCanonicalName());
    this.workflows.add(clazz.getCanonicalName());
    // If possible, attempt to grab the workflow names from the underlying durableTask framework and use those
    // since they are already registered in a hashmap
    // Potentially need to create getter methods inside the underlying java code. This would also apply for activities
    // Class<?>[] classarry = this.builder.getClass().getClasses();

    return this;
  }

  /**
   * Registers an Activity object.
   *
   * @param clazz the class being registered
   * @param <T>   any WorkflowActivity type
   */
  public <T extends WorkflowActivity> void registerActivity(Class<T> clazz) {
    this.builder = this.builder.addActivity(
        new ActivityWrapper<>(clazz)
    );
    this.logger.log(Level.INFO, "Registered Activity: " +  clazz.getCanonicalName());
    this.activities.add(clazz.getCanonicalName());
  }
}
