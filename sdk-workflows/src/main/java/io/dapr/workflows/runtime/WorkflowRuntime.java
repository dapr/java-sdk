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

import com.microsoft.durabletask.DurableTaskGrpcWorker;
import com.microsoft.durabletask.DurableTaskGrpcWorkerBuilder;
import io.dapr.config.Properties;

/**
 * Contains methods to register workflows and activities.
 */
public class WorkflowRuntime implements AutoCloseable {

  private static volatile WorkflowRuntime instance;
  private DurableTaskGrpcWorkerBuilder builder;
  private DurableTaskGrpcWorker worker;

  private WorkflowRuntime() throws IllegalStateException {

    if (instance != null) {
      throw new IllegalStateException("WorkflowRuntime should only be constructed once");
    }

    int port = Properties.GRPC_PORT.get();
    if (port <= 0) {
      throw new IllegalStateException(String.format("Invalid port, %s. Must greater than 0", port));
    }

    this.builder = new DurableTaskGrpcWorkerBuilder().port(port);
  }

  /**
   * Returns an WorkflowRuntime object.
   *
   * @return An WorkflowRuntime object.
   */
  public static WorkflowRuntime getInstance() {
    if (instance == null) {
      synchronized (WorkflowRuntime.class) {
        if (instance == null) {
          instance = new WorkflowRuntime();
        }
      }
    }
    return instance;
  }

  /**
   * Registers a Workflow object.
   *
   * @param <T>   any Workflow type
   * @param clazz the class being registered
   */
  public <T extends Workflow> void registerWorkflow(Class<T> clazz) {
    this.builder = this.builder.addOrchestration(
        new OrchestratorWrapper<>(clazz)
    );
  }

  /**
   * Registers an Activity object.
   *
   * @param <T>   any Activity type
   * @param clazz the class being registered
   */
  public <T extends WorkflowActivity> void registerActivity(Class<T> clazz) {
    this.builder = this.builder.addActivity(
        new ActivityWrapper<>(clazz)
    );
  }

  /**
   * Start the Workflow runtime processing items on a non-blocking
   * background thread indefinitely or until that thread is interrupted.
   */
  public void start() {
    if (this.worker == null) {
      this.worker = this.builder.build();
      this.worker.start();
    }
  }

  /**
   * Start the Workflow runtime processing items on the current thread
   * and block indefinitely or until that thread is interrupted.
   */
  public void startAndBlock() {
    if (this.worker == null) {
      this.worker = this.builder.build();
      this.worker.startAndBlock();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    if (this.worker != null) {
      this.worker.close();
      this.worker = null;
    }
  }
}