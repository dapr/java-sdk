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

package io.dapr.examples.workflows.historypropagation.multiapp;

import io.dapr.durabletask.HistoryPropagationScope;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import io.dapr.workflows.WorkflowTaskOptions;

/**
 * Workflow running in app1 that calls a cross-app activity in app2 with LINEAGE
 * propagation. The app2 activity then receives this workflow's full execution
 * history via {@code ctx.getPropagatedHistory()}.
 */
public class App1Workflow implements Workflow {
  @Override
  public WorkflowStub create() {
    return ctx -> {
      var logger = ctx.getLogger();
      logger.info("=== App1Workflow starting (instance={}) ===", ctx.getInstanceId());

      String input = ctx.getInput(String.class);
      logger.info("App1Workflow input: {}", input);

      // Call an activity in app2 with both appId (cross-app) and LINEAGE propagation.
      WorkflowTaskOptions options = new WorkflowTaskOptions(
          null, null, "app2", HistoryPropagationScope.LINEAGE);

      String result = ctx.callActivity(
          App2AuditActivity.class.getName(),
          input,
          options,
          String.class
      ).await();

      logger.info("App1Workflow completed with: {}", result);
      ctx.complete(result);
    };
  }
}
