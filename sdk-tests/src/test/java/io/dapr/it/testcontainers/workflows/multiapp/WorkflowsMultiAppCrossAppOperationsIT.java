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

package io.dapr.it.testcontainers.workflows.multiapp;

import io.dapr.config.Properties;
import io.dapr.it.testcontainers.ContainerConstants;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import io.dapr.testcontainers.DaprPlacementContainer;
import io.dapr.testcontainers.DaprSchedulerContainer;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.NewWorkflowOptions;
import io.dapr.workflows.client.WorkflowRuntimeStatus;
import io.dapr.workflows.client.WorkflowState;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_EDGE_IMAGE_TAG;
import static io.dapr.testcontainers.DaprContainerConstants.DAPR_PLACEMENT_IMAGE_TAG;
import static io.dapr.testcontainers.DaprContainerConstants.DAPR_SCHEDULER_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Cross-app workflow operations integration test.
 *
 * <p>Two apps share a placement and a scheduler on one Docker network:
 * <ul>
 *   <li>the caller app, which has no workflow worker at all</li>
 *   <li>the host app, whose worker is the only one that registers {@link CrossAppEventWorkflow}</li>
 * </ul>
 *
 * <p>A {@link DaprWorkflowClient} in the test JVM talks only to the caller app's sidecar and drives
 * the whole lifecycle of an instance owned by the host app, using the app ID overloads. Because the
 * workflow is registered only on the host app, an instance that actually runs proves the operations
 * were routed there rather than handled locally.
 */
@Testcontainers
@Tag("testcontainers")
public class WorkflowsMultiAppCrossAppOperationsIT {

  private static final String CALLER_APP_ID = "crossapp-caller";
  private static final String HOST_APP_ID = "crossapp-host";

  private static final Duration POLL_TIMEOUT = Duration.ofSeconds(60);

  private static final Network DAPR_NETWORK = Network.newNetwork();

  @Container
  private final static DaprPlacementContainer sharedPlacementContainer = new DaprPlacementContainer(DAPR_PLACEMENT_IMAGE_TAG)
      .withNetwork(DAPR_NETWORK)
      .withNetworkAliases("placement")
      .withReuse(false);

  @Container
  private final static DaprSchedulerContainer sharedSchedulerContainer = new DaprSchedulerContainer(DAPR_SCHEDULER_IMAGE_TAG)
      .withNetwork(DAPR_NETWORK)
      .withNetworkAliases("scheduler")
      .withReuse(false);

  // Caller app sidecar. The test JVM's workflow client connects to this one.
  //
  // Both sidecars run the edge image: cross-app client operations are not in any release yet, and a
  // release daprd ignores the app ID and applies every operation to the caller's own app, which
  // would make this test fail rather than skip. Placement and scheduler stay on the release images
  // because the feature lives entirely in daprd.
  @Container
  private final static DaprContainer CALLER_SIDECAR = new DaprContainer(DAPR_RUNTIME_EDGE_IMAGE_TAG)
      .withAppName(CALLER_APP_ID)
      .withNetwork(DAPR_NETWORK)
      .withNetworkAliases("caller-sidecar")
      .withPlacementContainer(sharedPlacementContainer)
      .withSchedulerContainer(sharedSchedulerContainer)
      .withComponent(new Component("kvstore", "state.in-memory", "v1", Map.of("actorStateStore", "true")))
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .dependsOn(sharedPlacementContainer, sharedSchedulerContainer)
      .withLogConsumer(outputFrame -> System.out.println("CALLER: " + outputFrame.getUtf8String()))
      .withAppChannelAddress("host.testcontainers.internal");

  // Host app sidecar. This is the app that owns and runs the workflow instances.
  @Container
  private final static DaprContainer HOST_SIDECAR = new DaprContainer(DAPR_RUNTIME_EDGE_IMAGE_TAG)
      .withAppName(HOST_APP_ID)
      .withNetwork(DAPR_NETWORK)
      .withNetworkAliases("host-sidecar")
      .withPlacementContainer(sharedPlacementContainer)
      .withSchedulerContainer(sharedSchedulerContainer)
      .withAppChannelAddress("caller-sidecar:3500")
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .dependsOn(sharedPlacementContainer, sharedSchedulerContainer, CALLER_SIDECAR)
      .withComponent(new Component("kvstore", "state.in-memory", "v1", Map.of("actorStateStore", "true")))
      .withLogConsumer(outputFrame -> System.out.println("HOST: " + outputFrame.getUtf8String()));

  // The only worker in the test: it registers CrossAppEventWorkflow for the host app.
  @Container
  private final static GenericContainer<?> hostWorker = new GenericContainer<>(ContainerConstants.JDK_17_TEMURIN_JAMMY)
      .withCopyFileToContainer(MountableFile.forHostPath("target"), "/app")
      .withWorkingDirectory("/app")
      .withCommand("java", "-cp", "test-classes:classes:dependency/*:*",
          "-Ddapr.app.id=" + HOST_APP_ID,
          "-Ddapr.grpc.endpoint=host-sidecar:50001",
          "-Ddapr.http.endpoint=host-sidecar:3500",
          "io.dapr.it.testcontainers.workflows.multiapp.CrossAppEventWorker")
      .withNetwork(DAPR_NETWORK)
      .dependsOn(HOST_SIDECAR)
      .waitingFor(Wait.forLogMessage(".*CrossAppEventWorker started.*", 1))
      .withLogConsumer(outputFrame -> System.out.println("HostWorker: " + outputFrame.getUtf8String()));

