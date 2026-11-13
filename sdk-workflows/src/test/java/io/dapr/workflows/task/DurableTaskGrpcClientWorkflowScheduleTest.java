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
import io.dapr.durabletask.implementation.protobuf.OrchestratorService;
import io.dapr.durabletask.implementation.protobuf.TaskHubSidecarServiceGrpc;
import io.dapr.durabletask.implementation.protobuf.WorkflowScheduleOuterClass;
import io.dapr.workflows.task.client.DurableTaskClient;
import io.dapr.workflows.task.client.DurableTaskGrpcClientBuilder;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DurableTaskGrpcClientWorkflowScheduleTest {

  private static final WorkflowScheduleOuterClass.WorkflowSchedule SCHEDULE =
      WorkflowScheduleOuterClass.WorkflowSchedule.newBuilder()
          .setName("nightly")
          .setWorkflowName("wf")
          .setSchedule("@daily")
          .build();

  private Server server;
  private ManagedChannel channel;
  private DurableTaskClient client;

  private final AtomicReference<Object> request = new AtomicReference<>();

  @BeforeEach
  void setUp() throws Exception {
    String serverName = InProcessServerBuilder.generateName();
    server = InProcessServerBuilder.forName(serverName)
        .directExecutor()
        .addService(new TaskHubSidecarServiceGrpc.TaskHubSidecarServiceImplBase() {
          @Override
          public void createWorkflowSchedule(
              WorkflowScheduleOuterClass.CreateWorkflowScheduleRequest req,
              StreamObserver<WorkflowScheduleOuterClass.CreateWorkflowScheduleResponse> observer) {
            reply(req, WorkflowScheduleOuterClass.CreateWorkflowScheduleResponse.getDefaultInstance(), observer);
          }

          @Override
          public void getWorkflowSchedule(
              WorkflowScheduleOuterClass.GetWorkflowScheduleRequest req,
              StreamObserver<WorkflowScheduleOuterClass.GetWorkflowScheduleResponse> observer) {
            if (req.getName().equals("missing")) {
              observer.onError(Status.NOT_FOUND.asRuntimeException());
              return;
            }
            reply(req, WorkflowScheduleOuterClass.GetWorkflowScheduleResponse.newBuilder()
                .setSchedule(SCHEDULE).build(), observer);
          }

          @Override
          public void listWorkflowSchedules(
              WorkflowScheduleOuterClass.ListWorkflowSchedulesRequest req,
              StreamObserver<WorkflowScheduleOuterClass.ListWorkflowSchedulesResponse> observer) {
            reply(req, WorkflowScheduleOuterClass.ListWorkflowSchedulesResponse.newBuilder()
                .addSchedules(SCHEDULE).setContinuationToken("next").build(), observer);
          }

          @Override
          public void deleteWorkflowSchedule(
              WorkflowScheduleOuterClass.DeleteWorkflowScheduleRequest req,
              StreamObserver<WorkflowScheduleOuterClass.DeleteWorkflowScheduleResponse> observer) {
            reply(req, WorkflowScheduleOuterClass.DeleteWorkflowScheduleResponse.getDefaultInstance(), observer);
          }

          @Override
          public void triggerWorkflowSchedule(
              WorkflowScheduleOuterClass.TriggerWorkflowScheduleRequest req,
              StreamObserver<WorkflowScheduleOuterClass.TriggerWorkflowScheduleResponse> observer) {
            reply(req, WorkflowScheduleOuterClass.TriggerWorkflowScheduleResponse.newBuilder()
                .setInstanceId(req.getName() + "_20260101T000000Z_manual").build(), observer);
          }

          @Override
          public void listInstanceIDs(
              OrchestratorService.ListInstanceIDsRequest req,
              StreamObserver<OrchestratorService.ListInstanceIDsResponse> observer) {
            reply(req, OrchestratorService.ListInstanceIDsResponse.getDefaultInstance(), observer);
          }

          private <T> void reply(Object req, T response, StreamObserver<T> observer) {
            request.set(req);
            observer.onNext(response);
            observer.onCompleted();
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
  void createSerializesInputAndClearsStatus() {
    WorkflowScheduleOuterClass.WorkflowSchedule schedule = SCHEDULE.toBuilder()
        .setInput(StringValue.of("ignored"))
        .setStatus(WorkflowScheduleOuterClass.WorkflowScheduleStatus.newBuilder().setRunCount(3))
        .build();

    client.createWorkflowSchedule(schedule, "payload", true);

    WorkflowScheduleOuterClass.CreateWorkflowScheduleRequest req =
        (WorkflowScheduleOuterClass.CreateWorkflowScheduleRequest) request.get();
    assertTrue(req.getOverwrite());
    assertEquals("nightly", req.getSchedule().getName());
    assertEquals("\"payload\"", req.getSchedule().getInput().getValue());
    assertFalse(req.getSchedule().hasStatus());
  }

  @Test
  void createWithoutInputLeavesInputUnset() {
    client.createWorkflowSchedule(SCHEDULE, null, false);

    WorkflowScheduleOuterClass.CreateWorkflowScheduleRequest req =
        (WorkflowScheduleOuterClass.CreateWorkflowScheduleRequest) request.get();
    assertFalse(req.getOverwrite());
    assertFalse(req.getSchedule().hasInput());
  }

  @Test
  void createSendsSkipMissed() {
    client.createWorkflowSchedule(SCHEDULE.toBuilder().setSkipMissed(true).build(), null, false);

    WorkflowScheduleOuterClass.CreateWorkflowScheduleRequest req =
        (WorkflowScheduleOuterClass.CreateWorkflowScheduleRequest) request.get();
    assertTrue(req.getSchedule().hasSkipMissed());
    assertTrue(req.getSchedule().getSkipMissed());
  }

  @Test
  void getReturnsSchedule() {
    assertEquals(SCHEDULE, client.getWorkflowSchedule("nightly"));
    assertEquals("nightly", ((WorkflowScheduleOuterClass.GetWorkflowScheduleRequest) request.get()).getName());
  }

  @Test
  void getSurfacesGrpcStatus() {
    StatusRuntimeException e = assertThrows(StatusRuntimeException.class,
        () -> client.getWorkflowSchedule("missing"));
    assertEquals(Status.Code.NOT_FOUND, e.getStatus().getCode());
  }

  @Test
  void listSendsPaging() {
    WorkflowScheduleOuterClass.ListWorkflowSchedulesResponse response = client.listWorkflowSchedules("tok", 5);

    WorkflowScheduleOuterClass.ListWorkflowSchedulesRequest req =
        (WorkflowScheduleOuterClass.ListWorkflowSchedulesRequest) request.get();
    assertEquals("tok", req.getContinuationToken());
    assertEquals(5, req.getPageSize());
    assertEquals(1, response.getSchedulesCount());
    assertEquals("next", response.getContinuationToken());
  }

  @Test
  void listWithoutPagingLeavesFieldsUnset() {
    client.listWorkflowSchedules(null, null);

    WorkflowScheduleOuterClass.ListWorkflowSchedulesRequest req =
        (WorkflowScheduleOuterClass.ListWorkflowSchedulesRequest) request.get();
    assertFalse(req.hasContinuationToken());
    assertFalse(req.hasPageSize());
  }

  @Test
  void listRejectsNonPositivePageSize() {
    assertThrows(IllegalArgumentException.class, () -> client.listWorkflowSchedules(null, 0));
    assertNull(request.get());
  }

  @Test
  void deleteSendsName() {
    client.deleteWorkflowSchedule("nightly");
    assertEquals("nightly", ((WorkflowScheduleOuterClass.DeleteWorkflowScheduleRequest) request.get()).getName());
  }

  @Test
  void triggerReturnsInstanceId() {
    assertEquals("nightly_20260101T000000Z_manual", client.triggerWorkflowSchedule("nightly"));
  }

  @Test
  void listInstanceIdsSendsPrefix() {
    client.listInstanceIds(null, null, "nightly_");
    assertEquals("nightly_",
        ((OrchestratorService.ListInstanceIDsRequest) request.get()).getInstanceIdPrefix());
  }

  @Test
  void listInstanceIdsWithoutPrefixLeavesItUnset() {
    client.listInstanceIds(null, null);
    assertFalse(((OrchestratorService.ListInstanceIDsRequest) request.get()).hasInstanceIdPrefix());
  }
}
