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

import com.google.protobuf.StringValue;
import io.dapr.durabletask.implementation.protobuf.Orchestration;
import io.dapr.durabletask.implementation.protobuf.OrchestratorService;
import io.dapr.durabletask.implementation.protobuf.TaskHubSidecarServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies DurableTaskGrpcWorker accepts a work item larger than gRPC's 4MB default receive limit.
 *
 * <p>daprd sends work items up to its own max body size, so a fan-out of five 1MB activity results
 * is a work item of about 6MB. The channel is built like the SDK's own, whose inbound limit defaults
 * to 4MB (DAPR_GRPC_MAX_INBOUND_MESSAGE_SIZE_BYTES). Uses a real TCP server because the in-process
 * transport does not enforce message sizes.</p>
 */
class DurableTaskGrpcWorkerLargeWorkItemTest {

  private static final int DEFAULT_MAX_INBOUND_BYTES = 4 * 1024 * 1024;
  private static final int PAYLOAD_CHARS = 6 * 1024 * 1024;
  private static final String ACTIVITY_NAME = "PayloadLength";

  private DurableTaskGrpcWorker worker;
  private ManagedChannel channel;
  private Server server;

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
  void workerProcessesWorkItemLargerThanDefaultReceiveLimit() throws Exception {
    OrchestratorService.ActivityRequest activityRequest = OrchestratorService.ActivityRequest.newBuilder()
        .setName(ACTIVITY_NAME)
        .setTaskId(1)
        .setWorkflowInstance(Orchestration.WorkflowInstance.newBuilder().setInstanceId("instance").build())
        .setInput(StringValue.of("\"" + "x".repeat(PAYLOAD_CHARS) + "\""))
        .build();
    OrchestratorService.WorkItem workItem = OrchestratorService.WorkItem.newBuilder()
        .setActivityRequest(activityRequest)
        .build();

    AtomicInteger streams = new AtomicInteger();
    CompletableFuture<OrchestratorService.ActivityResponse> activityResponse = new CompletableFuture<>();
    server = NettyServerBuilder.forPort(0)
        .addService(new TaskHubSidecarServiceGrpc.TaskHubSidecarServiceImplBase() {
          @Override
          public void getWorkItems(
              OrchestratorService.GetWorkItemsRequest request,
              StreamObserver<OrchestratorService.WorkItem> responseObserver) {
            streams.incrementAndGet();
            if (!activityResponse.isDone()) {
              responseObserver.onNext(workItem);
            }
            // Hold the stream open, as daprd does.
          }

          @Override
          public void completeActivityTask(
              OrchestratorService.ActivityResponse request,
              StreamObserver<OrchestratorService.CompleteTaskResponse> responseObserver) {
            activityResponse.complete(request);
            responseObserver.onNext(OrchestratorService.CompleteTaskResponse.getDefaultInstance());
            responseObserver.onCompleted();
          }
        })
        .build()
        .start();

    channel = ManagedChannelBuilder.forAddress("localhost", server.getPort())
        .usePlaintext()
        .maxInboundMessageSize(DEFAULT_MAX_INBOUND_BYTES)
        .build();
    worker = new DurableTaskGrpcWorkerBuilder()
        .grpcChannel(channel)
        .addActivity(new TaskActivityFactory() {
          @Override
          public String getName() {
            return ACTIVITY_NAME;
          }

          @Override
          public TaskActivity create() {
            return ctx -> ctx.getInput(String.class).length();
          }
        })
        .build();
    worker.start();

    OrchestratorService.ActivityResponse result;
    try {
      result = activityResponse.get(15, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      throw new AssertionError("no activity result after " + streams.get() + " work item streams: "
          + "the worker could not receive the work item", e);
    }
    assertEquals(String.valueOf(PAYLOAD_CHARS), result.getResult().getValue());
  }
}
