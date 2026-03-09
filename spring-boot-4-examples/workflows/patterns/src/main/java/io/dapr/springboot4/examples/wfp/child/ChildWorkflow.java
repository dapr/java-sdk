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

package io.dapr.springboot4.examples.wfp.child;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.springframework.stereotype.Component;

@Component
public class ChildWorkflow implements Workflow {
  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting ChildWorkflow: " + ctx.getName());

      var childWorkflowInput = ctx.getInput(String.class);
      ctx.getLogger().info("ChildWorkflow received input: " + childWorkflowInput);

      ctx.getLogger().info("ChildWorkflow is calling Activity: " + ReverseActivity.class.getName());
      String result = ctx.callActivity(ReverseActivity.class.getName(), childWorkflowInput, String.class).await();

      ctx.getLogger().info("ChildWorkflow finished with: " + result);
      ctx.complete(result);
    };
  }
}
