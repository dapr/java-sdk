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
 * Dapr Workflow Activity that executes a single {@code @Tool}-annotated method call on
 * behalf of a running {@link io.quarkiverse.dapr.langchain4j.agent.workflow.AgentRunWorkflow}.
 * <p>
 * <h3>How it works</h3>
 * <ol>
 *   <li>Receives {@link ToolCallInput} with the {@code agentRunId} and {@code toolCallId}.</li>
 *   <li>Looks up the {@link AgentRunContext} from {@link DaprAgentRunRegistry}.</li>
 *   <li>Retrieves the {@link AgentRunContext.PendingCall} registered by
 *       {@link io.quarkiverse.dapr.langchain4j.agent.DaprToolCallInterceptor}.</li>
 *   <li>Sets {@link DaprToolCallInterceptor#IS_ACTIVITY_CALL} on this thread so that
 *       the CDI interceptor passes through when the method is called via the CDI proxy.</li>
 *   <li>Invokes the {@code @Tool} method via reflection on the CDI proxy.</li>
 *   <li>Completes the {@code CompletableFuture} stored in the pending call, unblocking
 *       the agent thread waiting in {@code DaprToolCallInterceptor.intercept()}.</li>
 * </ol>
 */
@ApplicationScoped
@ActivityMetadata(name = "tool-call")
public class ToolCallActivity implements WorkflowActivity {

    private static final Logger LOG = Logger.getLogger(ToolCallActivity.class);

    @Override
    public Object run(WorkflowActivityContext ctx) {
        ToolCallInput input = ctx.getInput(ToolCallInput.class);

        LOG.infof("[AgentRun:%s][ToolCall:%s] ToolCallActivity started — tool=%s, args=%s",
                input.agentRunId(), input.toolCallId(), input.toolName(), input.args());

        AgentRunContext runCtx = DaprAgentRunRegistry.get(input.agentRunId());
        if (runCtx == null) {
            throw new IllegalStateException(
                    "No AgentRunContext found for agentRunId: " + input.agentRunId()
                            + ". Registered IDs: " + DaprAgentRunRegistry.getRegisteredIds());
        }

        AgentRunContext.PendingCall pendingCall = runCtx.getPendingCall(input.toolCallId());
        if (pendingCall == null) {
            throw new IllegalStateException(
                    "No PendingCall found for toolCallId: " + input.toolCallId()
                            + " in agentRunId: " + input.agentRunId());
        }

        LOG.infof("[AgentRun:%s][ToolCall:%s] Executing tool method: %s",
                input.agentRunId(), input.toolCallId(), pendingCall.method().getName());

        // Set the flag so the CDI interceptor passes through on this thread.
        DaprToolCallInterceptor.IS_ACTIVITY_CALL.set(Boolean.TRUE);
        try {
            // Invoke the @Tool method via the CDI proxy.
            // The CDI interceptor will fire again but pass through because IS_ACTIVITY_CALL is set.
            Object result = pendingCall.method().invoke(pendingCall.target(), pendingCall.args());
            String resultStr = String.valueOf(result);
            runCtx.completeCall(input.toolCallId(), result);
            LOG.infof("[AgentRun:%s][ToolCall:%s] Tool method completed: %s → %s",
                    input.agentRunId(), input.toolCallId(), pendingCall.method().getName(), resultStr);
            return new ToolCallOutput(input.toolName(), input.args(), resultStr);
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getCause() != null ? ite.getCause() : ite;
            LOG.errorf("[AgentRun:%s][ToolCall:%s] Tool method failed: %s — %s",
                    input.agentRunId(), input.toolCallId(), pendingCall.method().getName(), cause.getMessage());
            runCtx.failCall(input.toolCallId(), cause);
            throw new RuntimeException("Tool execution failed: " + pendingCall.method().getName(), cause);
        } catch (Exception e) {
            LOG.errorf("[AgentRun:%s][ToolCall:%s] Tool method failed: %s — %s",
                    input.agentRunId(), input.toolCallId(), pendingCall.method().getName(), e.getMessage());
            runCtx.failCall(input.toolCallId(), e);
            throw new RuntimeException("Tool execution failed: " + pendingCall.method().getName(), e);
        } finally {
            DaprToolCallInterceptor.IS_ACTIVITY_CALL.remove();
        }
    }
}