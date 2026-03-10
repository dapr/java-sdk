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
import io.quarkiverse.dapr.langchain4j.agent.AgentRunContext;
import io.quarkiverse.dapr.langchain4j.agent.DaprAgentRunRegistry;
import io.quarkiverse.dapr.langchain4j.workflow.DaprPlannerRegistry;
import io.quarkiverse.dapr.langchain4j.workflow.DaprWorkflowPlanner;
import io.quarkiverse.dapr.langchain4j.workflow.DaprWorkflowPlanner.AgentMetadata;
import io.quarkiverse.dapr.langchain4j.workflow.orchestration.AgentExecInput;
import io.quarkiverse.dapr.workflows.ActivityMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
/**
 * Dapr WorkflowActivity that bridges the Dapr Workflow execution to the
 * LangChain4j planner. When invoked by an orchestration workflow alongside a
 * child {@link io.quarkiverse.dapr.langchain4j.agent.workflow.AgentRunWorkflow}, it:
 * <ol>
 *   <li>Looks up the planner from the registry.</li>
 *   <li>Creates a per-agent {@link AgentRunContext} and registers it.</li>
 *   <li>Submits the agent to the planner's exchange queue (along with its {@code agentRunId})
 *       so the planner can set {@link io.quarkiverse.dapr.langchain4j.agent.DaprAgentContextHolder}
 *       on the executing thread before tool calls begin.</li>
 *   <li>Returns immediately — the planner's {@code nextAction()} handles completion
 *       signaling (sending {@code "done"} to the AgentRunWorkflow, raising an external
 *       event to the orchestration workflow, and cleaning up the registry).</li>
 * </ol>
 *
 * <p>This activity is intentionally <b>non-blocking</b> to avoid exhausting the Dapr
 * activity thread pool when composite agents (e.g., a {@code @SequenceAgent} nested
 * inside a {@code @ParallelAgent}) spawn additional activities for their inner workflows.
 */

@ApplicationScoped
@ActivityMetadata(name = "agent-call")
public class AgentExecutionActivity implements WorkflowActivity {

  private static final Logger LOG = Logger.getLogger(AgentExecutionActivity.class);

  @Override
  public Object run(WorkflowActivityContext ctx) {
    AgentExecInput input = ctx.getInput(AgentExecInput.class);
    DaprWorkflowPlanner planner = DaprPlannerRegistry.get(input.plannerId());
    if (planner == null) {
      throw new IllegalStateException("No planner found for ID: " + input.plannerId()
          + ". Registered IDs: " + DaprPlannerRegistry.getRegisteredIds());
    }

    AgentMetadata metadata = planner.getAgentMetadata(input.agentIndex());
    String agentName = metadata.agentName();
    String agentRunId = input.agentRunId();

    LOG.infof("[Planner:%s] AgentExecutionActivity started — agent=%s, agentRunId=%s",
        input.plannerId(), agentName, agentRunId);

    AgentRunContext runContext = new AgentRunContext(agentRunId);
    DaprAgentRunRegistry.register(agentRunId, runContext);

    // Submit the agent to the planner's exchange queue (non-blocking).
    // The planner's nextAction() handles completion signaling and cleanup.
    planner.executeAgent(planner.getAgent(input.agentIndex()), agentRunId);

    LOG.infof("[Planner:%s] AgentExecutionActivity submitted — agent=%s, agentRunId=%s",
        input.plannerId(), agentName, agentRunId);

    return null;
  }
}
