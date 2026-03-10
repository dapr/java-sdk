package io.quarkiverse.dapr.langchain4j.agent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static registry that maps agent run IDs to their {@link AgentRunContext}.
 * <p>
 * Similar to {@link io.quarkiverse.dapr.langchain4j.workflow.DaprPlannerRegistry} but for
 * individual agent executions. Allows {@link io.quarkiverse.dapr.langchain4j.agent.activities.ToolCallActivity}
 * to look up the in-progress context for a given agent run ID.
 */
public class DaprAgentRunRegistry {

    private static final Map<String, AgentRunContext> REGISTRY = new ConcurrentHashMap<>();

    private DaprAgentRunRegistry() {
    }

    public static void register(String agentRunId, AgentRunContext context) {
        REGISTRY.put(agentRunId, context);
    }

    public static AgentRunContext get(String agentRunId) {
        return REGISTRY.get(agentRunId);
    }

    public static void unregister(String agentRunId) {
        REGISTRY.remove(agentRunId);
    }

    public static Set<String> getRegisteredIds() {
        return REGISTRY.keySet();
    }
}