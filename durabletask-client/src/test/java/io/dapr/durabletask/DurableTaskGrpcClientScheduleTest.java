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
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that {@link NewOrchestrationInstanceOptions} are mapped onto the
 * {@code CreateInstanceRequest} sent to the sidecar.
 */
class DurableTaskGrpcClientScheduleTest {

  private static final String ORCHESTRATION_NAME = "TestOrchestration";

  private Server server;
  private ManagedChannel channel;
  private DurableTaskClient client;
  private final AtomicReference<OrchestratorService.CreateInstanceRequest> capturedRequest = new AtomicReference<>();
  private final AtomicReference<Status> responseStatus = new AtomicReference<>(Status.OK);

  @BeforeEach
  void setUp() throws Exception {
    String serverName = InProcessServerBuilder.generateName();
    server = InProcessServerBuilder.forName(serverName)
        .directExecutor()
        .addService(new TaskHubSidecarServiceGrpc.TaskHubSidecarServiceImplBase() {
          @Override
          public void startInstance(
              OrchestratorService.CreateInstanceRequest request,
              StreamObserver<OrchestratorService.CreateInstanceResponse> responseObserver) {
            capturedRequest.set(request);
            Status status = responseStatus.get();
            if (!status.isOk()) {
              responseObserver.onError(status.asRuntimeException());
              return;
            }
            responseObserver.onNext(OrchestratorService.CreateInstanceResponse.newBuilder()
                .setInstanceId(request.getInstanceId())
                .build());
            responseObserver.onCompleted();
          }
        })
        .build()
        .start();
    channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
    client = new DurableTaskGrpcClientBuilder()
        .grpcChannel(channel)
        .build();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (client != null) {
      client.close();
    }
    if (channel != null) {
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (server != null) {
      server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void scheduleWithoutOptionsDoesNotEnforceUniqueInstanceId() {
    client.scheduleNewOrchestrationInstance(ORCHESTRATION_NAME);

    assertFalse(capturedRequest.get().getEnforceUniqueInstanceId());
  }

  @Test
  void scheduleWithDefaultOptionsDoesNotEnforceUniqueInstanceId() {
    client.scheduleNewOrchestrationInstance(ORCHESTRATION_NAME, new NewOrchestrationInstanceOptions());

    assertFalse(capturedRequest.get().getEnforceUniqueInstanceId());
  }

  @Test
  void scheduleWithEnforceUniqueInstanceIdSetsRequestField() {
    NewOrchestrationInstanceOptions options = new NewOrchestrationInstanceOptions()
        .setInstanceId("myInstance")
        .setEnforceUniqueInstanceId(true);

    String instanceId = client.scheduleNewOrchestrationInstance(ORCHESTRATION_NAME, options);

    OrchestratorService.CreateInstanceRequest request = capturedRequest.get();
    assertEquals("myInstance", instanceId);
    assertEquals("myInstance", request.getInstanceId());
    assertTrue(request.getEnforceUniqueInstanceId());
  }

  @Test
  void scheduleWithEnforceUniqueInstanceIdSurfacesAlreadyExists() {
    responseStatus.set(Status.ALREADY_EXISTS.withDescription("a workflow with ID 'myInstance' already exists"));
    NewOrchestrationInstanceOptions options = new NewOrchestrationInstanceOptions()
        .setInstanceId("myInstance")
        .setEnforceUniqueInstanceId(true);

    StatusRuntimeException exception = assertThrows(StatusRuntimeException.class,
        () -> client.scheduleNewOrchestrationInstance(ORCHESTRATION_NAME, options));

    assertEquals(Status.Code.ALREADY_EXISTS, exception.getStatus().getCode());
    assertTrue(capturedRequest.get().getEnforceUniqueInstanceId());
  }
}
