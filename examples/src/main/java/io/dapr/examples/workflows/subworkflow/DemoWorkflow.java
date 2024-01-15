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

package io.dapr.examples.workflows.subworkflow;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;

public class DemoWorkflow extends Workflow {
  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting Workflow: " + ctx.getName());

      var subWorkflowInput = "Hello Dapr Workflow!";
      ctx.getLogger().info("calling subworkflow with input: " + subWorkflowInput);

      var subWorkflowOutput =
          ctx.callSubWorkflow(DemoSubWorkflow.class.getName(), subWorkflowInput, String.class).await();

      ctx.getLogger().info("subworkflow finished with: " + subWorkflowOutput);
      ctx.complete(subWorkflowOutput);
    };
  }
}
