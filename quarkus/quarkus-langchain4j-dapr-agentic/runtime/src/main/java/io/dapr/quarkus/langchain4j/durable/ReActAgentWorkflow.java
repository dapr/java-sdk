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

package io.dapr.quarkus.langchain4j.durable;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import io.dapr.durabletask.Task;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowContext;
import io.dapr.workflows.WorkflowStub;
import io.quarkiverse.dapr.workflows.WorkflowMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Durable ReAct loop for a single {@code @Agent}, run as a Dapr Workflow (control-inversion
 * approach — the workflow <em>owns</em> the loop instead of recording an in-memory one).
 *
 * <p>This is the reimplementation of LangChain4j's {@code ToolService.executeInferenceAndToolsLoop}
 * against a {@link io.dapr.workflows.WorkflowContext}: the only non-deterministic steps — the
 * model call and each tool call — are activities ({@code agent-llm} / {@code agent-tool}), and
 * the loop control is deterministic given the recorded activity outputs.
 *
 * <h2>Why this gives durability, recovery and scale for free</h2>
 * <ul>
 *   <li><b>Per-call recovery</b>: on replay, completed {@code agent-llm}/{@code agent-tool}
 *       calls return from history; the loop resumes at the next call.</li>
 *   <li><b>Horizontal scale</b>: activities are pure functions of their input, so Dapr's
 *       random activity placement across replicas is correct — there is no in-memory per-run
 *       context, blocked future, or {@code ThreadLocal}.</li>
 *   <li><b>Observability</b>: every step is a history entry; {@link AgentStatus} is published
 *       as custom status.</li>
 * </ul>
 *
 * <p>Normally started through the drop-in {@code @Agent} proxy entry point (which renders the
 * prompt from the method args); it can also be scheduled directly via
 * {@code DaprWorkflowClient.scheduleNewWorkflow("react-agent", input, id)}. Long agents should
 * periodically {@code ctx.continueAsNew(...)} to cap history.
 */
@ApplicationScoped
@WorkflowMetadata(name = "react-agent")
public class ReActAgentWorkflow implements Workflow {

  private static final Logger LOG = Logger.getLogger(ReActAgentWorkflow.class);
  private static final int DEFAULT_MAX_STEPS = 16;

  @Override
  public WorkflowStub create() {
    return ctx -> {
      ReActInput input = ctx.getInput(ReActInput.class);

      // Deterministic seed: prior memory (if any) + rendered system/user messages.
      List<ChatMessage> messages = new ArrayList<>();
      if (input.priorMessagesJson() != null && !input.priorMessagesJson().isBlank()) {
        messages.addAll(ChatMessageDeserializer.messagesFromJson(input.priorMessagesJson()));
      }
      if (input.systemMessage() != null && !input.systemMessage().isBlank()) {
        messages.add(SystemMessage.from(input.systemMessage()));
      }
      messages.add(UserMessage.from(input.userMessage()));

      int stepsLeft = input.maxSteps() > 0 ? input.maxSteps() : DEFAULT_MAX_STEPS;
      while (stepsLeft-- > 0) {

        // (1) Model call as an activity — replayed from history on recovery, runs on any replica.
        LlmResult llm = ctx.callActivity("agent-llm",
            new LlmInput(input.agentName(), ChatMessageSerializer.messagesToJson(messages)),
            LlmResult.class).await();
        AiMessage aiMessage = (AiMessage) ChatMessageDeserializer.messageFromJson(llm.aiMessageJson());
        messages.add(aiMessage);
        ctx.setCustomStatus(new AgentStatus(
            input.agentName(), messages.size(), !aiMessage.hasToolExecutionRequests()));

        if (!aiMessage.hasToolExecutionRequests()) {
          saveMemory(ctx, input, messages);
          ctx.complete(aiMessage.text());
          return;
        }

        // (2) Tool calls as activities, fanned out in parallel — each replica-agnostic.
        List<Task<ToolResult>> toolTasks = new ArrayList<>();
        for (ToolExecutionRequest request : aiMessage.toolExecutionRequests()) {
          toolTasks.add(ctx.callActivity("agent-tool",
              new ToolInput(input.agentName(), request.id(), request.name(), request.arguments()),
              ToolResult.class));
        }
        for (ToolResult result : ctx.allOf(toolTasks).await()) {
          messages.add(ToolExecutionResultMessage.from(
              result.toolCallId(), result.toolName(), result.resultText()));
        }
      }

      LOG.warnf("[ReAct:%s] exceeded maxSteps without a final answer", input.agentName());
      throw new IllegalStateException(
          "Agent '" + input.agentName() + "' exceeded maxSteps without a final answer");
    };
  }

  /**
   * Persists the conversation for a {@code @MemoryId} agent via the {@code memory-save} activity
   * (durable + idempotent). The system message is excluded — it is regenerated from the template
   * each turn, not part of the stored history.
   */
  private static void saveMemory(WorkflowContext ctx, ReActInput input, List<ChatMessage> messages) {
    if (input.memoryId() == null || input.memoryId().isBlank()) {
      return;
    }
    List<ChatMessage> toPersist = new ArrayList<>();
    for (ChatMessage message : messages) {
      if (!(message instanceof SystemMessage)) {
        toPersist.add(message);
      }
    }
    ctx.callActivity("memory-save",
        new MemorySaveInput(input.memoryId(), ChatMessageSerializer.messagesToJson(toPersist)),
        String.class).await();
  }
}
