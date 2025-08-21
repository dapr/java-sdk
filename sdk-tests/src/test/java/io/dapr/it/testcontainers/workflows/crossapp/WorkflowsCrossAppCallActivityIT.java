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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.io.File;

import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static io.dapr.testcontainers.DaprContainerConstants.DAPR_PLACEMENT_IMAGE_TAG;
import static io.dapr.testcontainers.DaprContainerConstants.DAPR_SCHEDULER_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import io.dapr.testcontainers.Subscription;

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
  
  // Shared placement and scheduler containers for all Dapr instances
  private static DaprPlacementContainer sharedPlacementContainer;
  private static DaprSchedulerContainer sharedSchedulerContainer;
  
  // Main workflow orchestrator container
  private static DaprContainer MAIN_WORKFLOW_CONTAINER;
  
  // App2 container for App2TransformActivity - using DaprContainer with custom ports
  private static DaprContainer APP2_CONTAINER;
  
  // App3 container for App3FinalizeActivity - using DaprContainer with custom ports
  private static DaprContainer APP3_CONTAINER;

  // TestContainers for each app
  private static GenericContainer<?> crossappWorker;
  private static GenericContainer<?> app2Worker;
  private static GenericContainer<?> app3Worker;

  @BeforeAll
  public static void setUp() throws Exception {
    // Ensure dependencies are copied for container classpath
    ensureDependenciesCopied();
    
    sharedPlacementContainer = new DaprPlacementContainer(DAPR_PLACEMENT_IMAGE_TAG)
        .withNetwork(DAPR_NETWORK)
        .withNetworkAliases("placement")
        .withReuse(false);
    sharedPlacementContainer.start();
    
    sharedSchedulerContainer = new DaprSchedulerContainer(DAPR_SCHEDULER_IMAGE_TAG)
        .withNetwork(DAPR_NETWORK)
        .withNetworkAliases("scheduler")
        .withReuse(false);
    sharedSchedulerContainer.start();

    // Initialize Dapr containers with shared placement and scheduler
    MAIN_WORKFLOW_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
        .withAppName("crossapp-worker")
        .withNetwork(DAPR_NETWORK)
        .withNetworkAliases("main-workflow-sidecar")
        .withPlacementContainer(sharedPlacementContainer)
        .withSchedulerContainer(sharedSchedulerContainer)
        .withComponent(new Component("kvstore", "state.in-memory", "v1", Map.of("actorStateStore", "true")))
        .withDaprLogLevel(DaprLogLevel.DEBUG)
        .withLogConsumer(outputFrame -> System.out.println("MAIN_WORKFLOW: " + outputFrame.getUtf8String()))
        .withAppChannelAddress("host.testcontainers.internal");
    
    APP2_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
        .withAppName("app2")
        .withNetwork(DAPR_NETWORK)
        .withNetworkAliases("app2-sidecar")
        .withPlacementContainer(sharedPlacementContainer)
        .withSchedulerContainer(sharedSchedulerContainer)
        .withAppChannelAddress("main-workflow-sidecar:3500")
        .withDaprLogLevel(DaprLogLevel.DEBUG)
        .withComponent(new Component("kvstore", "state.in-memory", "v1", Map.of("actorStateStore", "true")))
        .withLogConsumer(outputFrame -> System.out.println("APP2: " + outputFrame.getUtf8String()))
        .withExposedPorts(3500, 50001);
    
    APP3_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
        .withAppName("app3")
        .withNetwork(DAPR_NETWORK)
        .withNetworkAliases("app3-sidecar")
        .withPlacementContainer(sharedPlacementContainer)
        .withSchedulerContainer(sharedSchedulerContainer)
        .withAppChannelAddress("main-workflow-sidecar:3500")
        .withDaprLogLevel(DaprLogLevel.DEBUG)
        .withComponent(new Component("kvstore", "state.in-memory", "v1", Map.of("actorStateStore", "true")))
        .withLogConsumer(outputFrame -> System.out.println("APP3: " + outputFrame.getUtf8String()))
        .withExposedPorts(3500, 50001);
    
    // Start crossapp worker (connects to MAIN_WORKFLOW_CONTAINER)
    crossappWorker = new GenericContainer<>("openjdk:17-jdk-slim")
        .withCopyFileToContainer(MountableFile.forHostPath("target/test-classes"), "/app/classes")
        .withCopyFileToContainer(MountableFile.forHostPath("target/dependency"), "/app/libs")
        .withWorkingDirectory("/app")
        .withCommand("java", "-cp", "/app/classes:/app/libs/*", 
                     "-Ddapr.app.id=crossapp-worker",
                     "-Ddapr.grpc.endpoint=main-workflow-sidecar:50001",
                     "-Ddapr.http.endpoint=main-workflow-sidecar:3500",
                     "io.dapr.it.testcontainers.workflows.crossapp.CrossAppWorker")
        .withNetwork(DAPR_NETWORK)
        .waitingFor(Wait.forLogMessage(".*CrossAppWorker started.*", 1))
        .withLogConsumer(outputFrame -> System.out.println("CrossAppWorker: " + outputFrame.getUtf8String()));
    
    // Start all Dapr containers
    MAIN_WORKFLOW_CONTAINER.start();
    APP2_CONTAINER.start();
    APP3_CONTAINER.start();

      // Start app2 worker (connects to APP2_CONTAINER)
    app2Worker = new GenericContainer<>("openjdk:17-jdk-slim")
        .withCopyFileToContainer(MountableFile.forHostPath("target/test-classes"), "/app/classes")
        .withCopyFileToContainer(MountableFile.forHostPath("target/dependency"), "/app/libs")
        .withWorkingDirectory("/app")
        .withCommand("java", "-cp", "/app/classes:/app/libs/*", 
                     "-Ddapr.app.id=app2",
                     "-Ddapr.grpc.endpoint=app2-sidecar:50001",
                     "-Ddapr.http.endpoint=app2-sidecar:3500",
                     "io.dapr.it.testcontainers.workflows.crossapp.App2Worker")
        .withNetwork(DAPR_NETWORK)
        .waitingFor(Wait.forLogMessage(".*App2Worker started.*", 1))
        .withLogConsumer(outputFrame -> System.out.println("App2Worker: " + outputFrame.getUtf8String()));

    // Start app3 worker (connects to APP3_CONTAINER)
    app3Worker = new GenericContainer<>("openjdk:17-jdk-slim")
        .withCopyFileToContainer(MountableFile.forHostPath("target/test-classes"), "/app/classes")
        .withCopyFileToContainer(MountableFile.forHostPath("target/dependency"), "/app/libs")
        .withWorkingDirectory("/app")
        .withCommand("java", "-cp", "/app/classes:/app/libs/*", 
                     "-Ddapr.app.id=app3",
                     "-Ddapr.grpc.endpoint=app3-sidecar:50001",
                     "-Ddapr.http.endpoint=app3-sidecar:3500",
                     "io.dapr.it.testcontainers.workflows.crossapp.App3Worker")
        .withNetwork(DAPR_NETWORK)
        .waitingFor(Wait.forLogMessage(".*App3Worker started.*", 1))
        .withLogConsumer(outputFrame -> System.out.println("App3Worker: " + outputFrame.getUtf8String()));
    
    // Start all worker containers
    crossappWorker.start();
    app2Worker.start();
    app3Worker.start();
  }

  @AfterAll
  public static void tearDown() {
    if (crossappWorker != null) {
      crossappWorker.stop();
    }
    if (app2Worker != null) {
      app2Worker.stop();
    }
    if (app3Worker != null) {
      app3Worker.stop();
    }
    if (MAIN_WORKFLOW_CONTAINER != null) {
      MAIN_WORKFLOW_CONTAINER.stop();
    }
    if (APP2_CONTAINER != null) {
      APP2_CONTAINER.stop();
    }
    if (APP3_CONTAINER != null) {
      APP3_CONTAINER.stop();
    }
    if (sharedPlacementContainer != null) {
      sharedPlacementContainer.stop();
    }
    if (sharedSchedulerContainer != null) {
      sharedSchedulerContainer.stop();
    }
  }

  @Test
  public void testCrossAppWorkflow() throws Exception {
    // TestContainers wait strategies ensure all containers are ready before this test runs

    String input = "Hello World";
    String expectedOutput = "HELLO WORLD [TRANSFORMED BY APP2] [FINALIZED BY APP3]";
    
    // Create workflow client connected to the main workflow orchestrator
    // Use the same endpoint configuration that the workers use
    // The workers use host.testcontainers.internal:50001, so we need to use the mapped port
    String grpcEndpoint = "localhost:" + MAIN_WORKFLOW_CONTAINER.getMappedPort(50001);
    String httpEndpoint = "localhost:" + MAIN_WORKFLOW_CONTAINER.getMappedPort(3500);
    System.setProperty("dapr.grpc.endpoint", grpcEndpoint);
    System.setProperty("dapr.http.endpoint", httpEndpoint);
    Map<String, String> propertyOverrides = Map.of(
        "dapr.grpc.endpoint", grpcEndpoint,
        "dapr.http.endpoint", httpEndpoint
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
  
  /**
   * Ensures that dependencies are copied to target/dependency for container classpath.
   * This is needed because the containers need access to the workflow runtime classes.
   */
  private static void ensureDependenciesCopied() {
    File dependencyDir = new File("target/dependency");
    if (!dependencyDir.exists() || dependencyDir.listFiles() == null || dependencyDir.listFiles().length == 0) {
      System.out.println("Dependencies not found in target/dependency, copying...");
      try {
        ProcessBuilder pb = new ProcessBuilder("mvn", "dependency:copy-dependencies",
            "-Dspotbugs.skip=true", "-Dcheckstyle.skip=true");
        pb.inheritIO();
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
          throw new RuntimeException("Failed to copy dependencies, exit code: " + exitCode);
        }
        System.out.println("Dependencies copied successfully");
      } catch (Exception e) {
        throw new RuntimeException("Failed to ensure dependencies are copied", e);
      }
    } else {
      System.out.println("Dependencies already exist in target/dependency");
    }
  }
}
