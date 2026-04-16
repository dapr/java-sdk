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

package io.dapr.quarkus.langchain4j.agent.workflow;

import io.dapr.quarkus.langchain4j.agent.activities.LlmCallActivity;
import io.dapr.quarkus.langchain4j.agent.activities.LlmCallInput;
import io.dapr.quarkus.langchain4j.agent.activities.LlmCallOutput;
import io.dapr.quarkus.langchain4j.agent.activities.ToolCallActivity;
import io.dapr.quarkus.langchain4j.agent.activities.ToolCallInput;
import io.dapr.quarkus.langchain4j.agent.activities.ToolCallOutput;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import io.quarkiverse.dapr.workflows.WorkflowMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
/**
 * Dapr Workflow representing the execution of a single {@code @Agent}-annotated method,
 * including all tool and LLM calls the agent makes during its ReAct loop.
 *
 * <p><h3>Lifecycle</h3>
 * <ol>
 *   <li>Started by {@link io.dapr.quarkus.langchain4j.workflow.orchestration.activities.AgentExecutionActivity}
 *       (orchestration path) or lazily by {@link io.dapr.quarkus.langchain4j.agent.AgentRunLifecycleManager}
 *       (standalone {@code @Agent} path) just before the agent is submitted.</li>
 *   <li>Loops waiting for {@code "agent-event"} external events raised by
 *       {@link io.dapr.quarkus.langchain4j.agent.DaprToolCallInterceptor} and
 *       {@link io.dapr.quarkus.langchain4j.agent.DaprLlmCallInterceptor}.</li>
 *   <li>For each {@code "tool-call"} event, schedules a {@link ToolCallActivity} that
 *       executes the tool on the Dapr activity thread and returns a {@link ToolCallOutput}.</li>
 *   <li>For each {@code "llm-call"} event, schedules a {@link LlmCallActivity} that
 *       executes the LLM call on the Dapr activity thread and returns a {@link LlmCallOutput}.</li>
 *   <li>After each activity, updates the Dapr custom status with an {@link AgentRunOutput}
 *       snapshot so observers can follow execution progress in real time.</li>
 *   <li>Terminates when a {@code "done"} event is received, setting the final
 *       {@link AgentRunOutput} as the custom status.</li>
 * </ol>
 */

@ApplicationScoped
@WorkflowMetadata(name = "agent")
public class AgentRunWorkflow implements Workflow {

  private static final Logger LOG = Logger.getLogger(AgentRunWorkflow.class);

  @Override
  public WorkflowStub create() {
    return ctx -> {
      AgentRunInput input = ctx.getInput(AgentRunInput.class);
      String agentRunId = input.agentRunId();
      String agentName = input.agentName();

      LOG.infof("[AgentRun:%s] AgentRunWorkflow started — agent=%s, userMessage=%s, systemMessage=%s",
          agentRunId, agentName,
          truncate(input.userMessage(), 120),
          truncate(input.systemMessage(), 120));

      List<ToolCallOutput> toolCallOutputs = new ArrayList<>();
      List<LlmCallOutput> llmCallOutputs = new ArrayList<>();
      int eventIndex = 0;

      while (true) {
        LOG.infof("[AgentRun:%s][iter:%d] Waiting for agent-event (replay=%s)",
            agentRunId, eventIndex, ctx.isReplaying());
        AgentEvent event = ctx.waitForExternalEvent("agent-event", AgentEvent.class).await();
        LOG.infof("[AgentRun:%s][iter:%d] Received event: type=%s, callId=%s (replay=%s)",
            agentRunId, eventIndex, event.type(), event.toolCallId(), ctx.isReplaying());

        if ("done".equals(event.type())) {
          LOG.infof("[AgentRun:%s][iter:%d] Done — agent=%s, toolCalls=%d, llmCalls=%d",
              agentRunId, eventIndex, agentName, toolCallOutputs.size(), llmCallOutputs.size());
          break;
        }

        if ("tool-call".equals(event.type())) {
          LOG.infof("[AgentRun:%s][iter:%d] PRE-callActivity tool-call=%s (replay=%s)",
              agentRunId, eventIndex, event.toolName(), ctx.isReplaying());
          ToolCallOutput toolOutput = ctx.callActivity(
              "tool-call",
              new ToolCallInput(agentRunId, event.toolCallId(), event.toolName(), event.args()),
              ToolCallOutput.class).await();
          toolCallOutputs.add(toolOutput);
          LOG.infof("[AgentRun:%s][iter:%d] POST-callActivity tool-call=%s → %s",
              agentRunId, eventIndex, event.toolName(), toolOutput.result());
          ctx.setCustomStatus(new AgentRunOutput(agentName, toolCallOutputs, llmCallOutputs));
        }

        if ("llm-call".equals(event.type())) {
          LOG.infof("[AgentRun:%s][iter:%d] PRE-callActivity llm-call=%s (replay=%s)",
              agentRunId, eventIndex, event.toolName(), ctx.isReplaying());
          LlmCallOutput llmOutput = ctx.callActivity(
              "llm-call",
              new LlmCallInput(agentRunId, event.toolCallId(), event.toolName(), event.args()),
              LlmCallOutput.class).await();
          llmCallOutputs.add(llmOutput);
          LOG.infof("[AgentRun:%s][iter:%d] POST-callActivity llm-call=%s → %s",
              agentRunId, eventIndex, event.toolName(), llmOutput.response());
          ctx.setCustomStatus(new AgentRunOutput(agentName, toolCallOutputs, llmCallOutputs));
        }

        eventIndex++;
      }

      // Set the final output so it is visible in the Dapr workflow dashboard.
      ctx.setCustomStatus(new AgentRunOutput(agentName, toolCallOutputs, llmCallOutputs));
    };
  }

  private static String truncate(String s, int maxLength) {
    if (s == null) {
      return null;
    }
    String trimmed = s.strip();
    return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength) + "…";
  }
}
