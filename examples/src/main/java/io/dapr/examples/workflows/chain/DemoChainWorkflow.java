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

package io.dapr.examples.workflows.chain;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;

public class DemoChainWorkflow extends Workflow {
  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting Workflow: " + ctx.getName());

      String result = "";
      result += ctx.callActivity(ToUpperCaseActivity.class.getName(), "Tokyo", String.class).await() + ", ";
      result += ctx.callActivity(ToUpperCaseActivity.class.getName(), "London", String.class).await() + ", ";
      result += ctx.callActivity(ToUpperCaseActivity.class.getName(), "Seattle", String.class).await();

      ctx.getLogger().info("Workflow finished with result: " + result);
      ctx.complete(result);
    };
  }
}