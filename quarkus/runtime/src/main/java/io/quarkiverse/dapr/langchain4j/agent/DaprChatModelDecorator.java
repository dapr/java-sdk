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

package io.quarkiverse.dapr.langchain4j.agent;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.quarkiverse.dapr.langchain4j.agent.workflow.AgentEvent;
import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;
import org.jboss.logging.Logger;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
/**
 * CDI Decorator that routes {@code ChatModel.chat(ChatRequest)} calls through a Dapr
 * Workflow Activity when executing inside an active agent run.
 *
 * <p><h3>Why a decorator instead of a CDI interceptor</h3>
 * quarkus-langchain4j registers {@code ChatModel} as a <em>synthetic bean</em>
 * ({@code SyntheticBeanBuildItem}). Arc does not apply CDI interceptors to synthetic
 * beans based on {@code AnnotationsTransformer} modifications to the interface — the
 * synthetic bean proxy is generated without interceptor binding metadata. CDI decorators,
 * however, work at the <em>bean type</em> level and are applied by Arc to any bean (including
 * synthetic beans) whose types include the delegate type.
 *
 * <p><h3>Execution flow</h3>
 * <ol>
 *   <li>The LangChain4j AiService calls {@code chatModel.chat(request)} which routes
 *       through this decorator.</li>
 *   <li>If a Dapr agent run is active (identified by {@link DaprAgentContextHolder}), the
 *       decorator registers a {@link AgentRunContext.PendingCall} and raises an
 *       {@code "llm-call"} event to the running
 *       {@link io.quarkiverse.dapr.langchain4j.agent.workflow.AgentRunWorkflow}.</li>
 *   <li>The decorator blocks the agent thread on a {@link CompletableFuture}.</li>
 *   <li>{@link io.quarkiverse.dapr.langchain4j.agent.activities.LlmCallActivity} picks up
 *       the event, sets {@link DaprToolCallInterceptor#IS_ACTIVITY_CALL}, and re-invokes
 *       this decorator's {@code chat()} method via reflection on the stored target.</li>
 *   <li>The decorator sees {@code IS_ACTIVITY_CALL} set and passes through to
 *       {@code delegate.chat(request)} — executing the real LLM call on the Dapr
 *       activity thread.</li>
 *   <li>The result is returned to {@code LlmCallActivity}, which completes the future,
 *       unblocking the agent thread.</li>
 * </ol>
 *
 * <p><h3>Lazy activation</h3>
 * When an {@code @Agent} method is called standalone (no orchestration workflow),
 * the first LLM call will find no active {@code agentRunId} in {@link DaprAgentContextHolder}.
 * This decorator calls {@link AgentRunLifecycleManager#getOrActivate()} to lazily start
 * an {@link io.quarkiverse.dapr.langchain4j.agent.workflow.AgentRunWorkflow} so that all
 * subsequent LLM and tool calls are routed through Dapr.
 */

@Decorator
@Priority(Interceptor.Priority.APPLICATION)
@Dependent
public class DaprChatModelDecorator implements ChatModel {

  private static final Logger LOG = Logger.getLogger(DaprChatModelDecorator.class);

  @Inject
  @Delegate
  @Any
  ChatModel delegate;

  @Inject
  DaprWorkflowClient workflowClient;

  @Inject
  Instance<AgentRunLifecycleManager> lifecycleManager;

  /**
   * Explicit delegation for the {@code doChat()} template method.
   *
   * <p>The default {@link ChatModel#chat(ChatRequest)} implementation calls
   * {@code this.doChat(ChatRequest)} internally. Because our decorator only overrides
   * {@code chat()}, Arc does not generate a {@code doChat$superforward} bridge method in
   * the decorated bean's Arc subclass proxy. Without it, the CDI delegate proxy cannot
   * forward {@code doChat()} to the actual bean — it falls through to the interface
   * default which throws {@code "Not implemented"}.
   *
   * <p>Overriding {@code doChat()} here — even as a pure delegation — causes Arc to generate
   * the required bridge, so the internal {@code chat() → doChat()} chain resolves correctly
   * through the delegate to the actual {@code ChatModel} implementation.
   */
  @Override
  public ChatResponse doChat(ChatRequest request) {
    return delegate.doChat(request);
  }

