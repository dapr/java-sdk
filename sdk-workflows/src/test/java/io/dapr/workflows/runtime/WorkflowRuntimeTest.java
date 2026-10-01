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
import io.dapr.durabletask.implementation.protobuf.OrchestratorService;
import io.dapr.durabletask.implementation.protobuf.TaskHubSidecarServiceGrpc;
import io.dapr.utils.NetworkUtils;
import io.dapr.workflows.internal.GrpcChannelKeepalive;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
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

  /**
   * Regression test for dapr/java-sdk#1734. While the worker is blocked on the sidecar's
   * work-item stream, a graceful channel shutdown cannot complete until that stream ends, so
   * {@link WorkflowRuntime#close()} used to hang for the whole 60 second channel timeout. The
   * worker has to be stopped before the channel is shut down.
   */
  @Test
  public void closeDoesNotBlockWhileWorkerIsStreaming() throws Exception {
    CountDownLatch streamOpened = new CountDownLatch(1);
    String serverName = InProcessServerBuilder.generateName();
    Server server = InProcessServerBuilder.forName(serverName)
        .directExecutor()
        .addService(new TaskHubSidecarServiceGrpc.TaskHubSidecarServiceImplBase() {
          @Override
          public void getWorkItems(OrchestratorService.GetWorkItemsRequest request,
                                   StreamObserver<OrchestratorService.WorkItem> responseObserver) {
            // Never complete the stream: mimic an idle sidecar with no work items to hand out.
            streamOpened.countDown();
          }
        })
        .build()
        .start();
    ManagedChannel channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();

    try {
      ExecutorService executorService = Executors.newCachedThreadPool();
      DurableTaskGrpcWorker worker = new DurableTaskGrpcWorkerBuilder()
          .grpcChannel(channel)
          .withExecutorService(executorService)
          .build();
      WorkflowRuntime runtime = new WorkflowRuntime(worker, channel, executorService);

      runtime.start(false);
      assertTrue(streamOpened.await(10, TimeUnit.SECONDS), "worker should open the work-item stream");

      long startedAt = System.nanoTime();
      assertDoesNotThrow(runtime::close);
      long elapsedSeconds = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startedAt);

      assertTrue(elapsedSeconds < 30, "close() blocked for " + elapsedSeconds + "s waiting on the open stream");
      assertTrue(channel.isTerminated(), "channel should be terminated after close()");
      assertTrue(executorService.isShutdown(), "executor should be shut down after close()");
      assertTrue(runtime.isClosed(), "runtime should report itself closed");
    } finally {
      channel.shutdownNow();
      server.shutdownNow();
    }
  }

  @Test
  public void closeIsIdempotent() {
    DurableTaskGrpcWorker worker = new DurableTaskGrpcWorkerBuilder().build();
    ManagedChannel channel = NetworkUtils.buildGrpcManagedChannel(new Properties());
    WorkflowRuntime runtime = new WorkflowRuntime(worker, channel, Executors.newCachedThreadPool());

    assertFalse(runtime.isClosed());
    runtime.close();
    assertTrue(runtime.isClosed());
    assertDoesNotThrow(runtime::close);
    assertTrue(channel.isShutdown());
  }
}