  @Test
  public void testCrossAppWorkflowLifecycle() throws Exception {
    try (DaprWorkflowClient workflowClient = callerWorkflowClient()) {
      String instanceId = workflowClient.scheduleNewWorkflow(CrossAppEventWorkflow.class,
          new NewWorkflowOptions().setAppId(HOST_APP_ID).setInput("Hello"));
      assertNotNull(instanceId, "Workflow instance ID should not be null");

      WorkflowState started = workflowClient.waitForWorkflowStart(instanceId, POLL_TIMEOUT, false, HOST_APP_ID);
      assertNotNull(started, "Workflow should have started on the host app");

      WorkflowState state = workflowClient.getWorkflowState(instanceId, true, HOST_APP_ID);
      assertNotNull(state, "Workflow state should be readable from the host app");
      assertEquals(CrossAppEventWorkflow.class.getCanonicalName(), state.getName());
      assertEquals(instanceId, state.getWorkflowId());

      assertNotHostedByCaller(workflowClient, instanceId);

      workflowClient.suspendWorkflow(instanceId, "pausing from the caller app", HOST_APP_ID);
      awaitStatus(workflowClient, instanceId, WorkflowRuntimeStatus.SUSPENDED);

      workflowClient.resumeWorkflow(instanceId, "resuming from the caller app", HOST_APP_ID);
      awaitStatus(workflowClient, instanceId, WorkflowRuntimeStatus.RUNNING);

      workflowClient.raiseEvent(instanceId, CrossAppEventWorkflow.CONTINUE_EVENT, "carry on", HOST_APP_ID);

      WorkflowState completed = workflowClient.waitForWorkflowCompletion(instanceId, POLL_TIMEOUT, true, HOST_APP_ID);
      assertNotNull(completed, "Workflow status should not be null");
      assertEquals(WorkflowRuntimeStatus.COMPLETED, completed.getRuntimeStatus());
      assertEquals("Hello [carry on] [hosted by " + HOST_APP_ID + "]", completed.readOutputAs(String.class),
          "The workflow must have run on the host app, not on the caller app");

      assertTrue(workflowClient.purgeWorkflow(instanceId, HOST_APP_ID),
          "Purging the host app's instance from the caller app should report a purged instance");
    }
  }

  @Test
  public void testCrossAppTerminate() throws Exception {
    try (DaprWorkflowClient workflowClient = callerWorkflowClient()) {
      String instanceId = workflowClient.scheduleNewWorkflow(CrossAppEventWorkflow.class,
          new NewWorkflowOptions().setAppId(HOST_APP_ID).setInput("Doomed"));
      assertNotNull(instanceId, "Workflow instance ID should not be null");

      workflowClient.waitForWorkflowStart(instanceId, POLL_TIMEOUT, false, HOST_APP_ID);
      assertNotHostedByCaller(workflowClient, instanceId);

      workflowClient.terminateWorkflow(instanceId, "terminated from the caller app", HOST_APP_ID);
      awaitStatus(workflowClient, instanceId, WorkflowRuntimeStatus.TERMINATED);

      assertTrue(workflowClient.purgeWorkflow(instanceId, HOST_APP_ID),
          "Purging the terminated instance from the caller app should report a purged instance");
    }
  }

  private static DaprWorkflowClient callerWorkflowClient() {
    Map<String, String> propertyOverrides = Map.of(
        "dapr.grpc.endpoint", CALLER_SIDECAR.getGrpcEndpoint(),
        "dapr.http.endpoint", CALLER_SIDECAR.getHttpEndpoint()
    );

    return new DaprWorkflowClient(new Properties(propertyOverrides));
  }

  /**
   * The two apps have their own state stores and their own workflow actor types, so the instance must
   * not be visible when the same client is used without a target app ID.
   */
  private static void assertNotHostedByCaller(DaprWorkflowClient workflowClient, String instanceId) {
    WorkflowState localState;
    try {
      localState = workflowClient.getWorkflowState(instanceId, false);
    } catch (RuntimeException e) {
      // A runtime that reports an unknown instance as an error is equally proof it is not local.
      return;
    }

    if (localState != null && localState.getName() != null && !localState.getName().isEmpty()) {
      fail("Instance " + instanceId + " should not exist on " + CALLER_APP_ID
          + " but the caller app reported it as " + localState.getName());
    }
  }

  private static void awaitStatus(DaprWorkflowClient workflowClient, String instanceId,
                                  WorkflowRuntimeStatus expected) throws InterruptedException {
    Instant deadline = Instant.now().plus(POLL_TIMEOUT);
    WorkflowRuntimeStatus observed = null;

    while (Instant.now().isBefore(deadline)) {
      WorkflowState state = workflowClient.getWorkflowState(instanceId, false, HOST_APP_ID);
      observed = state == null ? null : state.getRuntimeStatus();
      if (observed == expected) {
        return;
      }
      Thread.sleep(500);
    }

    fail("Timed out waiting for status " + expected + " on " + instanceId + ", last seen: " + observed);
  }
}
