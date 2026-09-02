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


import io.dapr.config.Properties;
import io.dapr.utils.NetworkUtils;
import io.dapr.workflows.internal.GrpcChannelKeepalive;
import io.dapr.workflows.task.worker.DurableTaskGrpcWorker;
import io.dapr.workflows.task.worker.DurableTaskGrpcWorkerBuilder;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WorkflowRuntimeTest {

  @Test
  public void startTest() {
    DurableTaskGrpcWorker worker = new DurableTaskGrpcWorkerBuilder().build();
    WorkflowRuntime runtime = new WorkflowRuntime(worker, NetworkUtils.buildGrpcManagedChannel(new Properties()),
            Executors.newCachedThreadPool());
    assertDoesNotThrow(() -> runtime.start(false));
  }

  @Test
  public void startStartsKeepaliveAndCloseStopsIt() throws InterruptedException {
    String threadName = "dapr-workflow-runtime-keepalive";
    DurableTaskGrpcWorker worker = new DurableTaskGrpcWorkerBuilder().build();
    ManagedChannel channel = NetworkUtils.buildGrpcManagedChannel(new Properties());
    GrpcChannelKeepalive keepalive = new GrpcChannelKeepalive(channel, threadName, Duration.ofSeconds(30));
    WorkflowRuntime runtime = new WorkflowRuntime(worker, channel, Executors.newCachedThreadPool(), keepalive);
    assertFalse(keepaliveThreadAlive(threadName), "keepalive must stay inert until the runtime starts");
    runtime.start(false);
    assertTrue(keepaliveThreadAlive(threadName), "keepalive thread expected after runtime start");
    assertDoesNotThrow(runtime::close);
    long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
    while (keepaliveThreadAlive(threadName) && System.currentTimeMillis() < deadline) {
      Thread.sleep(10);
    }
    assertFalse(keepaliveThreadAlive(threadName), "keepalive thread should terminate after close()");
  }

  private static boolean keepaliveThreadAlive(String threadName) {
    return Thread.getAllStackTraces().keySet().stream()
        .anyMatch(t -> t.getName().equals(threadName) && t.isAlive());
  }

  @Test
  public void closeWithoutStarting() {
    DurableTaskGrpcWorker worker = new DurableTaskGrpcWorkerBuilder().build();
    try (WorkflowRuntime runtime = new WorkflowRuntime(worker, NetworkUtils.buildGrpcManagedChannel(new Properties()),
            Executors.newCachedThreadPool())) {
      assertDoesNotThrow(runtime::close);
    }
  }

  @Test
  public void closeShutsDownAnExecutorItOwns() {
    DurableTaskGrpcWorker worker = new DurableTaskGrpcWorkerBuilder().build();
    ExecutorService executorService = Executors.newCachedThreadPool();
    WorkflowRuntime runtime = new WorkflowRuntime(worker, NetworkUtils.buildGrpcManagedChannel(new Properties()),
        executorService, null, true);

    runtime.close();

    assertTrue(executorService.isShutdown(), "an executor created by the runtime must be shut down on close()");
  }

  @Test
  public void closeLeavesACallerSuppliedExecutorRunning() {
    DurableTaskGrpcWorker worker = new DurableTaskGrpcWorkerBuilder().build();
    ExecutorService executorService = Executors.newCachedThreadPool();
    WorkflowRuntime runtime = new WorkflowRuntime(worker, NetworkUtils.buildGrpcManagedChannel(new Properties()),
        executorService, null, false);

    runtime.close();

    assertFalse(executorService.isShutdown(),
        "an executor supplied by the caller must outlive the runtime, so Spring's shared task executor is not killed");
    executorService.shutdown();
  }
}
