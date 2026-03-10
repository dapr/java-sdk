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

package io.quarkiverse.dapr.langchain4j.workflow.orchestration;

import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import io.quarkiverse.dapr.langchain4j.agent.workflow.AgentRunInput;
import io.quarkiverse.dapr.langchain4j.workflow.DaprPlannerRegistry;
import io.quarkiverse.dapr.langchain4j.workflow.DaprWorkflowPlanner;
import io.quarkiverse.dapr.langchain4j.workflow.DaprWorkflowPlanner.AgentMetadata;
import io.quarkiverse.dapr.workflows.WorkflowMetadata;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Dapr Workflow that conditionally executes agents based on runtime predicates.
 * For each agent, a condition check activity determines whether to execute it.
 * Each agent is run as a child {@code AgentRunWorkflow} with a non-blocking bridging
 * activity for planner coordination. Completion is detected via external events
 * raised by {@link DaprWorkflowPlanner#nextAction}.
 */
@ApplicationScoped
@WorkflowMetadata(name = "conditional-agent")
public class ConditionalOrchestrationWorkflow implements Workflow {

  @Override
  public WorkflowStub create() {
    return ctx -> {
      OrchestrationInput input = ctx.getInput(OrchestrationInput.class);
      DaprWorkflowPlanner planner = DaprPlannerRegistry.get(input.plannerId());

      for (int i = 0; i < input.agentCount(); i++) {
        boolean shouldExec = ctx.callActivity("condition-check",
            new ConditionCheckInput(input.plannerId(), i),
            Boolean.class).await();
        if (shouldExec) {
          String agentRunId = input.plannerId() + ":" + i;
          AgentMetadata metadata = planner.getAgentMetadata(i);
          AgentRunInput agentInput = new AgentRunInput(agentRunId, metadata.agentName(),
              metadata.userMessage(), metadata.systemMessage());

          var childWorkflow = ctx.callChildWorkflow("agent", agentInput, agentRunId, Void.class);
          // Submit agent to planner (non-blocking activity — returns immediately)
          ctx.callActivity("agent-call",
              new AgentExecInput(input.plannerId(), i, agentRunId), Void.class).await();
          // Wait for agent completion (signaled by planner's nextAction)
          ctx.waitForExternalEvent("agent-complete-" + agentRunId, Void.class).await();
          childWorkflow.await();
        }
      }
      // Signal planner that the workflow has completed
      if (planner != null) {
        planner.signalWorkflowComplete();
      }
    };
  }
}
