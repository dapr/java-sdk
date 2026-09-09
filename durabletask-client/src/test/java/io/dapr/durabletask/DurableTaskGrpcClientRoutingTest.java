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
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that client operations carry a router with the target app ID when an
 * app ID is provided (cross-app routing), and no router otherwise. Only the
 * target app ID is set by the client; the sidecar stamps the source app ID.
 */
class DurableTaskGrpcClientRoutingTest {

  private static final String INSTANCE_ID = "testInstanceId";
  private static final String APP_ID = "targetApp";
  private static final String ORCHESTRATION_NAME = "TestOrchestration";

  // Returned by the getInstance RPC so that restartInstance finds an instance to reschedule.
  private static final OrchestratorService.GetInstanceResponse EXISTING_INSTANCE =
      OrchestratorService.GetInstanceResponse.newBuilder()
          .setExists(true)
          .setWorkflowState(Orchestration.WorkflowState.newBuilder()
              .setInstanceId(INSTANCE_ID)
              .setName(ORCHESTRATION_NAME)
              .setInput(StringValue.of("\"payload\"")))
          .build();

  private Server server;
  private ManagedChannel channel;
  private DurableTaskClient client;

  private final AtomicReference<OrchestratorService.CreateInstanceRequest> createRequest = new AtomicReference<>();
  private final AtomicReference<OrchestratorService.RaiseEventRequest> raiseEventRequest = new AtomicReference<>();
  private final AtomicReference<OrchestratorService.GetInstanceRequest> getInstanceRequest = new AtomicReference<>();
  private final AtomicReference<OrchestratorService.TerminateRequest> terminateRequest = new AtomicReference<>();
  private final AtomicReference<OrchestratorService.SuspendRequest> suspendRequest = new AtomicReference<>();
  private final AtomicReference<OrchestratorService.ResumeRequest> resumeRequest = new AtomicReference<>();
  private final AtomicReference<OrchestratorService.PurgeInstancesRequest> purgeRequest = new AtomicReference<>();

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
          public void raiseEvent(
              OrchestratorService.RaiseEventRequest request,
              StreamObserver<OrchestratorService.RaiseEventResponse> responseObserver) {
            raiseEventRequest.set(request);
            responseObserver.onNext(OrchestratorService.RaiseEventResponse.getDefaultInstance());
            responseObserver.onCompleted();
          }

          @Override
          public void getInstance(
              OrchestratorService.GetInstanceRequest request,
              StreamObserver<OrchestratorService.GetInstanceResponse> responseObserver) {
            getInstanceRequest.set(request);
            responseObserver.onNext(EXISTING_INSTANCE);
            responseObserver.onCompleted();
          }

          @Override
          public void waitForInstanceStart(
              OrchestratorService.GetInstanceRequest request,
              StreamObserver<OrchestratorService.GetInstanceResponse> responseObserver) {
            getInstanceRequest.set(request);
            responseObserver.onNext(OrchestratorService.GetInstanceResponse.getDefaultInstance());
            responseObserver.onCompleted();
          }

          @Override
          public void waitForInstanceCompletion(
              OrchestratorService.GetInstanceRequest request,
              StreamObserver<OrchestratorService.GetInstanceResponse> responseObserver) {
            getInstanceRequest.set(request);
            responseObserver.onNext(OrchestratorService.GetInstanceResponse.getDefaultInstance());
            responseObserver.onCompleted();
          }

          @Override
          public void terminateInstance(
              OrchestratorService.TerminateRequest request,
              StreamObserver<OrchestratorService.TerminateResponse> responseObserver) {
            terminateRequest.set(request);
            responseObserver.onNext(OrchestratorService.TerminateResponse.getDefaultInstance());
            responseObserver.onCompleted();
          }

          @Override
          public void suspendInstance(
              OrchestratorService.SuspendRequest request,
              StreamObserver<OrchestratorService.SuspendResponse> responseObserver) {
            suspendRequest.set(request);
            responseObserver.onNext(OrchestratorService.SuspendResponse.getDefaultInstance());
            responseObserver.onCompleted();
          }

          @Override
          public void resumeInstance(
              OrchestratorService.ResumeRequest request,
              StreamObserver<OrchestratorService.ResumeResponse> responseObserver) {
            resumeRequest.set(request);
            responseObserver.onNext(OrchestratorService.ResumeResponse.getDefaultInstance());
            responseObserver.onCompleted();
          }

