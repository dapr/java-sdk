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
limitations under the License.
*/

package io.dapr.workflows.task;

import io.dapr.durabletask.implementation.protobuf.Orchestration;
import io.dapr.durabletask.implementation.protobuf.OrchestratorService;
import io.dapr.durabletask.implementation.protobuf.TaskHubSidecarServiceGrpc;
import io.dapr.workflows.task.worker.DurableTaskGrpcWorker;
import io.dapr.workflows.task.worker.DurableTaskGrpcWorkerBuilder;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that close() waits for the work this worker submitted to a caller-supplied executor,
 * without shutting that executor down.
 */
class DurableTaskGrpcWorkerSuppliedExecutorCloseTest {

  private static final String ACTIVITY_NAME = "Blocking";

  private ExecutorService callerPool;
  private ManagedChannel channel;
  private Server server;

  @AfterEach
  void tearDown() throws Exception {
    if (callerPool != null) {
      callerPool.shutdownNow();
    }
    if (channel != null) {
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (server != null) {
      server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void closeWaitsForInFlightActivityOnSuppliedExecutor() throws Exception {
    OrchestratorService.WorkItem workItem = OrchestratorService.WorkItem.newBuilder()
        .setActivityRequest(OrchestratorService.ActivityRequest.newBuilder()
            .setName(ACTIVITY_NAME)
            .setTaskId(1)
            .setWorkflowInstance(Orchestration.WorkflowInstance.newBuilder().setInstanceId("instance").build()))
        .build();

    CompletableFuture<Void> delivered = new CompletableFuture<>();
    String serverName = InProcessServerBuilder.generateName();
    server = InProcessServerBuilder.forName(serverName)
        .directExecutor()
        .addService(new TaskHubSidecarServiceGrpc.TaskHubSidecarServiceImplBase() {
          @Override
          public void getWorkItems(
              OrchestratorService.GetWorkItemsRequest request,
              StreamObserver<OrchestratorService.WorkItem> responseObserver) {
            if (delivered.complete(null)) {
              responseObserver.onNext(workItem);
            }
            // Hold the stream open, as daprd does.
          }

          @Override
          public void completeActivityTask(
              OrchestratorService.ActivityResponse request,
              StreamObserver<OrchestratorService.CompleteTaskResponse> responseObserver) {
            responseObserver.onNext(OrchestratorService.CompleteTaskResponse.getDefaultInstance());
            responseObserver.onCompleted();
          }
        })
        .build()
        .start();
    channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();

    CountDownLatch activityStarted = new CountDownLatch(1);
    CountDownLatch releaseActivity = new CountDownLatch(1);
    callerPool = Executors.newCachedThreadPool();
    DurableTaskGrpcWorker worker = new DurableTaskGrpcWorkerBuilder()
        .grpcChannel(channel)
        .withExecutorService(callerPool)
        .addActivity(new TaskActivityFactory() {
          @Override
          public String getName() {
            return ACTIVITY_NAME;
          }

          @Override
          public TaskActivity create() {
            return ctx -> {
              activityStarted.countDown();
              try {
                releaseActivity.await();
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
              return null;
            };
          }
        })
        .build();
    worker.start();
    assertTrue(activityStarted.await(10, TimeUnit.SECONDS), "activity never started");

    CompletableFuture<Void> closed = CompletableFuture.runAsync(worker::close);
    Thread.sleep(500);
    assertFalse(closed.isDone(), "close() returned while an activity was still running");

    releaseActivity.countDown();
    closed.get(10, TimeUnit.SECONDS);
    assertFalse(callerPool.isShutdown(), "close() shut down an executor it does not own");
  }
}
