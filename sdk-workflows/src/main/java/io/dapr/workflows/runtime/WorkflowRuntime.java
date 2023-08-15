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

/**
 * Contains methods to register workflows and activities.
 */
public class WorkflowRuntime implements AutoCloseable {

  private DurableTaskGrpcWorker worker;

  public WorkflowRuntime(DurableTaskGrpcWorker worker) {
    this.worker = worker;
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
  @Override
  public void close() {
    if (this.worker != null) {
      this.worker.close();
      this.worker = null;
    }
  }
}