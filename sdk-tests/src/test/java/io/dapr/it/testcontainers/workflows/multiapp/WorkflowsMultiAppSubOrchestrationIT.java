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

import io.dapr.it.testcontainers.ContainerConstants;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import io.dapr.testcontainers.DaprPlacementContainer;
import io.dapr.testcontainers.DaprSchedulerContainer;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowState;
import io.dapr.workflows.client.WorkflowRuntimeStatus;
import io.dapr.config.Properties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.util.Map;

import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static io.dapr.testcontainers.DaprContainerConstants.DAPR_PLACEMENT_IMAGE_TAG;
import static io.dapr.testcontainers.DaprContainerConstants.DAPR_SCHEDULER_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Multi-App Sub-Orchestration integration test.
 *
 * This test demonstrates cross-app sub-orchestration by:
 * 1. Starting 2 Dapr containers (suborchestration-parent, app2)
 * 2. Launching Java processes that register workflows in separate apps
 * 3. The parent workflow calls a child workflow on app2 via callChildWorkflow
 * 4. The child workflow calls a local activity and returns the result
 * 5. Asserting successful completion with expected output
 */
@Testcontainers
@Tag("testcontainers")
public class WorkflowsMultiAppSubOrchestrationIT {

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

  // Parent workflow orchestrator sidecar
  @Container
  private final static DaprContainer PARENT_WORKFLOW_SIDECAR = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
      .withAppName("suborchestration-parent")
      .withNetwork(DAPR_NETWORK)
      .withNetworkAliases("parent-workflow-sidecar")
      .withPlacementContainer(sharedPlacementContainer)
      .withSchedulerContainer(sharedSchedulerContainer)
      .withComponent(new Component("kvstore", "state.in-memory", "v1", Map.of("actorStateStore", "true")))
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .dependsOn(sharedPlacementContainer, sharedSchedulerContainer)
      .withLogConsumer(outputFrame -> System.out.println("PARENT_WORKFLOW: " + outputFrame.getUtf8String()))
      .withAppChannelAddress("host.testcontainers.internal");

  // App2 sidecar for the child sub-orchestration
  @Container
  private final static DaprContainer APP2_SIDECAR = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
      .withAppName("app2")
      .withNetwork(DAPR_NETWORK)
      .withNetworkAliases("app2-sidecar")
      .withPlacementContainer(sharedPlacementContainer)
      .withSchedulerContainer(sharedSchedulerContainer)
      .withAppChannelAddress("parent-workflow-sidecar:3500")
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .dependsOn(sharedPlacementContainer, sharedSchedulerContainer, PARENT_WORKFLOW_SIDECAR)
      .withComponent(new Component("kvstore", "state.in-memory", "v1", Map.of("actorStateStore", "true")))
      .withLogConsumer(outputFrame -> System.out.println("APP2: " + outputFrame.getUtf8String()));

  // Parent worker container - registers SubOrchestrationParentWorkflow
  @Container
  private static GenericContainer<?> parentWorker = new GenericContainer<>(ContainerConstants.JDK_17_TEMURIN_JAMMY)
      .withCopyFileToContainer(MountableFile.forHostPath("target"), "/app")
      .withWorkingDirectory("/app")
      .withCommand("java", "-cp", "test-classes:classes:dependency/*:*",
          "-Ddapr.app.id=suborchestration-parent",
          "-Ddapr.grpc.endpoint=parent-workflow-sidecar:50001",
          "-Ddapr.http.endpoint=parent-workflow-sidecar:3500",
          "io.dapr.it.testcontainers.workflows.multiapp.SubOrchestrationParentWorker")
      .withNetwork(DAPR_NETWORK)
      .dependsOn(PARENT_WORKFLOW_SIDECAR)
      .waitingFor(Wait.forLogMessage(".*SubOrchestrationParentWorker started.*", 1))
      .withLogConsumer(outputFrame -> System.out.println("ParentWorker: " + outputFrame.getUtf8String()));

  // App2 worker container - registers SubOrchestrationChildWorkflow and ChildTransformActivity
  @Container
  private final static GenericContainer<?> app2Worker = new GenericContainer<>(ContainerConstants.JDK_17_TEMURIN_JAMMY)
      .withCopyFileToContainer(MountableFile.forHostPath("target"), "/app")
      .withWorkingDirectory("/app")
      .withCommand("java", "-cp", "test-classes:classes:dependency/*:*",
          "-Ddapr.app.id=app2",
          "-Ddapr.grpc.endpoint=app2-sidecar:50001",
          "-Ddapr.http.endpoint=app2-sidecar:3500",
          "io.dapr.it.testcontainers.workflows.multiapp.SubOrchestrationChildWorker")
      .withNetwork(DAPR_NETWORK)
      .dependsOn(APP2_SIDECAR)
      .waitingFor(Wait.forLogMessage(".*SubOrchestrationChildWorker started.*", 1))
      .withLogConsumer(outputFrame -> System.out.println("App2Worker: " + outputFrame.getUtf8String()));

  @Test
  public void testMultiAppSubOrchestration() throws Exception {
    String input = "Hello World";
    // Child transforms: "Hello World" -> "HELLO WORLD [CHILD TRANSFORMED]"
    // Parent appends: -> "HELLO WORLD [CHILD TRANSFORMED] [PARENT DONE]"
    String expectedOutput = "HELLO WORLD [CHILD TRANSFORMED] [PARENT DONE]";

    Map<String, String> propertyOverrides = Map.of(
        "dapr.grpc.endpoint", PARENT_WORKFLOW_SIDECAR.getGrpcEndpoint(),
        "dapr.http.endpoint", PARENT_WORKFLOW_SIDECAR.getHttpEndpoint()
    );

    Properties clientProperties = new Properties(propertyOverrides);
    DaprWorkflowClient workflowClient = new DaprWorkflowClient(clientProperties);

    try {
      String instanceId = workflowClient.scheduleNewWorkflow(SubOrchestrationParentWorkflow.class, input);
      assertNotNull(instanceId, "Workflow instance ID should not be null");
      workflowClient.waitForWorkflowStart(instanceId, Duration.ofSeconds(30), false);

      WorkflowState workflowStatus = workflowClient.waitForWorkflowCompletion(instanceId, null, true);
      assertNotNull(workflowStatus, "Workflow status should not be null");
      assertEquals(WorkflowRuntimeStatus.COMPLETED, workflowStatus.getRuntimeStatus(),
          "Workflow should complete successfully");
      String workflowOutput = workflowStatus.readOutputAs(String.class);
      assertEquals(expectedOutput, workflowOutput, "Workflow output should match expected result");
    } finally {
      workflowClient.close();
    }
  }
}
