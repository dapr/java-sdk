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

package io.dapr.quarkus.langchain4j.agent.recovery;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.dapr.quarkus.langchain4j.agent.DaprToolCallInterceptor;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import io.quarkiverse.dapr.workflows.ActivityMetadata;
import io.quarkus.arc.Arc;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dapr Workflow Activity that re-runs an agent's entire ReAct loop from scratch
 * after a crash. This is the core of agent-level crash recovery.
 *
 * <p>Unlike {@link io.dapr.quarkus.langchain4j.agent.activities.LlmCallActivity} which
 * relies on in-memory {@code PendingCall} objects, this activity is self-contained:
 * it calls {@code ChatModel.chat()} directly and invokes tools via {@link ToolRegistry}.
 *
 * <p>The activity runs as a single durable unit. If the process crashes during recovery,
 * Dapr will re-dispatch this activity from the beginning (the agent re-runs again).
 */
@ApplicationScoped
@ActivityMetadata(name = "recovery-agent")
public class RecoveryAgentActivity implements WorkflowActivity {

  private static final Logger LOG = Logger.getLogger(RecoveryAgentActivity.class);
  private static final int MAX_ITERATIONS = 10;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public Object run(WorkflowActivityContext ctx) {
    RecoveryAgentInput input = ctx.getInput(RecoveryAgentInput.class);

    LOG.infof("[Recovery:%s] Starting recovery for agent=%s",
        input.agentRunId(), input.agentName());

    ToolRegistry toolRegistry = Arc.container().instance(ToolRegistry.class).get();

    // Build tool specifications from the agent's @ToolBox classes
    List<ToolSpecification> toolSpecs = input.toolClassNames() != null
        ? toolRegistry.getToolSpecsForClasses(input.toolClassNames())
        : toolRegistry.getAllToolSpecs();

    LOG.infof("[Recovery:%s] Available tools: %d", input.agentRunId(), toolSpecs.size());

    ChatModel chatModel = resolveChatModel();

    // Build initial messages
    List<ChatMessage> messages = new ArrayList<>();
    if (input.systemMessage() != null && !input.systemMessage().isBlank()) {
      messages.add(new SystemMessage(input.systemMessage()));
    }
    if (input.userMessage() != null && !input.userMessage().isBlank()) {
      messages.add(new UserMessage(input.userMessage()));
    }

    int llmCalls = 0;
    int toolCalls = 0;

    // ReAct loop: call LLM, execute tools, repeat until text response
    for (int i = 0; i < MAX_ITERATIONS; i++) {
      LOG.infof("[Recovery:%s][iter:%d] Calling ChatModel with %d messages, %d tools",
          input.agentRunId(), i, messages.size(), toolSpecs.size());

      ChatRequest.Builder requestBuilder = ChatRequest.builder().messages(messages);
      if (!toolSpecs.isEmpty()) {
        requestBuilder.toolSpecifications(toolSpecs);
      }

      // Bypass DaprChatModelDecorator routing
      DaprToolCallInterceptor.IS_ACTIVITY_CALL.set(Boolean.TRUE);
      ChatResponse response;
      try {
        response = chatModel.chat(requestBuilder.build());
      } finally {
        DaprToolCallInterceptor.IS_ACTIVITY_CALL.remove();
      }
      llmCalls++;

      AiMessage aiMessage = response.aiMessage();
      messages.add(aiMessage);

      // Check if LLM returned tool calls
      if (aiMessage.toolExecutionRequests() != null
          && !aiMessage.toolExecutionRequests().isEmpty()) {
        LOG.infof("[Recovery:%s][iter:%d] LLM requested %d tool calls",
            input.agentRunId(), i, aiMessage.toolExecutionRequests().size());

        for (ToolExecutionRequest toolRequest : aiMessage.toolExecutionRequests()) {
          String toolName = toolRequest.name();
          String argsJson = toolRequest.arguments();

          LOG.infof("[Recovery:%s][iter:%d] Executing tool: %s(%s)",
              input.agentRunId(), i, toolName, argsJson);

          try {
            Object[] args = parseToolArguments(toolRegistry, toolName, argsJson);
            String result = toolRegistry.invokeTool(toolName, args);
            toolCalls++;

            LOG.infof("[Recovery:%s][iter:%d] Tool %s returned: %s",
                input.agentRunId(), i, toolName, truncate(result, 200));

            messages.add(new ToolExecutionResultMessage(
                toolRequest.id(), toolName, result));
          } catch (Exception e) {
            LOG.errorf("[Recovery:%s][iter:%d] Tool %s failed: %s",
                input.agentRunId(), i, toolName, e.getMessage());
            messages.add(new ToolExecutionResultMessage(
                toolRequest.id(), toolName, "Error: " + e.getMessage()));
          }
        }
      } else {
        // LLM returned text — agent is done
        String resultText = aiMessage.text();
        LOG.infof("[Recovery:%s] Recovery complete — llmCalls=%d, toolCalls=%d, result=%s",
            input.agentRunId(), llmCalls, toolCalls, truncate(resultText, 200));
        return new RecoveryAgentOutput(input.agentName(), resultText, llmCalls, toolCalls);
      }
    }

    // Max iterations reached
    LOG.warnf("[Recovery:%s] Max iterations (%d) reached — returning last response",
        input.agentRunId(), MAX_ITERATIONS);
    return new RecoveryAgentOutput(input.agentName(),
        "Recovery reached max iterations (" + MAX_ITERATIONS + ")",
        llmCalls, toolCalls);
  }

