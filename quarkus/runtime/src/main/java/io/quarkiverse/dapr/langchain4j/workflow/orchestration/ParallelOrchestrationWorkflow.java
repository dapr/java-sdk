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

package io.quarkiverse.dapr.langchain4j.workflow.orchestration;

import io.dapr.durabletask.Task;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import io.quarkiverse.dapr.langchain4j.agent.workflow.AgentRunInput;
import io.quarkiverse.dapr.langchain4j.workflow.DaprPlannerRegistry;
import io.quarkiverse.dapr.langchain4j.workflow.DaprWorkflowPlanner;
import io.quarkiverse.dapr.langchain4j.workflow.DaprWorkflowPlanner.AgentMetadata;
import io.quarkiverse.dapr.workflows.WorkflowMetadata;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

/**
 * Dapr Workflow that executes all agents in parallel and waits for all to complete.
 *
 * <p>Each agent is run as a child {@code AgentRunWorkflow} with a non-blocking bridging
 * activity for planner coordination. Completion is detected via external events
 * raised by {@link DaprWorkflowPlanner#nextAction}.
 */
@ApplicationScoped
@WorkflowMetadata(name = "parallel-agent")
public class ParallelOrchestrationWorkflow implements Workflow {

  @Override
  public WorkflowStub create() {
    return ctx -> {
      OrchestrationInput input = ctx.getInput(OrchestrationInput.class);
      DaprWorkflowPlanner planner = DaprPlannerRegistry.get(input.plannerId());

      List<Task<Void>> childWorkflows = new ArrayList<>();
      List<Task<Void>> submitTasks = new ArrayList<>();
      List<Task<Void>> completionEvents = new ArrayList<>();
      for (int i = 0; i < input.agentCount(); i++) {
        String agentRunId = input.plannerId() + ":" + i;
        AgentMetadata metadata = planner.getAgentMetadata(i);
        AgentRunInput agentInput = new AgentRunInput(agentRunId, metadata.agentName(),
            metadata.userMessage(), metadata.systemMessage());

        // Start AgentRunWorkflow as a child workflow for proper nesting
        childWorkflows.add(ctx.callChildWorkflow("agent", agentInput, agentRunId, Void.class));
        // Submit agent to planner (non-blocking activity -- returns immediately)
        submitTasks.add(ctx.callActivity("agent-call",
            new AgentExecInput(input.plannerId(), i, agentRunId), Void.class));
        // Register event listener for agent completion (signaled by planner's nextAction)
        completionEvents.add(
            ctx.waitForExternalEvent("agent-complete-" + agentRunId, Void.class));
      }
      // Wait for all agents to be submitted
      ctx.allOf(submitTasks).await();
      // Wait for all agents to complete (planner raises events after each agent finishes)
      ctx.allOf(completionEvents).await();
      // Wait for all child AgentRunWorkflows to finish (they received "done" from planner)
      ctx.allOf(childWorkflows).await();
      // Signal planner that the workflow has completed
      if (planner != null) {
        planner.signalWorkflowComplete();
      }
    };
  }
}