  @Override
  public ChatResponse chat(ChatRequest request) {
    // If called from LlmCallActivity (IS_ACTIVITY_CALL is set), this is the real
    // execution — pass through to the real ChatModel without routing through Dapr.
    if (Boolean.TRUE.equals(DaprToolCallInterceptor.IS_ACTIVITY_CALL.get())) {
      return delegate.chat(request);
    }

    // Check whether we are inside a Dapr-backed agent run.
    String agentRunId = DaprAgentContextHolder.get();

    if (agentRunId == null) {
      // No orchestration context — try to lazily activate a workflow for this request.
      // The first event in the ReAct loop is always an LLM call, so this is typically
      // where the AgentRunWorkflow is started for standalone @Agent invocations.
      // Pass the rendered messages so they are recorded in the workflow input.
      agentRunId = tryLazyActivate(extractUserMessage(request), extractSystemMessage(request));
      if (agentRunId == null) {
        // Not in a CDI request scope (e.g., background thread) — execute directly.
        return delegate.chat(request);
      }
    }

    AgentRunContext runCtx = DaprAgentRunRegistry.get(agentRunId);
    if (runCtx == null) {
      return delegate.chat(request);
    }

    // Register this LLM call and get a future for the result.
    String llmCallId = UUID.randomUUID().toString();
    try {
      // Store (this, chat-method, request) so LlmCallActivity can re-invoke
      // this decorator's chat() with IS_ACTIVITY_CALL set, which passes through
      // to delegate.chat(request) — the real LLM execution.
      Method chatMethod = ChatModel.class.getMethod("chat", ChatRequest.class);
      CompletableFuture<Object> future = runCtx.registerCall(
          llmCallId, this, chatMethod, new Object[]{request});

      // Extract the prompt for observability in the workflow history.
      String prompt = extractPrompt(request);

      LOG.infof("[AgentRun:%s][LlmCall:%s] Routing LLM call through Dapr: chat()",
          agentRunId, llmCallId);

      // Notify the AgentRunWorkflow that an LLM call is waiting.
      // The prompt is passed as args so it is stored in the Dapr activity input.
      workflowClient.raiseEvent(agentRunId, "agent-event",
          new AgentEvent("llm-call", llmCallId, "chat", prompt));

      // Block the agent thread until LlmCallActivity completes the LLM execution.
      return (ChatResponse) future.join();

    } catch (NoSuchMethodException ex) {
      LOG.warnf("[AgentRun:%s][LlmCall:%s] Could not find chat(ChatRequest) via reflection"
          + " — falling back to direct call: %s", agentRunId, llmCallId, ex.getMessage());
      return delegate.chat(request);
    }
  }

  /**
   * Lazily activates an {@link AgentRunLifecycleManager} for the current CDI request scope,
   * recording the rendered user and system messages in the workflow input for observability.
   *
   * @param userMessage   the rendered user message extracted from the {@code ChatRequest}
   * @param systemMessage the rendered system message extracted from the {@code ChatRequest}
   * @return the new {@code agentRunId}, or {@code null} if no request scope is active
   */
  private String tryLazyActivate(String userMessage, String systemMessage) {
    try {
      // Check whether the generated CDI decorator stored @Agent metadata on this thread.
      // This provides the real agent name and annotation-level messages even when the
      // decorator's own getOrActivate() call failed and fell through to direct delegation.
      DaprAgentMetadataHolder.AgentMetadata metadata = DaprAgentMetadataHolder.get();
      String agentName = "standalone";
      if (metadata != null) {
        agentName = metadata.agentName();
        if (userMessage == null) {
          userMessage = metadata.userMessage();
        }
        if (systemMessage == null) {
          systemMessage = metadata.systemMessage();
        }
        DaprAgentMetadataHolder.clear();
      }
      String agentRunId = lifecycleManager.get().getOrActivate(agentName, userMessage, systemMessage);
      LOG.infof("[AgentRun:%s] Lazy activation triggered by first LLM call (agent=%s)",
          agentRunId, agentName);
      return agentRunId;
    } catch (Exception ex) {
      LOG.debugf("Could not lazily activate AgentRunWorkflow (no active request scope?): %s",
          ex.getMessage());
      return null;
    }
  }

  /**
   * Extracts the messages from a {@code ChatRequest} for observability in the workflow history.
   * Uses reflection to avoid a hard version-specific dependency on langchain4j internals.
   */
  private String extractPrompt(ChatRequest request) {
    if (request == null) {
      return null;
    }
    try {
      Object messages = request.getClass().getMethod("messages").invoke(request);
      return String.valueOf(messages);
    } catch (Exception ex) {
      return String.valueOf(request);
    }
  }

  /**
   * Extracts the last (most recent) user message text from the {@code ChatRequest}.
   * Uses reflection to remain decoupled from specific langchain4j internals.
   */
  private String extractUserMessage(ChatRequest request) {
    if (request == null) {
      return null;
    }
    try {
      List<?> messages = (List<?>) request.getClass().getMethod("messages").invoke(request);
      for (int ii = messages.size() - 1; ii >= 0; ii--) {
        Object msg = messages.get(ii);
        if ("UserMessage".equals(msg.getClass().getSimpleName())) {
          try {
            return (String) msg.getClass().getMethod("singleText").invoke(msg);
          } catch (ReflectiveOperationException ex) {
            return String.valueOf(msg);
          }
        }
      }
    } catch (ReflectiveOperationException ignored) {
      // intentionally empty
    }
    return null;
  }

  /**
   * Extracts the system message text from the {@code ChatRequest}.
   * Uses reflection to remain decoupled from specific langchain4j internals.
   */
  private String extractSystemMessage(ChatRequest request) {
    if (request == null) {
      return null;
    }
    try {
      List<?> messages = (List<?>) request.getClass().getMethod("messages").invoke(request);
      for (Object msg : messages) {
        if ("SystemMessage".equals(msg.getClass().getSimpleName())) {
          try {
            return (String) msg.getClass().getMethod("text").invoke(msg);
          } catch (ReflectiveOperationException ex) {
            return String.valueOf(msg);
          }
        }
      }
    } catch (ReflectiveOperationException ignored) {
      // intentionally empty
    }
    return null;
  }
}
