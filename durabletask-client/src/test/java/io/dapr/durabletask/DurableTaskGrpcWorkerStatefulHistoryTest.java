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
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Worker-level tests for the stateful-history protocol driven through the real
 * {@link DurableTaskGrpcWorker} against an in-process fake sidecar: the capability is advertised by
 * default and suppressed when disabled, and a delta work item against a cold cache falls back to
 * the GetInstanceHistory RPC.
 */
class DurableTaskGrpcWorkerStatefulHistoryTest {

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

  private void startWorker(TaskHubSidecarServiceGrpc.TaskHubSidecarServiceImplBase service, boolean disableStatefulHistory)
      throws Exception {
    String serverName = InProcessServerBuilder.generateName();
    server = InProcessServerBuilder.forName(serverName).directExecutor().addService(service).build().start();
    channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
    worker = new DurableTaskGrpcWorkerBuilder()
        .grpcChannel(channel)
        .disableStatefulHistory(disableStatefulHistory)
        .build();
    worker.start();
  }

  @Test
  void advertisesStatefulHistoryCapabilityByDefault() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<List<OrchestratorService.WorkerCapability>> captured = new AtomicReference<>();

    startWorker(new TaskHubSidecarServiceGrpc.TaskHubSidecarServiceImplBase() {
      @Override
      public void getWorkItems(OrchestratorService.GetWorkItemsRequest request,
          StreamObserver<OrchestratorService.WorkItem> responseObserver) {
        captured.compareAndSet(null, request.getCapabilitiesList());
        latch.countDown();
        // Keep the stream open so the worker does not reconnect in a tight loop.
      }
    }, false);

    assertTrue(latch.await(10, TimeUnit.SECONDS), "worker should have called getWorkItems");
    assertNotNull(captured.get());
    assertTrue(captured.get().contains(OrchestratorService.WorkerCapability.WORKER_CAPABILITY_STATEFUL_HISTORY),
        "the worker must advertise WORKER_CAPABILITY_STATEFUL_HISTORY by default");
  }

  @Test
  void doesNotAdvertiseCapabilityWhenDisabled() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<List<OrchestratorService.WorkerCapability>> captured = new AtomicReference<>();

    startWorker(new TaskHubSidecarServiceGrpc.TaskHubSidecarServiceImplBase() {
      @Override
      public void getWorkItems(OrchestratorService.GetWorkItemsRequest request,
          StreamObserver<OrchestratorService.WorkItem> responseObserver) {
        captured.compareAndSet(null, request.getCapabilitiesList());
        latch.countDown();
      }
    }, true);

    assertTrue(latch.await(10, TimeUnit.SECONDS), "worker should have called getWorkItems");
    assertNotNull(captured.get());
    assertFalse(captured.get().contains(OrchestratorService.WorkerCapability.WORKER_CAPABILITY_STATEFUL_HISTORY),
        "a disabled worker must not advertise the capability");
    assertTrue(captured.get().isEmpty());
  }

  @Test
  void deltaWorkItemWithColdCacheFallsBackToGetInstanceHistory() throws Exception {
    CountDownLatch fetchLatch = new CountDownLatch(1);
    AtomicInteger getHistoryCalls = new AtomicInteger(0);

    startWorker(new TaskHubSidecarServiceGrpc.TaskHubSidecarServiceImplBase() {
      @Override
      public void getWorkItems(OrchestratorService.GetWorkItemsRequest request,
          StreamObserver<OrchestratorService.WorkItem> responseObserver) {
        // A delta work item (cachedHistory set) for an instance the freshly connected worker holds
        // nothing for: its cache is cold, so it must fetch the full history.
        OrchestratorService.WorkflowRequest workflowRequest = OrchestratorService.WorkflowRequest.newBuilder()
            .setInstanceId("inst-miss")
            .setCachedHistory(OrchestratorService.CachedHistory.newBuilder().setEventCount(5))
            .build();
        responseObserver.onNext(OrchestratorService.WorkItem.newBuilder()
            .setWorkflowRequest(workflowRequest)
            .build());
        // Keep the stream open.
      }

      @Override
      public void getInstanceHistory(OrchestratorService.GetInstanceHistoryRequest request,
          StreamObserver<OrchestratorService.GetInstanceHistoryResponse> responseObserver) {
        getHistoryCalls.incrementAndGet();
        fetchLatch.countDown();
        responseObserver.onNext(OrchestratorService.GetInstanceHistoryResponse.getDefaultInstance());
        responseObserver.onCompleted();
      }
    }, false);

    assertTrue(fetchLatch.await(10, TimeUnit.SECONDS),
        "a delta work item against a cold cache must trigger a GetInstanceHistory fetch");
    assertEquals(1, getHistoryCalls.get());
  }
}
