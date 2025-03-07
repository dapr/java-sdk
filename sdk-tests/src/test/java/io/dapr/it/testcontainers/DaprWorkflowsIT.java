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

package io.dapr.it.testcontainers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowInstanceStatus;
import io.dapr.workflows.runtime.WorkflowRuntime;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import static io.dapr.it.testcontainers.DaprContainerConstants.IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = {
        TestDaprWorkflowsConfiguration.class,
        TestWorkflowsApplication.class
    }
)
@Testcontainers
@Tag("testcontainers")
public class DaprWorkflowsIT {

  private static final Network DAPR_NETWORK = Network.newNetwork();

  @Container
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer(IMAGE_TAG)
      .withAppName("workflow-dapr-app")
      .withNetwork(DAPR_NETWORK)
      .withComponent(new Component("kvstore", "state.in-memory", "v1",
          Map.of("actorStateStore", "true")))
      .withComponent(new Component("pubsub", "pubsub.in-memory", "v1", Collections.emptyMap()))
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
      .withAppChannelAddress("host.testcontainers.internal");

  /**
   * Expose the Dapr ports to the host.
   *
   * @param registry the dynamic property registry
   */
  @DynamicPropertySource
  static void daprProperties(DynamicPropertyRegistry registry) {
    registry.add("dapr.http.endpoint", DAPR_CONTAINER::getHttpEndpoint);
    registry.add("dapr.grpc.endpoint", DAPR_CONTAINER::getGrpcEndpoint);
  }

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Autowired
  private DaprWorkflowClient workflowClient;

  @Autowired
  private WorkflowRuntimeBuilder workflowRuntimeBuilder;

  /**
   * Initializes the test.
   */
  @BeforeEach
  public void init() {
    try (WorkflowRuntime runtime = workflowRuntimeBuilder.build()) {
      System.out.println("Start workflow runtime");
      runtime.start(false);
    }
  }

  @Test
  public void testWorkflows() throws Exception {
    TestWorkflowPayload payload = new TestWorkflowPayload(new ArrayList<>());
    String instanceId = workflowClient.scheduleNewWorkflow(TestWorkflow.class, payload);

    workflowClient.waitForInstanceStart(instanceId, Duration.ofSeconds(10), false);
    workflowClient.raiseEvent(instanceId, "MoveForward", payload);

    Duration timeout = Duration.ofSeconds(10);
    WorkflowInstanceStatus workflowStatus = workflowClient.waitForInstanceCompletion(instanceId, timeout, true);

    assertNotNull(workflowStatus);

    TestWorkflowPayload workflowOutput = deserialize(workflowStatus.getSerializedOutput());

    assertEquals(2, workflowOutput.getPayloads().size());
    assertEquals("First Activity", workflowOutput.getPayloads().get(0));
    assertEquals("Second Activity", workflowOutput.getPayloads().get(1));
    assertEquals(instanceId, workflowOutput.getWorkflowId());
  }

  private TestWorkflowPayload deserialize(String value) throws JsonProcessingException {
    return OBJECT_MAPPER.readValue(value, TestWorkflowPayload.class);
  }

}
