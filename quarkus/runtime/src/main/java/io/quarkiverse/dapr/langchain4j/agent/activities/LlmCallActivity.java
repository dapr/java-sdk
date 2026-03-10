package io.quarkiverse.dapr.langchain4j.agent.activities;

import io.quarkiverse.dapr.workflows.ActivityMetadata;
import org.jboss.logging.Logger;

import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import io.quarkiverse.dapr.langchain4j.agent.AgentRunContext;
import io.quarkiverse.dapr.langchain4j.agent.DaprAgentRunRegistry;
import io.quarkiverse.dapr.langchain4j.agent.DaprToolCallInterceptor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Dapr Workflow Activity that executes a single {@code ChatModel.chat(ChatRequest)} call on
 * behalf of a running {@link io.quarkiverse.dapr.langchain4j.agent.workflow.AgentRunWorkflow}.
 * <p>
 * <h3>How it works</h3>
 * <ol>
 *   <li>Receives {@link LlmCallInput} with the {@code agentRunId}, {@code llmCallId},
 *       {@code methodName}, and the serialized {@code prompt} (messages sent to the LLM).</li>
 *   <li>Looks up the {@link AgentRunContext} from {@link DaprAgentRunRegistry}.</li>
 *   <li>Retrieves the {@link AgentRunContext.PendingCall} registered by
 *       {@link io.quarkiverse.dapr.langchain4j.agent.DaprLlmCallInterceptor}.</li>
 *   <li>Sets {@link DaprToolCallInterceptor#IS_ACTIVITY_CALL} on this thread so that
 *       {@code DaprChatModelDecorator} passes through to {@code delegate.chat()} when
 *       re-invoked via reflection on the stored decorator instance.</li>
 *   <li>Invokes the {@code ChatModel} method via reflection on the decorator instance.</li>
 *   <li>Extracts the response text from the {@code ChatResponse} via reflection
 *       ({@code aiMessage().text()}) and returns a {@link LlmCallOutput} containing the
 *       method name and response text — stored in the Dapr workflow history.</li>
 *   <li>Completes the {@code CompletableFuture} in the pending call, unblocking
 *       the agent thread waiting in {@code DaprChatModelDecorator.chat()}.</li>
 * </ol>
 */
@ApplicationScoped
@ActivityMetadata(name = "llm-call")
public class LlmCallActivity implements WorkflowActivity {

    private static final Logger LOG = Logger.getLogger(LlmCallActivity.class);

    @Override
    public Object run(WorkflowActivityContext ctx) {
        LlmCallInput input = ctx.getInput(LlmCallInput.class);

        LOG.infof("[AgentRun:%s][LlmCall:%s] LlmCallActivity started — method=%s",
                input.agentRunId(), input.llmCallId(), input.methodName());
        if (input.prompt() != null) {
            LOG.debugf("[AgentRun:%s][LlmCall:%s] Prompt:\n%s",
                    input.agentRunId(), input.llmCallId(), input.prompt());
        }

        AgentRunContext runCtx = DaprAgentRunRegistry.get(input.agentRunId());
        if (runCtx == null) {
            throw new IllegalStateException(
                    "No AgentRunContext found for agentRunId: " + input.agentRunId()
                            + ". Registered IDs: " + DaprAgentRunRegistry.getRegisteredIds());
        }

        AgentRunContext.PendingCall pendingCall = runCtx.getPendingCall(input.llmCallId());
        if (pendingCall == null) {
            throw new IllegalStateException(
                    "No PendingCall found for llmCallId: " + input.llmCallId()
                            + " in agentRunId: " + input.agentRunId());
        }

        LOG.infof("[AgentRun:%s][LlmCall:%s] Executing LLM call: %s",
                input.agentRunId(), input.llmCallId(), pendingCall.method().getName());

        // Set the flag so DaprChatModelDecorator passes through on this thread instead of routing.
        DaprToolCallInterceptor.IS_ACTIVITY_CALL.set(Boolean.TRUE);
        try {
            // Invoke chat() on the stored DaprChatModelDecorator instance via reflection.
            // IS_ACTIVITY_CALL is set, so the decorator calls delegate.chat() directly.
            Object result = pendingCall.method().invoke(pendingCall.target(), pendingCall.args());
            String responseText = extractResponseText(result);
            runCtx.completeCall(input.llmCallId(), result);
            LOG.infof("[AgentRun:%s][LlmCall:%s] LLM call completed: %s → %s",
                    input.agentRunId(), input.llmCallId(), pendingCall.method().getName(), responseText);
            return new LlmCallOutput(input.methodName(), input.prompt(), responseText);
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getCause() != null ? ite.getCause() : ite;
            LOG.errorf("[AgentRun:%s][LlmCall:%s] LLM call failed: %s — %s",
                    input.agentRunId(), input.llmCallId(), pendingCall.method().getName(), cause.getMessage());
            runCtx.failCall(input.llmCallId(), cause);
            throw new RuntimeException("LLM call failed: " + pendingCall.method().getName(), cause);
        } catch (Exception e) {
            LOG.errorf("[AgentRun:%s][LlmCall:%s] LLM call failed: %s — %s",
                    input.agentRunId(), input.llmCallId(), pendingCall.method().getName(), e.getMessage());
            runCtx.failCall(input.llmCallId(), e);
            throw new RuntimeException("LLM call failed: " + pendingCall.method().getName(), e);
        } finally {
            DaprToolCallInterceptor.IS_ACTIVITY_CALL.remove();
        }
    }

    /**
     * Extracts the AI response text from a {@code ChatResponse} object using reflection,
     * avoiding a hard compile-time dependency on a specific LangChain4j package path.
     * Calls {@code chatResponse.aiMessage().text()} if available; falls back to
     * {@code String.valueOf(result)} otherwise.
     */
    private String extractResponseText(Object result) {
        if (result == null) {
            return null;
        }
        try {
            Object aiMessage = result.getClass().getMethod("aiMessage").invoke(result);
            if (aiMessage != null) {
                Object text = aiMessage.getClass().getMethod("text").invoke(aiMessage);
                return String.valueOf(text);
            }
        } catch (Exception ignored) {
            // Not a ChatResponse or missing expected methods — fall through.
        }
        return String.valueOf(result);
    }
}
