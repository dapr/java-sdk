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

import io.dapr.durabletask.implementation.protobuf.OrchestratorService;
import io.dapr.durabletask.implementation.protobuf.TaskHubSidecarServiceGrpc;
import io.dapr.workflows.task.client.DurableTaskClient;
import io.dapr.workflows.task.client.DurableTaskGrpcClientBuilder;
import io.dapr.workflows.task.client.NewOrchestrationInstanceOptions;
import io.dapr.workflows.task.client.PurgeInstanceCriteria;
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

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers how DurableTaskGrpcClient maps request options and sidecar errors, and how it fails
 * when its TLS files cannot be read.
 */
class DurableTaskGrpcClientErrorPathsTest {

  private Server server;
  private ManagedChannel channel;
  private DurableTaskClient client;
  private final AtomicReference<Status> status = new AtomicReference<>(Status.OK);
  private final AtomicReference<OrchestratorService.CreateInstanceRequest> createRequest = new AtomicReference<>();
  private final AtomicReference<OrchestratorService.TerminateRequest> terminateRequest = new AtomicReference<>();

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
            createRequest.set(request);
            responseObserver.onNext(OrchestratorService.CreateInstanceResponse.newBuilder()
                .setInstanceId(request.getInstanceId())
                .build());
            responseObserver.onCompleted();
          }

          @Override
          public void waitForInstanceStart(
              OrchestratorService.GetInstanceRequest request,
              StreamObserver<OrchestratorService.GetInstanceResponse> responseObserver) {
            responseObserver.onError(status.get().asRuntimeException());
          }

          @Override
          public void waitForInstanceCompletion(
              OrchestratorService.GetInstanceRequest request,
              StreamObserver<OrchestratorService.GetInstanceResponse> responseObserver) {
            responseObserver.onError(status.get().asRuntimeException());
          }

          @Override
          public void purgeInstances(
              OrchestratorService.PurgeInstancesRequest request,
              StreamObserver<OrchestratorService.PurgeInstancesResponse> responseObserver) {
            responseObserver.onError(status.get().asRuntimeException());
          }

          @Override
          public void terminateInstance(
              OrchestratorService.TerminateRequest request,
              StreamObserver<OrchestratorService.TerminateResponse> responseObserver) {
            terminateRequest.set(request);
            responseObserver.onNext(OrchestratorService.TerminateResponse.getDefaultInstance());
            responseObserver.onCompleted();
          }
        })
        .build()
        .start();
    channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
    client = new DurableTaskGrpcClientBuilder().grpcChannel(channel).build();
  }

  @AfterEach
  void tearDown() throws Exception {
    client.close();
    channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
  }

  @Test
  void scheduleRejectsAnEmptyOrchestratorName() {
    assertThrows(IllegalArgumentException.class,
        () -> client.scheduleNewOrchestrationInstance("", new NewOrchestrationInstanceOptions()));
  }

  @Test
  void scheduleSendsVersionAndStartTime() {
    Instant startTime = Instant.ofEpochSecond(1_700_000_000L);

    client.scheduleNewOrchestrationInstance("Orchestration", new NewOrchestrationInstanceOptions()
        .setVersion("v2")
        .setStartTime(startTime));

    OrchestratorService.CreateInstanceRequest request = createRequest.get();
    assertEquals("v2", request.getVersion().getValue());
    assertEquals(startTime.getEpochSecond(), request.getScheduledStartTimestamp().getSeconds());
  }

  @Test
  void terminateSendsTheSerializedOutput() {
    client.terminate("instance", "done");

    assertEquals("\"done\"", terminateRequest.get().getOutput().getValue());
  }

  @Test
  void waitForInstanceStartMapsDeadlineExceededToTimeout() {
    status.set(Status.DEADLINE_EXCEEDED);

    assertThrows(TimeoutException.class, () -> client.waitForInstanceStart("instance", null, false));
  }

  @Test
  void waitForInstanceStartRethrowsOtherErrors() {
    status.set(Status.INTERNAL);

    StatusRuntimeException thrown = assertThrows(StatusRuntimeException.class,
        () -> client.waitForInstanceStart("instance", Duration.ofSeconds(5), false));
    assertEquals(Status.Code.INTERNAL, thrown.getStatus().getCode());
  }

  @Test
  void waitForInstanceCompletionMapsDeadlineExceededToTimeout() {
    status.set(Status.DEADLINE_EXCEEDED);

    assertThrows(TimeoutException.class, () -> client.waitForInstanceCompletion("instance", null, false));
  }

  @Test
  void waitForInstanceCompletionRethrowsOtherErrors() {
    status.set(Status.INTERNAL);

    StatusRuntimeException thrown = assertThrows(StatusRuntimeException.class,
        () -> client.waitForInstanceCompletion("instance", Duration.ofSeconds(5), false));
    assertEquals(Status.Code.INTERNAL, thrown.getStatus().getCode());
  }

  @Test
  void purgeInstancesMapsDeadlineExceededToTimeout() {
    status.set(Status.DEADLINE_EXCEEDED);
    PurgeInstanceCriteria criteria = new PurgeInstanceCriteria().setCreatedTimeFrom(Instant.EPOCH);

    TimeoutException thrown = assertThrows(TimeoutException.class, () -> client.purgeInstances(criteria));
    assertTrue(thrown.getMessage().contains("PT4M"), thrown.getMessage());
  }

  @Test
  void purgeInstancesRethrowsOtherErrors() {
    status.set(Status.INTERNAL);
    PurgeInstanceCriteria criteria = new PurgeInstanceCriteria()
        .setCreatedTimeFrom(Instant.EPOCH)
        .setTimeout(Duration.ofSeconds(5));

    StatusRuntimeException thrown = assertThrows(StatusRuntimeException.class,
        () -> client.purgeInstances(criteria));
    assertEquals(Status.Code.INTERNAL, thrown.getStatus().getCode());
  }

  @Test
  void insecureModeBuildsAClient() {
    DurableTaskClient insecureClient = new DurableTaskGrpcClientBuilder().insecure(true).build();
    insecureClient.close();
  }

  @Test
  void missingClientCertificateFailsTheBuild() {
    RuntimeException thrown = assertThrows(RuntimeException.class, () -> new DurableTaskGrpcClientBuilder()
        .tlsCertPath("/nonexistent/client.crt")
        .tlsKeyPath("/nonexistent/client.key")
        .tlsCaPath("/nonexistent/ca.crt")
        .build());
    assertEquals("Failed to create mTLS credentials with CA cert", thrown.getMessage());
  }

  @Test
  void missingCaCertificateFailsTheBuild() {
    RuntimeException thrown = assertThrows(RuntimeException.class, () -> new DurableTaskGrpcClientBuilder()
        .tlsCaPath("/nonexistent/ca.crt")
        .build());
    assertEquals("Failed to create TLS credentials with CA cert", thrown.getMessage());
  }
}
