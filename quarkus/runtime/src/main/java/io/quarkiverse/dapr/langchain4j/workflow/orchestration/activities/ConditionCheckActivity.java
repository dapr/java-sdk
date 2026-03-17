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

package io.quarkiverse.dapr.langchain4j.workflow.orchestration.activities;

import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import io.quarkiverse.dapr.langchain4j.workflow.DaprPlannerRegistry;
import io.quarkiverse.dapr.langchain4j.workflow.DaprWorkflowPlanner;
import io.quarkiverse.dapr.langchain4j.workflow.orchestration.ConditionCheckInput;
import io.quarkiverse.dapr.workflows.ActivityMetadata;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Dapr WorkflowActivity that checks whether a conditional agent should execute.
 * Returns {@code true} if the agent's condition is met, {@code false} otherwise.
 */
@ApplicationScoped
@ActivityMetadata(name = "condition-check")
public class ConditionCheckActivity implements WorkflowActivity {

  @Override
  public Object run(WorkflowActivityContext ctx) {
    ConditionCheckInput input = ctx.getInput(ConditionCheckInput.class);
    DaprWorkflowPlanner planner = DaprPlannerRegistry.get(input.plannerId());
    if (planner == null) {
      throw new IllegalStateException("No planner found for ID: " + input.plannerId());
    }
    return planner.checkCondition(input.agentIndex());
  }
}
