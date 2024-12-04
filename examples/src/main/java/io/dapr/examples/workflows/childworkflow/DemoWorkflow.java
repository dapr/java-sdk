/*
 * Copyright 2023 The Dapr Authors
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

package io.dapr.examples.workflows.childworkflow;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;

public class DemoWorkflow extends Workflow {
  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting Workflow: " + ctx.getName());

      var childWorkflowInput = "Hello Dapr Workflow!";
      ctx.getLogger().info("calling childworkflow with input: " + childWorkflowInput);

      var childWorkflowOutput =
          ctx.callChildWorkflow(DemoChildWorkflow.class.getName(), childWorkflowInput, String.class).await();

      ctx.getLogger().info("childworkflow finished with: " + childWorkflowOutput);
      ctx.complete(childWorkflowOutput);
    };
  }
}
