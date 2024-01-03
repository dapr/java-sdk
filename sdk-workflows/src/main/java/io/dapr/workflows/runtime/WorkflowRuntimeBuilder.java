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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WorkflowRuntimeBuilder {
  private static volatile WorkflowRuntime instance;
  private DurableTaskGrpcWorkerBuilder builder;
  private Logger logger;
  private String logLevel;
  private Set<String> workflows = new HashSet<String>();
  private Set<String> activities = new HashSet<String>();
  private static ClientInterceptor WORKFLOW_INTERCEPTOR = new ApiTokenClientInterceptor();

  /**
   * Constructs the WorkflowRuntimeBuilder.
   */
  public WorkflowRuntimeBuilder() {
    this.builder = new DurableTaskGrpcWorkerBuilder().grpcChannel(
                          NetworkUtils.buildGrpcManagedChannel(WORKFLOW_INTERCEPTOR));
    this.logger = Logger.getLogger(WorkflowRuntimeBuilder.class.getName());
    setLogLevel();
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
    String logTime = getLogTime();
    this.logger.log(Level.INFO, logTime + " Successfully built dapr workflow runtime");
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
    String logTime = getLogTime();
    this.logger.log(Level.INFO, logTime + " Registered Workflow: " +  clazz.getSimpleName());
    this.workflows.add(clazz.getSimpleName());
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
    String logTime = getLogTime();
    this.logger.log(Level.INFO, logTime + " Registered Activity: " +  clazz.getSimpleName());
    this.activities.add(clazz.getSimpleName());
  }

  /**
   * Sets the log level for the workflow runtime builder.
   */
  private void setLogLevel() {
    this.logLevel = System.getenv("DAPR_LOG_LEVEL");
    String logTime = getLogTime();
    if (this.logLevel == null || this.logLevel.isEmpty()) {
      this.logger.log(Level.INFO, logTime + " Setting the Dapr workflow runtime log level to INFO");
      this.logger.log(Level.INFO, logTime + " To change the log level, set the DAPR_LOG_LEVEL environment variable to"
          +  " one of the following: SEVERE, WARNING, INFO, DEBUG, CONFIG, FINE, FINER, FINEST");
      this.logger.setLevel(Level.INFO); 
    } else {
      // try to convert environment variabel to a specific log level.
      switch (this.logLevel.toUpperCase()) {
        case "SEVERE":
          this.logger.setLevel(Level.SEVERE);
          this.logger.log(Level.INFO, logTime + " Setting the Dapr workflow runtime log level to SEVERE");
          break;
        case "WARNING":
          this.logger.setLevel(Level.WARNING);
          this.logger.log(Level.INFO, logTime + " Setting the Dapr workflow runtime log level to WARNING");
          break;
        case "INFO":
          this.logger.setLevel(Level.INFO);
          this.logger.log(Level.INFO, logTime + " Setting the Dapr workflow runtime log level to INFO");
          break;
        case "DEBUG":
          // DEBUG is not a Java regonized log level enumeration, so it might make sense to use "CONFIG" as the
          // replacement level and still allow the user to set the variable to DEBUG and we convert it here
          this.logger.setLevel(Level.CONFIG);
          this.logger.log(Level.INFO, logTime + " Setting the Dapr workflow runtime log level to DEBUG/CONFIG");
          break;
        case "CONFIG":
          this.logger.setLevel(Level.CONFIG);
          this.logger.log(Level.INFO, logTime + " Setting the Dapr workflow runtime log level to DEBUG/CONFIG");
          break;
        case "FINE":
          this.logger.setLevel(Level.FINE);
          this.logger.log(Level.INFO, logTime + " Setting the Dapr workflow runtime log level to FINE");
          break;
        case "FINER":
          this.logger.setLevel(Level.FINER);
          this.logger.log(Level.INFO, logTime + " Setting the Dapr workflow runtime log level to FINER");
          break;
        case "FINEST":
          this.logger.setLevel(Level.FINEST);
          this.logger.log(Level.INFO, logTime + " Setting the Dapr workflow runtime log level to FINEST");
          break;
        default:
          this.logger.log(Level.INFO, logTime + " Environment variable: DAPR_LOG_LEVEL was not set to a recognized"
              + " value. Defaulting log level to INFO");
          this.logger.log(Level.INFO, logTime + " Available levels for the DAPR_LOG_LEVEL are: SEVERE, WARNING, INFO,"
              + " DEBUG, CONFIG, FINE, FINER, FINEST");
          break;
      }
    }
  }

  /**
   * Gets the local time and formats it for the logger.
   * @return returns the local time.
   */
  private String getLogTime() {
    LocalDateTime currentDateTime = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy HH:mm:ss");
    String logTime = currentDateTime.format(formatter);
    return logTime;
  }
}