          @Override
          public void purgeInstances(
              OrchestratorService.PurgeInstancesRequest request,
              StreamObserver<OrchestratorService.PurgeInstancesResponse> responseObserver) {
            purgeRequest.set(request);
            responseObserver.onNext(OrchestratorService.PurgeInstancesResponse.getDefaultInstance());
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

  private static void assertTargetsApp(Orchestration.TaskRouter router) {
    assertEquals(APP_ID, router.getTargetAppID());
    // The client must not stamp the source app ID; the sidecar does.
    assertEquals("", router.getSourceAppID());
    assertFalse(router.hasTargetAppNamespace());
  }

  @Test
  void scheduleWithoutAppIdDoesNotSetRouter() {
    client.scheduleNewOrchestrationInstance("TestOrchestration", new NewOrchestrationInstanceOptions());

    assertFalse(createRequest.get().hasRouter());
  }

  @Test
  void scheduleWithAppIdSetsRouter() {
    client.scheduleNewOrchestrationInstance("TestOrchestration",
        new NewOrchestrationInstanceOptions().setAppID(APP_ID));

    assertTrue(createRequest.get().hasRouter());
    assertTargetsApp(createRequest.get().getRouter());
  }

  @Test
  void raiseEventWithoutAppIdDoesNotSetRouter() {
    client.raiseEvent(INSTANCE_ID, "testEvent", "payload");

    assertFalse(raiseEventRequest.get().hasRouter());
  }

  @Test
  void raiseEventWithAppIdSetsRouter() {
    client.raiseEvent(INSTANCE_ID, "testEvent", "payload", APP_ID);

    assertTrue(raiseEventRequest.get().hasRouter());
    assertTargetsApp(raiseEventRequest.get().getRouter());
  }

  @Test
  void getInstanceMetadataWithoutAppIdDoesNotSetRouter() {
    client.getInstanceMetadata(INSTANCE_ID, true);

    assertFalse(getInstanceRequest.get().hasRouter());
  }

  @Test
  void getInstanceMetadataWithAppIdSetsRouter() {
    client.getInstanceMetadata(INSTANCE_ID, true, APP_ID);

    assertTrue(getInstanceRequest.get().hasRouter());
    assertTargetsApp(getInstanceRequest.get().getRouter());
  }

  @Test
  void waitForInstanceStartWithoutAppIdDoesNotSetRouter() throws Exception {
    client.waitForInstanceStart(INSTANCE_ID, Duration.ofSeconds(5), false);

    assertFalse(getInstanceRequest.get().hasRouter());
  }

  @Test
  void waitForInstanceStartWithAppIdSetsRouter() throws Exception {
    client.waitForInstanceStart(INSTANCE_ID, Duration.ofSeconds(5), false, APP_ID);

    assertTrue(getInstanceRequest.get().hasRouter());
    assertTargetsApp(getInstanceRequest.get().getRouter());
  }

  @Test
  void waitForInstanceCompletionWithoutAppIdDoesNotSetRouter() throws Exception {
    client.waitForInstanceCompletion(INSTANCE_ID, Duration.ofSeconds(5), false);

    assertFalse(getInstanceRequest.get().hasRouter());
  }

  @Test
  void waitForInstanceCompletionWithAppIdSetsRouter() throws Exception {
    client.waitForInstanceCompletion(INSTANCE_ID, Duration.ofSeconds(5), false, APP_ID);

    assertTrue(getInstanceRequest.get().hasRouter());
    assertTargetsApp(getInstanceRequest.get().getRouter());
  }

  @Test
  void terminateWithoutAppIdDoesNotSetRouter() {
    client.terminate(INSTANCE_ID, null);

    assertFalse(terminateRequest.get().hasRouter());
  }

  @Test
  void terminateWithAppIdSetsRouter() {
    client.terminate(INSTANCE_ID, null, APP_ID);

    assertTrue(terminateRequest.get().hasRouter());
    assertTargetsApp(terminateRequest.get().getRouter());
  }

  @Test
  void suspendWithoutAppIdDoesNotSetRouter() {
    client.suspendInstance(INSTANCE_ID, "reason");

    assertFalse(suspendRequest.get().hasRouter());
  }

  @Test
  void suspendWithAppIdSetsRouter() {
    client.suspendInstance(INSTANCE_ID, "reason", APP_ID);

    assertTrue(suspendRequest.get().hasRouter());
    assertTargetsApp(suspendRequest.get().getRouter());
  }

  @Test
  void resumeWithoutAppIdDoesNotSetRouter() {
    client.resumeInstance(INSTANCE_ID, "reason");

    assertFalse(resumeRequest.get().hasRouter());
  }

  @Test
  void resumeWithAppIdSetsRouter() {
    client.resumeInstance(INSTANCE_ID, "reason", APP_ID);

    assertTrue(resumeRequest.get().hasRouter());
    assertTargetsApp(resumeRequest.get().getRouter());
  }

  @Test
  void purgeInstanceWithoutAppIdDoesNotSetRouter() {
    client.purgeInstance(INSTANCE_ID);

    assertFalse(purgeRequest.get().hasRouter());
  }

  @Test
  void purgeInstanceWithAppIdSetsRouter() {
    client.purgeInstance(INSTANCE_ID, APP_ID);

    assertTrue(purgeRequest.get().hasRouter());
    assertTargetsApp(purgeRequest.get().getRouter());
  }

  /**
   * restartInstance reschedules the instance, so it issues two requests: the metadata lookup for the
   * original instance and the create request for the replacement. Both must stay on the local app.
   */
  @Test
  void restartInstanceWithoutAppIdDoesNotSetRouterOnEitherRequest() {
    client.restartInstance(INSTANCE_ID, false);

    assertFalse(getInstanceRequest.get().hasRouter());
    assertFalse(createRequest.get().hasRouter());
  }

  /**
   * Both requests issued by restartInstance must carry the router, otherwise the replacement instance
   * would be created on the calling app instead of the app that owns the original instance.
   */
  @Test
  void restartInstanceWithAppIdSetsRouterOnBothRequests() {
    client.restartInstance(INSTANCE_ID, false, APP_ID);

    assertTrue(getInstanceRequest.get().hasRouter());
    assertTargetsApp(getInstanceRequest.get().getRouter());

    assertTrue(createRequest.get().hasRouter());
    assertTargetsApp(createRequest.get().getRouter());
  }

  @Test
  void restartInstanceWithNewInstanceIdWithAppIdSetsRouterOnBothRequests() {
    client.restartInstance(INSTANCE_ID, true, APP_ID);

    assertTrue(getInstanceRequest.get().hasRouter());
    assertTargetsApp(getInstanceRequest.get().getRouter());

    assertTrue(createRequest.get().hasRouter());
    assertTargetsApp(createRequest.get().getRouter());
  }
}
