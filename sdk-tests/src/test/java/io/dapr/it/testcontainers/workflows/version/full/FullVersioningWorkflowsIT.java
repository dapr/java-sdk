/*
 * Copyright 2024 The Dapr Authors
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

package io.dapr.it.testcontainers.workflows.version.full;

import io.dapr.it.testcontainers.TestContainerNetworks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.config.Properties;
import io.dapr.it.spring.data.CustomMySQLContainer;
import io.dapr.it.testcontainers.ContainerConstants;
import io.dapr.it.testcontainers.workflows.TestWorkflowsApplication;
import io.dapr.it.testcontainers.workflows.TestWorkflowsConfiguration;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import io.dapr.testcontainers.DaprPlacementContainer;
import io.dapr.testcontainers.DaprSchedulerContainer;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowState;
import io.dapr.workflows.runtime.WorkflowRuntime;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static io.dapr.it.spring.data.DaprSpringDataConstants.STATE_STORE_NAME;
import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static io.dapr.testcontainers.DaprContainerConstants.DAPR_PLACEMENT_IMAGE_TAG;
import static io.dapr.testcontainers.DaprContainerConstants.DAPR_SCHEDULER_IMAGE_TAG;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = {
        TestWorkflowsConfiguration.class,
        TestWorkflowsApplication.class
    }
)
@Testcontainers
@Tag("testcontainers")
public class FullVersioningWorkflowsIT {

  private static final Network DAPR_NETWORK = TestContainerNetworks.WORKFLOWS_NETWORK;

  private static final WaitStrategy MYSQL_WAIT_STRATEGY = Wait
      .forLogMessage(".*port: 3306  MySQL Community Server \\(GPL\\).*", 1)
      .withStartupTimeout(Duration.of(60, ChronoUnit.SECONDS));

  private static final String STATE_STORE_DSN = "mysql:password@tcp(mysql:3306)/";
  private static final Map<String, String> STATE_STORE_PROPERTIES = createStateStoreProperties();

  @Container
  private static final MySQLContainer<?> MY_SQL_CONTAINER = new CustomMySQLContainer<>("mysql:5.7.34")
      .withNetworkAliases("mysql")
      .withDatabaseName("dapr_db")
      .withUsername("mysql")
      .withPassword("password")
      .withNetwork(DAPR_NETWORK)
      .waitingFor(MYSQL_WAIT_STRATEGY);

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

  @Container
  private static final DaprContainer DAPR_CONTAINER_V1 = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
      .withAppName("dapr-worker")
      .withNetworkAliases("dapr-worker-v1")
      .withNetwork(DAPR_NETWORK)
      .withComponent(new Component(STATE_STORE_NAME, "state.mysql", "v1", STATE_STORE_PROPERTIES))
      .withPlacementContainer(sharedPlacementContainer)
      .withSchedulerContainer(sharedSchedulerContainer)
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withLogConsumer(outputFrame -> System.out.println("daprV1 -> " +outputFrame.getUtf8String()))
      .withAppChannelAddress("host.testcontainers.internal")
      .dependsOn(MY_SQL_CONTAINER, sharedPlacementContainer, sharedSchedulerContainer);

  private static final DaprContainer DAPR_CONTAINER_V2 = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
      .withAppName("dapr-worker")
      .withNetworkAliases("dapr-worker-v2")
      .withNetwork(DAPR_NETWORK)
      .withComponent(new Component(STATE_STORE_NAME, "state.mysql", "v1", STATE_STORE_PROPERTIES))
      .withPlacementContainer(sharedPlacementContainer)
      .withSchedulerContainer(sharedSchedulerContainer)
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withLogConsumer(outputFrame -> System.out.println("daprV2 -> " + outputFrame.getUtf8String()))
      .withAppChannelAddress("host.testcontainers.internal")
      .dependsOn(MY_SQL_CONTAINER, sharedPlacementContainer, sharedSchedulerContainer);

  @Container
  private final static GenericContainer<?> workerV1 = new GenericContainer<>(ContainerConstants.JDK_17_TEMURIN_JAMMY)
      .withCopyFileToContainer(MountableFile.forHostPath("target"), "/app")
      .withWorkingDirectory("/app")
      .withCommand("java", "-cp", "test-classes:classes:dependency/*:*",
          "-Ddapr.app.id=dapr-worker",
          "-Ddapr.grpc.endpoint=dapr-worker-v1:50001",
          "-Ddapr.http.endpoint=dapr-worker-v1:3500",
          "io.dapr.it.testcontainers.workflows.version.full.WorkflowV1Worker")
      .withNetwork(DAPR_NETWORK)
      .dependsOn(DAPR_CONTAINER_V1)
      .waitingFor(Wait.forLogMessage(".*WorkerV1 started.*", 1))
      .withLogConsumer(outputFrame -> System.out.println("WorkerV1: " + outputFrame.getUtf8String()));

// This container will be started manually
  private final static GenericContainer<?> workerV2 = new GenericContainer<>(ContainerConstants.JDK_17_TEMURIN_JAMMY)
      .withCopyFileToContainer(MountableFile.forHostPath("target"), "/app")
      .withWorkingDirectory("/app")
      .withCommand("java", "-cp", "test-classes:classes:dependency/*:*",
          "-Ddapr.app.id=dapr-worker",
          "-Ddapr.grpc.endpoint=dapr-worker-v2:50001",
          "-Ddapr.http.endpoint=dapr-worker-v2:3500",
          "io.dapr.it.testcontainers.workflows.version.full.WorkflowV2Worker")
      .withNetwork(DAPR_NETWORK)
      .dependsOn(DAPR_CONTAINER_V2)
      .waitingFor(Wait.forLogMessage(".*WorkerV2 started.*", 1))
      .withLogConsumer(outputFrame -> System.out.println("WorkerV2: " + outputFrame.getUtf8String()));


  private static Map<String, String> createStateStoreProperties() {
    Map<String, String> result = new HashMap<>();

    result.put("keyPrefix", "name");
    result.put("schemaName", "dapr_db");
    result.put("actorStateStore", "true");
    result.put("connectionString", STATE_STORE_DSN);

    return result;
  }

  @DynamicPropertySource
  static void daprProperties(DynamicPropertyRegistry registry) {
    registry.add("dapr.http.endpoint", DAPR_CONTAINER_V1::getHttpEndpoint);
    registry.add("dapr.grpc.endpoint", DAPR_CONTAINER_V1::getGrpcEndpoint);
  }

  @Test
  public void testWorkflows() throws Exception {
    DaprWorkflowClient workflowClientV1 = daprWorkflowClient(DAPR_CONTAINER_V1.getHttpEndpoint(), DAPR_CONTAINER_V1.getGrpcEndpoint());
// Start workflow V1
    String instanceIdV1 = workflowClientV1.scheduleNewWorkflow("VersionWorkflow");
    workflowClientV1.waitForWorkflowStart(instanceIdV1, Duration.ofSeconds(10), false);

    // Stop worker and dapr
    workerV1.stop();
    DAPR_CONTAINER_V1.stop();

    // Start new worker with patched workflow
    DAPR_CONTAINER_V2.start();
    workerV2.start();
    Thread.sleep(1000);
    DaprWorkflowClient workflowClientV2 = daprWorkflowClient(DAPR_CONTAINER_V2.getHttpEndpoint(), DAPR_CONTAINER_V2.getGrpcEndpoint());

    // Start workflow V2
    String instanceIdV2 = workflowClientV2.scheduleNewWorkflow("VersionWorkflow");
    workflowClientV2.waitForWorkflowStart(instanceIdV2, Duration.ofSeconds(10), false);

    // Continue workflow V1
    workflowClientV2.raiseEvent(instanceIdV1, "test", null);

    // Wait for workflow to complete
    Duration timeout = Duration.ofSeconds(10);
    WorkflowState workflowStatusV1 = workflowClientV2.waitForWorkflowCompletion(instanceIdV1, timeout, true);
    WorkflowState workflowStatusV2 = workflowClientV2.waitForWorkflowCompletion(instanceIdV2, timeout, true);

    assertNotNull(workflowStatusV1);
    assertNotNull(workflowStatusV2);

    String resultV1 = workflowStatusV1.readOutputAs(String.class);
    assertEquals("Activity1, Activity2", resultV1);

    String resultV2 = workflowStatusV2.readOutputAs(String.class);
    assertEquals("Activity3, Activity4", resultV2);
  }

  public DaprWorkflowClient daprWorkflowClient(
     String daprHttpEndpoint,
      String daprGrpcEndpoint
  ){
    Map<String, String> overrides = Map.of(
        "dapr.http.endpoint", daprHttpEndpoint,
        "dapr.grpc.endpoint", daprGrpcEndpoint
    );

    return new DaprWorkflowClient(new Properties(overrides));
  }
}

