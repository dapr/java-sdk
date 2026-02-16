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

import io.dapr.durabletask.DurableTaskGrpcWorker;
import io.grpc.ManagedChannel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Contains methods to register workflows and activities.
 */
public class WorkflowRuntime implements AutoCloseable {

  private final DurableTaskGrpcWorker worker;
  private final ManagedChannel managedChannel;
  private final ExecutorService executorService;

  /**
   * Constructor.
   *
   * @param worker grpcWorker processing activities.
   * @param managedChannel grpc channel.
   * @param executorService executor service responsible for running the threads.
   */
  public WorkflowRuntime(DurableTaskGrpcWorker worker,
                         ManagedChannel managedChannel,
                         ExecutorService executorService) {
    this.worker = worker;
    this.managedChannel = managedChannel;
    this.executorService = executorService;
  }

  /**
   * Start the Workflow runtime processing items and block.
   *
   */
  public void start() {
    this.start(true);
  }

  /**
   * Start the Workflow runtime processing items.
   *
   * @param block block the thread if true
   */
  public void start(boolean block) {
    if (block) {
      this.worker.startAndBlock();
    } else {
      this.worker.start();
    }
  }

  /**
   * {@inheritDoc}
   */
  public void close() {
    this.shutDownWorkerPool();
    this.closeSideCarChannel();
    this.worker.close();
  }

  private void closeSideCarChannel() {
    this.managedChannel.shutdown();

    try {
      if (!this.managedChannel.awaitTermination(60, TimeUnit.SECONDS)) {
        this.managedChannel.shutdownNow();
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  private void shutDownWorkerPool() {
    this.executorService.shutdown();
    try {
      if (!this.executorService.awaitTermination(60, TimeUnit.SECONDS)) {
        this.executorService.shutdownNow();
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }
}
