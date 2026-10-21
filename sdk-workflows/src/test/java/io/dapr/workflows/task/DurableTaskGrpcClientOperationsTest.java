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

import com.google.protobuf.StringValue;
import io.dapr.durabletask.implementation.protobuf.HistoryEvents;
import io.dapr.durabletask.implementation.protobuf.Orchestration;
import io.dapr.durabletask.implementation.protobuf.OrchestratorService;
import io.dapr.durabletask.implementation.protobuf.TaskHubSidecarServiceGrpc;
import io.dapr.workflows.client.WorkflowRuntimeStatus;
import io.dapr.workflows.task.client.DurableTaskClient;
import io.dapr.workflows.task.client.DurableTaskGrpcClientBuilder;
import io.dapr.workflows.task.client.PurgeInstanceCriteria;
import io.dapr.workflows.task.client.PurgeResult;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the management operations of {@link io.dapr.workflows.task.client.DurableTaskGrpcClient}
 * against an in-process sidecar: what each one puts on the wire, and what it makes of the response.
 */
class DurableTaskGrpcClientOperationsTest {

  private static final String INSTANCE_ID = "instance-1";
  private static final String ORCHESTRATION_NAME = "DemoWorkflow";

  private Server server;
  private ManagedChannel channel;
  private DurableTaskClient client;

  private final AtomicReference<OrchestratorService.PurgeInstancesRequest> purgeRequest = new AtomicReference<>();
  private final AtomicReference<OrchestratorService.ListInstanceIDsRequest> listRequest = new AtomicReference<>();
  private final AtomicReference<OrchestratorService.GetInstanceHistoryRequest> historyRequest =
      new AtomicReference<>();
  private final AtomicReference<OrchestratorService.RerunWorkflowFromEventRequest> rerunRequest =
      new AtomicReference<>();
  private final AtomicReference<OrchestratorService.CreateInstanceRequest> createRequest = new AtomicReference<>();

