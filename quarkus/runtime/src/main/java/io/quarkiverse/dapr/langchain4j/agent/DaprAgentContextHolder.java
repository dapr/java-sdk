package io.quarkiverse.dapr.langchain4j.agent;

/**
 * Thread-local holder for the current Dapr agent run ID.
 * <p>
 * Set by {@link io.quarkiverse.dapr.langchain4j.workflow.DaprWorkflowPlanner} before an agent
 * begins execution, so that {@link DaprToolCallInterceptor} can detect when a tool call
 * is happening inside a Dapr-backed agent and route it through a Dapr Workflow Activity.
 */
public class DaprAgentContextHolder {

    private static final ThreadLocal<String> AGENT_RUN_ID = new ThreadLocal<>();

    private DaprAgentContextHolder() {
    }

    public static void set(String agentRunId) {
        AGENT_RUN_ID.set(agentRunId);
    }

    public static String get() {
        return AGENT_RUN_ID.get();
    }

    public static void clear() {
        AGENT_RUN_ID.remove();
    }
}