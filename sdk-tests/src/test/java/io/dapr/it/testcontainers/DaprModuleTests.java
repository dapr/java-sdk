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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowInstanceStatus;
import io.dapr.workflows.runtime.WorkflowRuntime;
import io.dapr.workflows.runtime.WorkflowRuntimeBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = MyTestWithWorkflowsApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
public class DaprModuleTests {

  @Autowired
  private SubscriptionsRestController subscriptionsRestController;

  private DaprWorkflowClient workflowClient;

  /**
   * Initializes the test.
   */
  @BeforeEach
  public void init() {
    WorkflowRuntimeBuilder builder = new WorkflowRuntimeBuilder().registerWorkflow(TestWorkflow.class);
    builder.registerActivity(FirstActivity.class);
    builder.registerActivity(SecondActivity.class);

    try (WorkflowRuntime runtime = builder.build()) {
      System.out.println("Start workflow runtime");
      runtime.start(false);
    }
  }

  @Test
  public void myWorkflowTest() throws Exception {
    workflowClient = new DaprWorkflowClient();

    TestWorkflowPayload payload = new TestWorkflowPayload(new ArrayList<>());
    String instanceId = workflowClient.scheduleNewWorkflow(TestWorkflow.class, payload);

    workflowClient.waitForInstanceStart(instanceId, Duration.ofSeconds(10), false);

    workflowClient.raiseEvent(instanceId, "MoveForward", payload);

    WorkflowInstanceStatus workflowStatus = workflowClient.waitForInstanceCompletion(instanceId,
        Duration.ofSeconds(10),
        true);

    // The workflow completed before 10 seconds
    assertNotNull(workflowStatus);

    String workflowPlayloadJson = workflowStatus.getSerializedOutput();

    ObjectMapper mapper = new ObjectMapper();
    TestWorkflowPayload workflowOutput = mapper.readValue(workflowPlayloadJson, TestWorkflowPayload.class);

    assertEquals(2, workflowOutput.getPayloads().size());
    assertEquals("First Activity", workflowOutput.getPayloads().get(0));
    assertEquals("Second Activity", workflowOutput.getPayloads().get(1));
    assertEquals(instanceId, workflowOutput.getWorkflowId());
  }

}
