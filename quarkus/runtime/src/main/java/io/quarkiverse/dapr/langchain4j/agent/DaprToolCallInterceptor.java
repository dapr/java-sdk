package io.quarkiverse.dapr.langchain4j.agent;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.jboss.logging.Logger;

import io.dapr.workflows.client.DaprWorkflowClient;
import io.quarkiverse.dapr.langchain4j.agent.workflow.AgentEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * CDI interceptor that routes {@code @Tool}-annotated method calls through a Dapr Workflow
 * Activity when executing inside a Dapr-backed agent run.
 * <p>
 * <h3>Execution flow (orchestration-driven)</h3>
 * When an agent is run via an orchestration workflow ({@code @SequenceAgent} etc.),
 * {@code AgentExecutionActivity} sets {@link DaprAgentContextHolder} before the agent starts.
 * Tool calls find a non-null {@code agentRunId} and are routed through
 * {@link io.quarkiverse.dapr.langchain4j.agent.activities.ToolCallActivity}.
 * <p>
 * <h3>Execution flow (standalone {@code @Agent})</h3>
 * When an {@code @Agent}-annotated method is called directly (without an orchestrator),
 * {@link DaprAgentContextHolder} is null on the first tool call. In this case the interceptor
 * calls {@link AgentRunLifecycleManager#getOrActivate()} to lazily start an
 * {@link io.quarkiverse.dapr.langchain4j.agent.workflow.AgentRunWorkflow} and set the context.
 * The workflow is terminated by {@link AgentRunLifecycleManager}'s {@code @PreDestroy} when the
 * CDI request scope ends.
 * <p>
 * <h3>Deadlock prevention</h3>
 * {@code ToolCallActivity} calls the {@code @Tool} method via reflection on the CDI proxy. This
 * would cause the interceptor to fire again. The {@link #IS_ACTIVITY_CALL} {@code ThreadLocal}
 * prevents recursion: when set on the activity thread, the interceptor calls {@code ctx.proceed()}
 * immediately without routing through Dapr.
 */
@DaprAgentToolInterceptorBinding
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class DaprToolCallInterceptor {

    private static final Logger LOG = Logger.getLogger(DaprToolCallInterceptor.class);

    /**
     * Thread-local flag set by {@code ToolCallActivity} to indicate that the current call
     * is the actual tool execution (not the routed interception), so the interceptor
     * should proceed normally.
     */
    public static final ThreadLocal<Boolean> IS_ACTIVITY_CALL = new ThreadLocal<>();

    @Inject
    DaprWorkflowClient workflowClient;

    @Inject
    Instance<AgentRunLifecycleManager> lifecycleManager;

    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        // If called from ToolCallActivity, this is the real execution — proceed normally.
        if (Boolean.TRUE.equals(IS_ACTIVITY_CALL.get())) {
            return ctx.proceed();
        }

        // Check whether we are inside a Dapr-backed agent run.
        String agentRunId = DaprAgentContextHolder.get();

        if (agentRunId == null) {
            // No orchestration context — try to lazily activate a workflow for this request.
            agentRunId = tryLazyActivate(ctx.getMethod().getName());
            if (agentRunId == null) {
                // Not in a CDI request scope (e.g., background thread) — execute directly.
                return ctx.proceed();
            }
        }

        AgentRunContext runCtx = DaprAgentRunRegistry.get(agentRunId);
        if (runCtx == null) {
            return ctx.proceed();
        }

        // Register this tool call and get a future for the result.
        String toolCallId = UUID.randomUUID().toString();
        CompletableFuture<Object> future = runCtx.registerCall(
                toolCallId,
                ctx.getTarget(),
                ctx.getMethod(),
                ctx.getParameters());

        String args = "";
        if (ctx.getParameters() != null) {
            args = Arrays.toString(ctx.getParameters());
        }

        LOG.infof("[AgentRun:%s][ToolCall:%s] Routing tool call through Dapr: method=%s, args=%s",
                agentRunId, toolCallId, ctx.getMethod().getName(), args);

        // Notify the AgentRunWorkflow that a tool call is waiting.
        workflowClient.raiseEvent(agentRunId, "agent-event",
                new AgentEvent("tool-call", toolCallId, ctx.getMethod().getName(), args));

        // Block the agent thread until ToolCallActivity completes the tool execution.
        return future.join();
    }

    /**
     * Lazily activates an {@link AgentRunLifecycleManager} for the current CDI request scope.
     * Returns the new {@code agentRunId}, or {@code null} if no request scope is active.
     */
    private String tryLazyActivate(String toolMethodName) {
        try {
            String agentRunId = lifecycleManager.get().getOrActivate();
            LOG.infof("[AgentRun:%s] Lazy activation triggered by first tool call: %s", agentRunId, toolMethodName);
            return agentRunId;
        } catch (Exception e) {
            LOG.debugf("Could not lazily activate AgentRunWorkflow (no active request scope?): %s", e.getMessage());
            return null;
        }
    }
}