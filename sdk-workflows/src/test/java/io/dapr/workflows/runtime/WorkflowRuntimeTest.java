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
import io.dapr.durabletask.DurableTaskGrpcWorkerBuilder;
import io.dapr.config.Properties;
import io.dapr.utils.NetworkUtils;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class WorkflowRuntimeTest {

  @Test
  public void startTest() {
    DurableTaskGrpcWorker worker = new DurableTaskGrpcWorkerBuilder().build();
    WorkflowRuntime runtime = new WorkflowRuntime(worker, NetworkUtils.buildGrpcManagedChannel(new Properties()),
            Executors.newCachedThreadPool());
    assertDoesNotThrow(() -> runtime.start(false));
  }

  @Test
  public void closeWithoutStarting() {
    DurableTaskGrpcWorker worker = new DurableTaskGrpcWorkerBuilder().build();
    try (WorkflowRuntime runtime = new WorkflowRuntime(worker, NetworkUtils.buildGrpcManagedChannel(new Properties()),
            Executors.newCachedThreadPool())) {
      assertDoesNotThrow(runtime::close);
    }
  }
}
