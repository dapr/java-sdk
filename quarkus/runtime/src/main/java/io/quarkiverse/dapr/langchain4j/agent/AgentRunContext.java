package io.quarkiverse.dapr.langchain4j.agent;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the synchronization state for a single agent execution.
 * <p>
 * When a {@code @Tool}-annotated method is intercepted by {@link DaprToolCallInterceptor},
 * it registers a {@link PendingCall} here and blocks until
 * {@link io.quarkiverse.dapr.langchain4j.agent.activities.ToolCallActivity} executes the
 * tool on the Dapr Workflow Activity thread and completes the future.
 */
public class AgentRunContext {

    /**
     * Holds all the information needed for {@code ToolCallActivity} to execute the tool
     * and unblock the waiting agent thread.
     */
    public record PendingCall(
            Object target,
            Method method,
            Object[] args,
            CompletableFuture<Object> resultFuture) {
    }

    private final String agentRunId;
    private final Map<String, PendingCall> pendingCalls = new ConcurrentHashMap<>();

    public AgentRunContext(String agentRunId) {
        this.agentRunId = agentRunId;
    }

    public String getAgentRunId() {
        return agentRunId;
    }

    /**
     * Register a pending tool call and return the future that will be completed by
     * {@code ToolCallActivity} once the tool has executed.
     */
    public CompletableFuture<Object> registerCall(String toolCallId, Object target, Method method, Object[] args) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        pendingCalls.put(toolCallId, new PendingCall(target, method, args, future));
        return future;
    }

    /**
     * Returns the pending call for the given tool call ID without removing it.
     * Used by {@code ToolCallActivity} to retrieve call details.
     */
    public PendingCall getPendingCall(String toolCallId) {
        return pendingCalls.get(toolCallId);
    }

    /**
     * Complete the pending call with a successful result. Removes the entry and
     * unblocks the agent thread waiting in {@link DaprToolCallInterceptor}.
     */
    public void completeCall(String toolCallId, Object result) {
        PendingCall call = pendingCalls.remove(toolCallId);
        if (call != null) {
            call.resultFuture().complete(result);
        }
    }

    /**
     * Complete the pending call with an exception. Removes the entry and
     * propagates the failure to the waiting agent thread.
     */
    public void failCall(String toolCallId, Throwable cause) {
        PendingCall call = pendingCalls.remove(toolCallId);
        if (call != null) {
            call.resultFuture().completeExceptionally(cause);
        }
    }
}