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

import io.dapr.durabletask.Task;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import io.dapr.workflows.WorkflowTaskOptions;
import io.dapr.workflows.WorkflowTaskRetryPolicy;

import java.time.Duration;

import org.slf4j.Logger;

public class TestExecutionKeysWorkflow implements Workflow {

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

      WorkflowTaskOptions options = new WorkflowTaskOptions(WorkflowTaskRetryPolicy.newBuilder()
      .setMaxNumberOfAttempts(3)
      .setFirstRetryInterval(Duration.ofSeconds(1))
      .setMaxRetryInterval(Duration.ofSeconds(10))
      .setBackoffCoefficient(2.0)  
      .setRetryTimeout(Duration.ofSeconds(50))
      .build());
            
      
      Task<TestWorkflowPayload> t =   ctx.callActivity(TaskExecutionIdActivity.class.getName(), workflowPayload, options,TestWorkflowPayload.class);

      TestWorkflowPayload payloadAfterExecution = t.await();
   
      ctx.complete(payloadAfterExecution);
    };
  }

}
