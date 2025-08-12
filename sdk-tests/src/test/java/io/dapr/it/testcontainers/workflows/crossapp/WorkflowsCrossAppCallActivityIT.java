/*
 * Copyright 2025 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowInstanceStatus;
import io.dapr.workflows.client.WorkflowRuntimeStatus;
import io.dapr.config.Properties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.testcontainers.images.builder.Transferable;

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

  // Main workflow orchestrator container
  @Container
  private static final DaprContainer MAIN_WORKFLOW_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
      .withAppName("crossapp-worker")
      .withNetwork(DAPR_NETWORK)
      .withNetworkAliases("main-workflow-sidecar")
      .withComponent(new Component("kvstore", "state.in-memory", "v1",
          Map.of("actorStateStore", "true")))
      .withComponent(new Component("pubsub", "pubsub.in-memory", "v1", Collections.emptyMap()))
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withLogConsumer(outputFrame -> System.out.println("MAIN_WORKFLOW: " + outputFrame.getUtf8String()))
      .withAppChannelAddress("host.testcontainers.internal");

  // App2 container for App2TransformActivity - using GenericContainer for custom ports
  @Container
  private static final GenericContainer<?> APP2_CONTAINER = new GenericContainer<>(DAPR_RUNTIME_IMAGE_TAG)
      .withNetwork(DAPR_NETWORK)
      .withNetworkAliases("app2-sidecar")
      .withExposedPorts(3501, 50002)
      .withCommand("./daprd", 
                   "--app-id", "app2",
                   "--dapr-listen-addresses=0.0.0.0",
                   "--dapr-http-port", "3501",
                   "--dapr-grpc-port", "50002",
                   "--app-protocol", "http",
                   "--placement-host-address", "placement:50005",
                   "--scheduler-host-address", "scheduler:51005",
                   "--app-channel-address", "main-workflow-sidecar:3500", // cant use host.testcontainers.internal because it's not a valid hostname
                   "--log-level", "DEBUG",
                   "--resources-path", "/dapr-resources")
      .withCopyToContainer(Transferable.of("apiVersion: dapr.io/v1alpha1\n" +
          "kind: Component\n" +
          "metadata:\n" +
          "  name: kvstore\n" +
          "spec:\n" +
          "  type: state.in-memory\n" +
          "  version: v1\n" +
          "  metadata:\n" +
          "  - name: actorStateStore\n" +
          "    value: 'true'\n"), "/dapr-resources/kvstore.yaml")
      .withCopyToContainer(Transferable.of("apiVersion: dapr.io/v1alpha1\n" +
          "kind: Component\n" +
          "metadata:\n" +
          "  name: pubsub\n" +
          "spec:\n" +
          "  type: pubsub.in-memory\n" +
          "  version: v1\n"), "/dapr-resources/pubsub.yaml")
      .withCopyToContainer(Transferable.of("apiVersion: dapr.io/v1alpha1\n" +
          "kind: Subscription\n" +
          "metadata:\n" +
          "  name: local\n" +
          "spec:\n" +
          "  pubsubname: pubsub\n" +
          "  topic: topic\n" +
          "  route: /events\n"), "/dapr-resources/subscription.yaml")
      .waitingFor(Wait.forHttp("/v1.0/healthz/outbound")
          .forPort(3501)
          .forStatusCodeMatching(statusCode -> statusCode >= 200 && statusCode <= 399))
      .withLogConsumer(outputFrame -> System.out.println("APP2: " + outputFrame.getUtf8String()));

  // App3 container for App3FinalizeActivity - using GenericContainer for custom ports
  @Container
  private static final GenericContainer<?> APP3_CONTAINER = new GenericContainer<>(DAPR_RUNTIME_IMAGE_TAG)
      .withNetwork(DAPR_NETWORK)
      .withNetworkAliases("app3-sidecar")
      .withExposedPorts(3502, 50003)
      .withCommand("./daprd", 
                   "--app-id", "app3",
                   "--dapr-listen-addresses=0.0.0.0",
                   "--dapr-http-port", "3502",
                   "--dapr-grpc-port", "50003",
                   "--app-protocol", "http",
                   "--placement-host-address", "placement:50005",
                   "--scheduler-host-address", "scheduler:51005",
                   "--app-channel-address", "main-workflow-sidecar:3500", // cant use host.testcontainers.internal because it's not a valid hostname
                   "--log-level", "DEBUG",
                   "--resources-path", "/dapr-resources")
      .withCopyToContainer(Transferable.of("apiVersion: dapr.io/v1alpha1\n" +
          "kind: Component\n" +
          "metadata:\n" +
          "  name: kvstore\n" +
          "spec:\n" +
          "  type: state.in-memory\n" +
          "  version: v1\n" +
          "  metadata:\n" +
          "  - name: actorStateStore\n" +
          "    value: 'true'\n"), "/dapr-resources/kvstore.yaml")
      .withCopyToContainer(Transferable.of("apiVersion: dapr.io/v1alpha1\n" +
          "kind: Component\n" +
          "metadata:\n" +
          "  name: pubsub\n" +
          "spec:\n" +
          "  type: pubsub.in-memory\n" +
          "  version: v1\n"), "/dapr-resources/pubsub.yaml")
      .withCopyToContainer(Transferable.of("apiVersion: dapr.io/v1alpha1\n" +
          "kind: Subscription\n" +
          "metadata:\n" +
          "  name: local\n" +
          "spec:\n" +
          "  pubsubname: pubsub\n" +
          "  topic: topic\n" +
          "  route: /events\n"), "/dapr-resources/subscription.yaml")
      .waitingFor(Wait.forHttp("/v1.0/healthz/outbound")
          .forPort(3502)
          .forStatusCodeMatching(statusCode -> statusCode >= 200 && statusCode <= 399))
      .withLogConsumer(outputFrame -> System.out.println("APP3: " + outputFrame.getUtf8String()));

  // TestContainers for each app
  private static GenericContainer<?> crossappWorker;
  private static GenericContainer<?> app2Worker;
  private static GenericContainer<?> app3Worker;

  @BeforeAll
  public static void setUp() throws Exception {
    // Wait for sidecars to be fully initialized & stabilize
    Thread.sleep(15000);
    
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
    
    // Start app2 worker (connects to APP2_CONTAINER)
    app2Worker = new GenericContainer<>("openjdk:17-jdk-slim")
        .withCopyFileToContainer(MountableFile.forHostPath("target/test-classes"), "/app/classes")
        .withCopyFileToContainer(MountableFile.forHostPath("target/dependency"), "/app/libs")
        .withWorkingDirectory("/app")
        .withCommand("java", "-cp", "/app/classes:/app/libs/*", 
                     "-Ddapr.app.id=app2",
                     "-Ddapr.grpc.endpoint=app2-sidecar:50002",
                     "-Ddapr.http.endpoint=app2-sidecar:3501",
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
                     "-Ddapr.grpc.endpoint=app3-sidecar:50003",
                     "-Ddapr.http.endpoint=app3-sidecar:3502",
                     "io.dapr.it.testcontainers.workflows.crossapp.App3Worker")
        .withNetwork(DAPR_NETWORK)
        .waitingFor(Wait.forLogMessage(".*App3Worker started.*", 1))
        .withLogConsumer(outputFrame -> System.out.println("App3Worker: " + outputFrame.getUtf8String()));
    
    // Start all worker containers
    crossappWorker.start();
    app2Worker.start();
    app3Worker.start();
    
    // Wait for workers to be fully ready and connected
    Thread.sleep(5000);
  }

  @AfterAll
  public static void tearDown() throws Exception {
    // Clean up worker containers
    if (crossappWorker != null) {
      crossappWorker.stop();
    }
    if (app2Worker != null) {
      app2Worker.stop();
    }
    if (app3Worker != null) {
      app3Worker.stop();
    }
  }

  /**
   * Verifies that all Dapr sidecars are healthy and ready to accept connections.
   * This helps prevent the "sidecar unavailable" errors we were seeing.
   */
  private void verifySidecarsReady() throws Exception {
    // Main container uses ports (3500, 50001)
    String mainHealthUrl = "http://localhost:" + MAIN_WORKFLOW_CONTAINER.getMappedPort(3500) + "/v1.0/healthz/outbound";
    waitForHealthEndpoint(mainHealthUrl, "Main workflow sidecar");
    
    // App2 uses custom ports (3501, 50002)
    String app2HealthUrl = "http://localhost:" + APP2_CONTAINER.getMappedPort(3501) + "/v1.0/healthz/outbound";
    waitForHealthEndpoint(app2HealthUrl, "App2 sidecar");
    
    // App3 uses custom ports (3502, 50003)
    String app3HealthUrl = "http://localhost:" + APP3_CONTAINER.getMappedPort(3502) + "/v1.0/healthz/outbound";
    waitForHealthEndpoint(app3HealthUrl, "App3 sidecar");
  }
  
  /**
   * Waits for a health endpoint to return a successful response.
   */
  private void waitForHealthEndpoint(String healthUrl, String sidecarName) throws Exception {
    int maxAttempts = 30; // 30s max
    int attempt = 0;
    
    while (attempt < maxAttempts) {
      try {
        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(healthUrl))
            .GET()
            .build();
        
        java.net.http.HttpResponse<String> response = client.send(request, 
            java.net.http.HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 200 && response.statusCode() < 400) {
          System.out.println(sidecarName + " is healthy and ready");
          return;
        }
      } catch (Exception e) {
        // Ignore connection errors bc they're expected while sidecar is starting
      }
      
      attempt++;
      Thread.sleep(1000); // Wait 1s before retry
    }
    
    throw new RuntimeException(sidecarName + " failed to become healthy within " + maxAttempts + " seconds");
  }

  @Test
  public void testCrossAppWorkflow() throws Exception {
    verifySidecarsReady();
    
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

      Duration timeout = Duration.ofMinutes(2);
      WorkflowInstanceStatus workflowStatus = workflowClient.waitForInstanceCompletion(instanceId, timeout, true);
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