  /**
   * Parses tool arguments from the LLM's JSON string into an Object array
   * matching the tool method's parameter order.
   */
  private Object[] parseToolArguments(ToolRegistry toolRegistry, String toolName, String argsJson) {
    ToolRegistry.ToolEntry entry = toolRegistry.getToolEntry(toolName);
    if (entry == null) {
      return new Object[0];
    }

    Method method = entry.method();
    Parameter[] params = method.getParameters();
    if (params.length == 0) {
      return new Object[0];
    }

    try {
      Map<String, Object> argsMap = MAPPER.readValue(argsJson,
          new TypeReference<Map<String, Object>>() {
          });

      Object[] result = new Object[params.length];
      for (int i = 0; i < params.length; i++) {
        // Use Java parameter name (requires -parameters compiler flag) — this matches
        // how LangChain4j generates ToolSpecification parameter names. The @P annotation
        // value is the description, not the name.
        String paramName = params[i].getName();
        Object value = argsMap.get(paramName);
        if (value == null && argsMap.size() == 1 && params.length == 1) {
          // Single-param tool: use whatever key the LLM sent
          value = argsMap.values().iterator().next();
        }
        result[i] = convertArgument(value, params[i].getType());
      }
      return result;
    } catch (Exception e) {
      LOG.warnf("Failed to parse tool args for %s: %s — trying positional", toolName, e.getMessage());
      // Fallback: try single-argument tools
      if (params.length == 1) {
        return new Object[]{argsJson};
      }
      return new Object[params.length];
    }
  }

  /**
   * Converts a JSON value to the target parameter type.
   */
  private Object convertArgument(Object value, Class<?> targetType) {
    if (value == null) {
      return null;
    }
    if (targetType == String.class) {
      return String.valueOf(value);
    }
    if (targetType == int.class || targetType == Integer.class) {
      return value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(String.valueOf(value));
    }
    if (targetType == long.class || targetType == Long.class) {
      return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(String.valueOf(value));
    }
    if (targetType == double.class || targetType == Double.class) {
      return value instanceof Number ? ((Number) value).doubleValue() : Double.parseDouble(String.valueOf(value));
    }
    if (targetType == boolean.class || targetType == Boolean.class) {
      return value instanceof Boolean ? value : Boolean.parseBoolean(String.valueOf(value));
    }
    // For complex types, try Jackson conversion
    return MAPPER.convertValue(value, targetType);
  }

  private ChatModel resolveChatModel() {
    return Arc.container().instance(ChatModel.class).get();
  }

  private static String truncate(String s, int maxLength) {
    if (s == null) {
      return null;
    }
    return s.length() <= maxLength ? s : s.substring(0, maxLength) + "...";
  }
}
