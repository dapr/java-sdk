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
limitations under the License.
*/

package io.dapr.it.testcontainers.workflows;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.slf4j.Logger;

public class TestNamedActivitiesWorkflow implements Workflow {

  @Override
  public WorkflowStub create() {
    return ctx -> {
      Logger logger = ctx.getLogger();
      String instanceId = ctx.getInstanceId();
      logger.info("Starting Workflow: " + ctx.getName());
      logger.info("Instance ID: " + instanceId);
      logger.info("Current Orchestration Time: " + ctx.getCurrentInstant());

      TestWorkflowPayload workflowPayload = ctx.getInput(TestWorkflowPayload.class);
      workflowPayload.setWorkflowId(instanceId);

      var payloadAfterA = ctx.callActivity("a", workflowPayload, TestWorkflowPayload.class)
              .await();

      var payloadAfterB = ctx.callActivity("b", payloadAfterA, TestWorkflowPayload.class)
              .await();

      var payloadAfterC = ctx.callActivity("c", payloadAfterB, TestWorkflowPayload.class)
              .await();

      var payloadAfterD = ctx.callActivity("d", payloadAfterC, TestWorkflowPayload.class)
              .await();

      var payloadAfterE = ctx.callActivity("e", payloadAfterD, TestWorkflowPayload.class)
              .await();
          
      ctx.complete(payloadAfterE);
    };
  }

}
