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

import io.dapr.workflows.internal.GrpcChannelKeepalive;
import io.dapr.workflows.task.worker.DurableTaskGrpcWorker;
import io.grpc.ManagedChannel;

import javax.annotation.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Contains methods to register workflows and activities.
 */
public class WorkflowRuntime implements AutoCloseable {

  private final DurableTaskGrpcWorker worker;
  private final ManagedChannel managedChannel;
  private final ExecutorService executorService;
  private final GrpcChannelKeepalive keepalive;
  private final boolean ownsExecutorService;

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
    this(worker, managedChannel, executorService, null);
  }

  /**
   * Constructor.
   *
   * @param worker grpcWorker processing activities.
   * @param managedChannel grpc channel.
   * @param executorService executor service responsible for running the threads.
   * @param keepalive application-level keepalive on the worker's channel, started with
   *                  {@link #start()} and stopped on {@link #close()}. May be null when
   *                  no keepalive is wanted.
   */
  public WorkflowRuntime(DurableTaskGrpcWorker worker,
                         ManagedChannel managedChannel,
                         ExecutorService executorService,
                         @Nullable GrpcChannelKeepalive keepalive) {
    this(worker, managedChannel, executorService, keepalive, true);
  }

  /**
   * Constructor.
   *
   * @param worker grpcWorker processing activities.
   * @param managedChannel grpc channel.
   * @param executorService executor service responsible for running the threads.
   * @param keepalive application-level keepalive on the worker's channel, started with
   *                  {@link #start()} and stopped on {@link #close()}. May be null when
   *                  no keepalive is wanted.
   * @param ownsExecutorService whether this runtime created the executor and is therefore
   *                            responsible for shutting it down on {@link #close()}. Pass
   *                            false for an executor owned by the caller or by a framework
   *                            such as Spring, which must outlive this runtime.
   */
  public WorkflowRuntime(DurableTaskGrpcWorker worker,
                         ManagedChannel managedChannel,
                         ExecutorService executorService,
                         @Nullable GrpcChannelKeepalive keepalive,
                         boolean ownsExecutorService) {
    this.worker = worker;
    this.managedChannel = managedChannel;
    this.executorService = executorService;
    this.keepalive = keepalive;
    this.ownsExecutorService = ownsExecutorService;
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
    if (this.keepalive != null) {
      this.keepalive.start();
    }
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
    if (this.keepalive != null) {
      this.keepalive.close();
    }
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
    if (!this.ownsExecutorService) {
      return;
    }

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
