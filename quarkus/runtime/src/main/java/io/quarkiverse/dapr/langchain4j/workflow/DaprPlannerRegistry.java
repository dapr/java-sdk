package io.quarkiverse.dapr.langchain4j.workflow;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Static registry mapping planner IDs to {@link DaprWorkflowPlanner} instances.
 * Allows Dapr WorkflowActivities (which are instantiated by the Dapr SDK) to
 * look up the in-process planner.
 */
public class DaprPlannerRegistry {

    private static final ConcurrentHashMap<String, DaprWorkflowPlanner> registry = new ConcurrentHashMap<>();

    public static void register(String id, DaprWorkflowPlanner planner) {
        registry.put(id, planner);
    }

    public static DaprWorkflowPlanner get(String id) {
        return registry.get(id);
    }

    public static void unregister(String id) {
        registry.remove(id);
    }

    public static String getRegisteredIds() {
        return registry.keySet().toString();
    }
}
