/*
 * Copyright 2026 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.dapr.durabletask;

import io.dapr.durabletask.implementation.protobuf.OrchestratorService;
import io.dapr.durabletask.implementation.protobuf.TaskHubSidecarServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that DurableTaskGrpcWorker auto-heals (reconnects) when the gRPC
 * connection to the sidecar drops with UNAVAILABLE or CANCELLED status.
 *
 * @see <a href="https://github.com/dapr/java-sdk/issues/1652">Issue #1652</a>
 */
class DurableTaskGrpcWorkerReconnectTest {

  private DurableTaskGrpcWorker worker;
  private Server server;
  private ManagedChannel channel;

  @AfterEach
  void tearDown() throws Exception {
    if (worker != null) {
      worker.close();
    }
    if (channel != null) {
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (server != null) {
      server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void workerReconnectsAfterUnavailableError() throws Exception {
    int requiredCalls = 3;
    CountDownLatch latch = new CountDownLatch(requiredCalls);
    AtomicInteger callCount = new AtomicInteger(0);

    String serverName = InProcessServerBuilder.generateName();
    server = InProcessServerBuilder.forName(serverName)
        .directExecutor()
        .addService(new TaskHubSidecarServiceGrpc.TaskHubSidecarServiceImplBase() {
          @Override
          public void getWorkItems(
              OrchestratorService.GetWorkItemsRequest request,
              StreamObserver<OrchestratorService.WorkItem> responseObserver) {
            callCount.incrementAndGet();
            latch.countDown();
            // Simulate sidecar being unavailable
            responseObserver.onError(Status.UNAVAILABLE
                .withDescription("Sidecar is unavailable")
                .asRuntimeException());
          }
        })
        .build()
        .start();

    channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();

    worker = new DurableTaskGrpcWorkerBuilder()
        .grpcChannel(channel)
        .build();
    worker.start();

    // The worker should retry multiple times after UNAVAILABLE errors.
    // With a 5-second retry delay, we wait long enough for at least 3 attempts.
    boolean reached = latch.await(30, TimeUnit.SECONDS);
    assertTrue(reached,
        "Expected at least " + requiredCalls + " getWorkItems calls (reconnect attempts), but got " + callCount.get());
  }

  @Test
  void workerReconnectsAfterCancelledError() throws Exception {
    int requiredCalls = 2;
    CountDownLatch latch = new CountDownLatch(requiredCalls);
    AtomicInteger callCount = new AtomicInteger(0);

    String serverName = InProcessServerBuilder.generateName();
    server = InProcessServerBuilder.forName(serverName)
        .directExecutor()
        .addService(new TaskHubSidecarServiceGrpc.TaskHubSidecarServiceImplBase() {
          @Override
          public void getWorkItems(
              OrchestratorService.GetWorkItemsRequest request,
              StreamObserver<OrchestratorService.WorkItem> responseObserver) {
            callCount.incrementAndGet();
            latch.countDown();
            // Simulate connection cancelled (e.g., sidecar restart)
            responseObserver.onError(Status.CANCELLED
                .withDescription("Connection cancelled")
                .asRuntimeException());
          }
        })
        .build()
        .start();

    channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();

    worker = new DurableTaskGrpcWorkerBuilder()
        .grpcChannel(channel)
        .build();
    worker.start();

    boolean reached = latch.await(30, TimeUnit.SECONDS);
    assertTrue(reached,
        "Expected at least " + requiredCalls + " getWorkItems calls after CANCELLED, but got " + callCount.get());
  }

  @Test
  void workerReconnectsAfterStreamEndsNormally() throws Exception {
    int requiredCalls = 2;
    CountDownLatch latch = new CountDownLatch(requiredCalls);
    AtomicInteger callCount = new AtomicInteger(0);

    String serverName = InProcessServerBuilder.generateName();
    server = InProcessServerBuilder.forName(serverName)
        .directExecutor()
        .addService(new TaskHubSidecarServiceGrpc.TaskHubSidecarServiceImplBase() {
          @Override
          public void getWorkItems(
              OrchestratorService.GetWorkItemsRequest request,
              StreamObserver<OrchestratorService.WorkItem> responseObserver) {
            callCount.incrementAndGet();
            latch.countDown();
            // Simulate stream ending normally (server completes without sending items)
            responseObserver.onCompleted();
          }
        })
        .build()
        .start();

    channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();

    worker = new DurableTaskGrpcWorkerBuilder()
        .grpcChannel(channel)
        .build();
    worker.start();

    // When the stream ends normally, the outer while(true) loop should
    // re-establish the stream immediately (no 5s delay for normal completion).
    boolean reached = latch.await(10, TimeUnit.SECONDS);
    assertTrue(reached,
        "Expected at least " + requiredCalls + " getWorkItems calls after normal stream end, but got " + callCount.get());
  }

  @Test
  void workerStopsCleanlyOnClose() throws Exception {
    CountDownLatch firstCallLatch = new CountDownLatch(1);

    String serverName = InProcessServerBuilder.generateName();
    server = InProcessServerBuilder.forName(serverName)
        .directExecutor()
        .addService(new TaskHubSidecarServiceGrpc.TaskHubSidecarServiceImplBase() {
          @Override
          public void getWorkItems(
              OrchestratorService.GetWorkItemsRequest request,
              StreamObserver<OrchestratorService.WorkItem> responseObserver) {
            firstCallLatch.countDown();
            // Keep stream open (simulate connected state)
            // The worker should be interrupted by close()
          }
        })
        .build()
        .start();

    channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();

    worker = new DurableTaskGrpcWorkerBuilder()
        .grpcChannel(channel)
        .build();
    worker.start();

    // Wait for the worker to connect
    assertTrue(firstCallLatch.await(10, TimeUnit.SECONDS), "Worker should have connected");

    // Close should stop the worker cleanly without hanging
    worker.close();
    worker = null; // prevent double-close in tearDown
  }
}
