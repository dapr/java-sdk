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
limitations under the License.
*/

package io.dapr.examples.workflows.historypropagation;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import io.dapr.workflows.WorkflowTaskOptions;

/**
 * Parent workflow demonstrating history propagation in a single-app scenario.
 *
 * <p>Flow:
 * <ol>
 *   <li>Parent calls a child sub-orchestrator with LINEAGE propagation - the child
 *       receives this workflow's full execution history plus any ancestor history.</li>
 *   <li>Parent calls an audit activity with OWN_HISTORY propagation - the activity
 *       receives only the immediate caller's events (no grandparent history).</li>
 * </ol>
 */
public class DemoHistoryPropagationWorkflow implements Workflow {
  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting parent workflow: " + ctx.getInstanceId());

      String input = ctx.getInput(String.class);

      // Step 1: child workflow with LINEAGE - gets full ancestor chain.
      String childResult = ctx.callChildWorkflow(
          DemoFraudCheckChildWorkflow.class.getName(),
          input,
          null,
          WorkflowTaskOptions.propagateLineage(),
          String.class
      ).await();
      ctx.getLogger().info("Child workflow returned: " + childResult);

      // Step 2: audit activity with OWN_HISTORY - sees only this workflow's events.
      String auditResult = ctx.callActivity(
          AuditActivity.class.getName(),
          input,
          WorkflowTaskOptions.propagateOwnHistory(),
          String.class
      ).await();
      ctx.getLogger().info("Audit activity returned: " + auditResult);

      ctx.complete("processed: " + input + " | child=" + childResult + " | audit=" + auditResult);
    };
  }
}
