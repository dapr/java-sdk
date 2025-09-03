/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.it.testcontainers.workflows.crossapp;

import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import io.dapr.testcontainers.DaprPlacementContainer;
import io.dapr.testcontainers.DaprSchedulerContainer;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowInstanceStatus;
import io.dapr.workflows.client.WorkflowRuntimeStatus;
import io.dapr.config.Properties;
import net.bytebuddy.utility.dispatcher.JavaDispatcher;
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
 * Cross-App Pattern integration test.
 *
 * This test demonstrates the cross-app pattern by:
 * 1. Starting 3 Dapr containers (crossapp-worker, app2, app3)
 * 2. Launching Java processes that register workflows/activities in separate apps
 * 3. Executing a cross-app workflow
 * 4. Asserting successful completion
 */
@Testcontainers
@Tag("testcontainers")
public class WorkflowsCrossAppCallActivityIT {

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

  // Main workflow orchestrator container
  @Container
  private final static DaprContainer MAIN_WORKFLOW_SIDECAR = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
      .withAppName("crossapp-worker")
      .withNetwork(DAPR_NETWORK)
      .withNetworkAliases("main-workflow-sidecar")
      .withPlacementContainer(sharedPlacementContainer)
      .withSchedulerContainer(sharedSchedulerContainer)
      .withComponent(new Component("kvstore", "state.in-memory", "v1", Map.of("actorStateStore", "true")))
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .dependsOn(sharedPlacementContainer, sharedSchedulerContainer)
      .withLogConsumer(outputFrame -> System.out.println("MAIN_WORKFLOW: " + outputFrame.getUtf8String()))
      .withAppChannelAddress("host.testcontainers.internal");
  
  // App2 container for App2TransformActivity
  @Container
  private final static DaprContainer APP2_SIDECAR = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
      .withAppName("app2")
      .withNetwork(DAPR_NETWORK)
      .withNetworkAliases("app2-sidecar")
      .withPlacementContainer(sharedPlacementContainer)
      .withSchedulerContainer(sharedSchedulerContainer)
      .withAppChannelAddress("main-workflow-sidecar:3500")
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .dependsOn(sharedPlacementContainer, sharedSchedulerContainer, MAIN_WORKFLOW_SIDECAR)
      .withComponent(new Component("kvstore", "state.in-memory", "v1", Map.of("actorStateStore", "true")))
      .withLogConsumer(outputFrame -> System.out.println("APP2: " + outputFrame.getUtf8String()));
  
  // App3 container for App3FinalizeActivity
  @Container
  private final static DaprContainer APP3_SIDECAR = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
      .withAppName("app3")
      .withNetwork(DAPR_NETWORK)
      .withNetworkAliases("app3-sidecar")
      .withPlacementContainer(sharedPlacementContainer)
      .withSchedulerContainer(sharedSchedulerContainer)
      .withAppChannelAddress("main-workflow-sidecar:3500")
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .dependsOn(sharedPlacementContainer, sharedSchedulerContainer, MAIN_WORKFLOW_SIDECAR)
      .withComponent(new Component("kvstore", "state.in-memory", "v1", Map.of("actorStateStore", "true")))
      .withLogConsumer(outputFrame -> System.out.println("APP3: " + outputFrame.getUtf8String()));


  // TestContainers for each app
  @Container
  private static GenericContainer<?> crossappWorker = new GenericContainer<>("openjdk:17-jdk-slim")
      .withCopyFileToContainer(MountableFile.forHostPath("target"), "/app")
      .withWorkingDirectory("/app")
      .withCommand("java", "-cp", "test-classes:classes:dependency/*:*",
          "-Ddapr.app.id=crossapp-worker",
          "-Ddapr.grpc.endpoint=main-workflow-sidecar:50001",
          "-Ddapr.http.endpoint=main-workflow-sidecar:3500",
          "io.dapr.it.testcontainers.workflows.crossapp.CrossAppWorker")
      .withNetwork(DAPR_NETWORK)
      .dependsOn(MAIN_WORKFLOW_SIDECAR)
      .waitingFor(Wait.forLogMessage(".*CrossAppWorker started.*", 1))
      .withLogConsumer(outputFrame -> System.out.println("CrossAppWorker: " + outputFrame.getUtf8String()));

  @Container
  private final static GenericContainer<?> app2Worker = new GenericContainer<>("openjdk:17-jdk-slim")
      .withCopyFileToContainer(MountableFile.forHostPath("target"), "/app")
      .withWorkingDirectory("/app")
      .withCommand("java", "-cp", "test-classes:classes:dependency/*:*",
          "-Ddapr.app.id=app2",
          "-Ddapr.grpc.endpoint=app2-sidecar:50001",
          "-Ddapr.http.endpoint=app2-sidecar:3500",
          "io.dapr.it.testcontainers.workflows.crossapp.App2Worker")
      .withNetwork(DAPR_NETWORK)
      .dependsOn(APP2_SIDECAR)
      .waitingFor(Wait.forLogMessage(".*App2Worker started.*", 1))
      .withLogConsumer(outputFrame -> System.out.println("App2Worker: " + outputFrame.getUtf8String()));

  @Container
  private final static GenericContainer<?> app3Worker = new GenericContainer<>("openjdk:17-jdk-slim")
      .withCopyFileToContainer(MountableFile.forHostPath("target"), "/app")
      .withWorkingDirectory("/app")
      .withCommand("java", "-cp", "test-classes:classes:dependency/*:*",
          "-Ddapr.app.id=app3",
          "-Ddapr.grpc.endpoint=app3-sidecar:50001",
          "-Ddapr.http.endpoint=app3-sidecar:3500",
          "io.dapr.it.testcontainers.workflows.crossapp.App3Worker")
      .withNetwork(DAPR_NETWORK)
      .dependsOn(APP3_SIDECAR)
      .waitingFor(Wait.forLogMessage(".*App3Worker started.*", 1))
      .withLogConsumer(outputFrame -> System.out.println("App3Worker: " + outputFrame.getUtf8String()));

  @Test
  public void testCrossAppWorkflow() throws Exception {
    // TestContainers wait strategies ensure all containers are ready before this test runs

    String input = "Hello World";
    String expectedOutput = "HELLO WORLD [TRANSFORMED BY APP2] [FINALIZED BY APP3]";

    // Create workflow client connected to the main workflow orchestrator
    // Use the same endpoint configuration that the workers use
    // The workers use host.testcontainers.internal:50001
    Map<String, String> propertyOverrides = Map.of(
        "dapr.grpc.endpoint", MAIN_WORKFLOW_SIDECAR.getGrpcEndpoint(),
        "dapr.http.endpoint", MAIN_WORKFLOW_SIDECAR.getHttpEndpoint()
    );

    Properties clientProperties = new Properties(propertyOverrides);
    DaprWorkflowClient workflowClient = new DaprWorkflowClient(clientProperties);

    try {
      String instanceId = workflowClient.scheduleNewWorkflow(CrossAppWorkflow.class, input);
      assertNotNull(instanceId, "Workflow instance ID should not be null");
      workflowClient.waitForInstanceStart(instanceId, Duration.ofSeconds(30), false);

      WorkflowInstanceStatus workflowStatus = workflowClient.waitForInstanceCompletion(instanceId, null, true);
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