  @BeforeEach
  void setUp() throws Exception {
    String serverName = InProcessServerBuilder.generateName();
    server = InProcessServerBuilder.forName(serverName)
        .directExecutor()
        .addService(new TaskHubSidecarServiceGrpc.TaskHubSidecarServiceImplBase() {
          @Override
          public void purgeInstances(
              OrchestratorService.PurgeInstancesRequest request,
              StreamObserver<OrchestratorService.PurgeInstancesResponse> responseObserver) {
            purgeRequest.set(request);
            responseObserver.onNext(OrchestratorService.PurgeInstancesResponse.newBuilder()
                .setDeletedInstanceCount(2)
                .build());
            responseObserver.onCompleted();
          }

          @Override
          public void listInstanceIDs(
              OrchestratorService.ListInstanceIDsRequest request,
              StreamObserver<OrchestratorService.ListInstanceIDsResponse> responseObserver) {
            listRequest.set(request);
            responseObserver.onNext(OrchestratorService.ListInstanceIDsResponse.newBuilder()
                .addInstanceIds("a")
                .addInstanceIds("b")
                .setContinuationToken("next-page")
                .build());
            responseObserver.onCompleted();
          }

          @Override
          public void getInstanceHistory(
              OrchestratorService.GetInstanceHistoryRequest request,
              StreamObserver<OrchestratorService.GetInstanceHistoryResponse> responseObserver) {
            historyRequest.set(request);
            responseObserver.onNext(OrchestratorService.GetInstanceHistoryResponse.newBuilder()
                .addEvents(HistoryEvents.HistoryEvent.newBuilder().setEventId(1))
                .addEvents(HistoryEvents.HistoryEvent.newBuilder().setEventId(2))
                .build());
            responseObserver.onCompleted();
          }

          @Override
          public void rerunWorkflowFromEvent(
              OrchestratorService.RerunWorkflowFromEventRequest request,
              StreamObserver<OrchestratorService.RerunWorkflowFromEventResponse> responseObserver) {
            rerunRequest.set(request);
            responseObserver.onNext(OrchestratorService.RerunWorkflowFromEventResponse.newBuilder()
                .setNewInstanceID("rerun-1")
                .build());
            responseObserver.onCompleted();
          }

          @Override
          public void getInstance(
              OrchestratorService.GetInstanceRequest request,
              StreamObserver<OrchestratorService.GetInstanceResponse> responseObserver) {
            boolean exists = INSTANCE_ID.equals(request.getInstanceId());
            OrchestratorService.GetInstanceResponse.Builder response =
                OrchestratorService.GetInstanceResponse.newBuilder().setExists(exists);
            if (exists) {
              response.setWorkflowState(Orchestration.WorkflowState.newBuilder()
                  .setInstanceId(INSTANCE_ID)
                  .setName(ORCHESTRATION_NAME)
                  .setWorkflowStatus(Orchestration.OrchestrationStatus.ORCHESTRATION_STATUS_COMPLETED)
                  .setInput(StringValue.of("\"the input\"")));
            }
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
          }

          @Override
          public void startInstance(
              OrchestratorService.CreateInstanceRequest request,
              StreamObserver<OrchestratorService.CreateInstanceResponse> responseObserver) {
            createRequest.set(request);
            responseObserver.onNext(OrchestratorService.CreateInstanceResponse.newBuilder()
                .setInstanceId(request.getInstanceId().isEmpty() ? "generated-1" : request.getInstanceId())
                .build());
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
  void shouldReportHowManyInstancesAPurgeDeleted() {
    PurgeResult result = client.purgeInstance(INSTANCE_ID);

    assertEquals(INSTANCE_ID, purgeRequest.get().getInstanceId());
    assertEquals(2, result.getDeletedInstanceCount());
  }

  @Test
  void shouldTurnPurgeCriteriaIntoAFilter() throws Exception {
    Instant from = Instant.parse("2026-01-01T00:00:00Z");
    Instant to = Instant.parse("2026-02-01T00:00:00Z");

    client.purgeInstances(new PurgeInstanceCriteria()
        .setCreatedTimeFrom(from)
        .setCreatedTimeTo(to)
        .setRuntimeStatusList(Arrays.asList(WorkflowRuntimeStatus.COMPLETED, WorkflowRuntimeStatus.FAILED))
        .setTimeout(Duration.ofSeconds(30)));

    OrchestratorService.PurgeInstanceFilter filter = purgeRequest.get().getPurgeInstanceFilter();
    assertEquals(from.getEpochSecond(), filter.getCreatedTimeFrom().getSeconds());
    assertEquals(to.getEpochSecond(), filter.getCreatedTimeTo().getSeconds());
    assertEquals(2, filter.getRuntimeStatusCount());
  }

  @Test
  void shouldLeaveTheEndTimeUnsetWhenTheCriteriaHasNone() throws Exception {
    client.purgeInstances(new PurgeInstanceCriteria()
        .setCreatedTimeFrom(Instant.parse("2026-01-01T00:00:00Z")));

    assertFalse(purgeRequest.get().getPurgeInstanceFilter().hasCreatedTimeTo());
    assertEquals(0, purgeRequest.get().getPurgeInstanceFilter().getRuntimeStatusCount());
  }

  @Test
  void shouldListInstanceIdsWithTheContinuationTokenAndPageSizeAsked() {
    OrchestratorService.ListInstanceIDsResponse response = client.listInstanceIds("token-1", 50);

    assertEquals("token-1", listRequest.get().getContinuationToken());
    assertEquals(50, listRequest.get().getPageSize());
    assertEquals(Arrays.asList("a", "b"), response.getInstanceIdsList());
    assertEquals("next-page", response.getContinuationToken());
  }

  @Test
  void shouldAskForTheFirstPageWhenNoPagingIsGiven() {
    client.listInstanceIds(null, null);

    assertTrue(listRequest.get().getContinuationToken().isEmpty());
    assertEquals(0, listRequest.get().getPageSize());
  }

  @Test
  void shouldRejectAPageSizeThatIsNotPositive() {
    assertThrows(IllegalArgumentException.class, () -> client.listInstanceIds(null, 0));
    assertThrows(IllegalArgumentException.class, () -> client.listInstanceIds(null, -1));
  }

  @Test
  void shouldReturnTheHistoryEventsForAnInstance() {
    List<HistoryEvents.HistoryEvent> history = client.getInstanceHistory(INSTANCE_ID);

    assertEquals(INSTANCE_ID, historyRequest.get().getInstanceId());
    assertEquals(2, history.size());
    assertEquals(1, history.get(0).getEventId());
  }

  @Test
  void shouldRequireAnInstanceIdToFetchHistory() {
    assertThrows(IllegalArgumentException.class, () -> client.getInstanceHistory(null));
  }

  @Test
  void shouldRerunFromAnEventAndReturnTheNewInstanceId() {
    String newInstanceId = client.rerunWorkflowFromEvent(INSTANCE_ID, 5, "chosen-id", "new input", true);

    assertEquals("rerun-1", newInstanceId);
    assertEquals(INSTANCE_ID, rerunRequest.get().getSourceInstanceID());
    assertEquals(5, rerunRequest.get().getEventID());
    assertEquals("chosen-id", rerunRequest.get().getNewInstanceID());
    assertTrue(rerunRequest.get().getOverwriteInput());
    assertEquals("\"new input\"", rerunRequest.get().getInput().getValue());
  }

  @Test
  void shouldNotSendAnInputWhenTheRerunKeepsTheOriginalOne() {
    client.rerunWorkflowFromEvent(INSTANCE_ID, 5, null, "ignored", false);

    assertFalse(rerunRequest.get().getOverwriteInput());
    assertFalse(rerunRequest.get().hasInput(), "the input is only sent when it overwrites the original");
    assertTrue(rerunRequest.get().getNewInstanceID().isEmpty());
  }

  @Test
  void shouldRequireASourceInstanceToRerunFrom() {
    assertThrows(IllegalArgumentException.class,
        () -> client.rerunWorkflowFromEvent(null, 5, null, null, false));
  }

  @Test
  void shouldRestartAnInstanceReusingItsNameAndInput() {
    String instanceId = client.restartInstance(INSTANCE_ID, false);

    assertEquals(ORCHESTRATION_NAME, createRequest.get().getName());
    assertEquals(INSTANCE_ID, createRequest.get().getInstanceId(),
        "restarting without a new ID reuses the original instance ID");
    assertEquals(INSTANCE_ID, instanceId);
  }

  @Test
  void shouldRestartAnInstanceUnderAFreshIdWhenAsked() {
    String instanceId = client.restartInstance(INSTANCE_ID, true);

    assertFalse(createRequest.get().getInstanceId().isEmpty());
    assertNotEquals(INSTANCE_ID, createRequest.get().getInstanceId(),
        "restarting with a new ID must not reuse the original one");
    assertEquals(createRequest.get().getInstanceId(), instanceId);
  }

  @Test
  void shouldRefuseToRestartAnInstanceThatDoesNotExist() {
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
        () -> client.restartInstance("missing-instance", false));

    assertTrue(thrown.getMessage().contains("missing-instance"), thrown.getMessage());
  }
}
