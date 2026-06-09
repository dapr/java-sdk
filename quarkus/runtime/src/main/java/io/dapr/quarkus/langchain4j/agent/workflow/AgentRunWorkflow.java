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

import io.dapr.durabletask.TaskFailedException;
import io.dapr.quarkus.langchain4j.agent.activities.LlmCallInput;
import io.dapr.quarkus.langchain4j.agent.activities.LlmCallOutput;
import io.dapr.quarkus.langchain4j.agent.activities.ToolCallInput;
import io.dapr.quarkus.langchain4j.agent.activities.ToolCallOutput;
import io.dapr.quarkus.langchain4j.agent.recovery.RecoveryAgentInput;
import io.dapr.quarkus.langchain4j.agent.recovery.RecoveryAgentOutput;
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
 * <p><h3>Lifecycle (normal path)</h3>
 * <ol>
 *   <li>Started by {@link io.dapr.quarkus.langchain4j.workflow.orchestration.activities.AgentExecutionActivity}
 *       (orchestration path) or lazily by {@link io.dapr.quarkus.langchain4j.agent.AgentRunLifecycleManager}
 *       (standalone {@code @Agent} path) just before the agent is submitted.</li>
 *   <li>Loops waiting for {@code "agent-event"} external events raised by
 *       {@link io.dapr.quarkus.langchain4j.agent.DaprToolCallInterceptor} and
 *       {@link io.dapr.quarkus.langchain4j.agent.DaprChatModelDecorator}.</li>
 *   <li>For each {@code "tool-call"} event, schedules a
 *       {@link io.dapr.quarkus.langchain4j.agent.activities.ToolCallActivity}.</li>
 *   <li>For each {@code "llm-call"} event, schedules a
 *       {@link io.dapr.quarkus.langchain4j.agent.activities.LlmCallActivity}.</li>
 *   <li>Terminates when a {@code "done"} event is received.</li>
 * </ol>
 *
 * <p><h3>Crash recovery</h3>
 * After a crash, the Dapr runtime replays this workflow. Cached events and activities return
 * instantly. When replay reaches the activity that was in-progress during the crash, the
 * activity is re-dispatched but fails because the in-memory {@code AgentRunContext} is gone.
 * The workflow catches this failure and calls
 * {@link io.dapr.quarkus.langchain4j.agent.recovery.RecoveryAgentActivity} to re-run the
 * agent's ReAct loop from scratch.
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

        try {
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
            ctx.setCustomStatus(new AgentRunOutput(agentName, toolCallOutputs, llmCallOutputs, null));
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
            ctx.setCustomStatus(new AgentRunOutput(agentName, toolCallOutputs, llmCallOutputs, null));
          }
        } catch (TaskFailedException e) {
          // Activity failed — the in-memory AgentRunContext is gone after a crash.
          // Enter recovery mode. (Do NOT catch Exception — the framework uses
          // OrchestratorBlockedException for internal yield, which must propagate.)
          LOG.warnf("[AgentRun:%s][iter:%d] Activity failed (crash recovery): %s",
              agentRunId, eventIndex, e.getMessage());
          enterRecovery(ctx, input, agentRunId, agentName, toolCallOutputs, llmCallOutputs);
          return;
        }

        eventIndex++;
      }

      // Set the final output so it is visible in the Dapr workflow dashboard.
      ctx.setCustomStatus(new AgentRunOutput(agentName, toolCallOutputs, llmCallOutputs, null));
    };
  }

  /**
   * Enters crash recovery mode: calls {@link io.dapr.quarkus.langchain4j.agent.recovery.RecoveryAgentActivity}
   * to re-run the agent's entire ReAct loop from scratch using the original prompt.
   */
  private void enterRecovery(io.dapr.workflows.WorkflowContext ctx,
      AgentRunInput input, String agentRunId, String agentName,
      List<ToolCallOutput> toolCallOutputs, List<LlmCallOutput> llmCallOutputs) {

    LOG.infof("[AgentRun:%s] Starting recovery — re-running agent=%s from scratch",
        agentRunId, agentName);

    RecoveryAgentInput recoveryInput = new RecoveryAgentInput(
        agentRunId, agentName, input.systemMessage(), input.userMessage(),
        input.toolClassNames());

    RecoveryAgentOutput recoveryOutput = ctx.callActivity(
        "recovery-agent", recoveryInput, RecoveryAgentOutput.class).await();

    LOG.infof("[AgentRun:%s] Recovery complete — llmCalls=%d, toolCalls=%d",
        agentRunId, recoveryOutput.llmCalls(), recoveryOutput.toolCalls());

    // Set the recovery result as the custom status
    ctx.setCustomStatus(new AgentRunOutput(agentName, toolCallOutputs, llmCallOutputs,
        recoveryOutput.result()));
  }

  private static String truncate(String s, int maxLength) {
    if (s == null) {
      return null;
    }
    String trimmed = s.strip();
    return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength) + "…";
  }
}
