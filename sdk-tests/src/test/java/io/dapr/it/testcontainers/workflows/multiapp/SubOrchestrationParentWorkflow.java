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

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import io.dapr.workflows.WorkflowTaskOptions;
import org.slf4j.Logger;

/**
 * Parent workflow that calls a child sub-orchestration on a remote app (app2).
 * The child workflow processes the input and returns the result back to the parent.
 */
public class SubOrchestrationParentWorkflow implements Workflow {
  @Override
  public WorkflowStub create() {
    return ctx -> {
      Logger logger = ctx.getLogger();
      String instanceId = ctx.getInstanceId();
      logger.info("Starting SubOrchestrationParentWorkflow: {}", ctx.getName());
      logger.info("Instance ID: {}", instanceId);

      String input = ctx.getInput(String.class);
      logger.info("Parent workflow input: {}", input);

      // Call SubOrchestrationChildWorkflow on app2
      String childResult = ctx.callChildWorkflow(
          SubOrchestrationChildWorkflow.class.getName(), input, null,
          new WorkflowTaskOptions("app2"), String.class
      ).await();

      logger.info("Child workflow result: {}", childResult);

      // Parent appends its own marker
      String finalResult = childResult + " [PARENT DONE]";
      ctx.complete(finalResult);
    };
  }
}
