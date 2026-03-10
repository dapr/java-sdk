package io.quarkiverse.dapr.langchain4j.workflow;

/**
 * Shared utility methods for Dapr agent services.
 */
public final class DaprAgentServiceUtil {

    private DaprAgentServiceUtil() {
    }

    /**
     * Sanitizes a name for use as a Dapr workflow identifier.
     * Replaces any non-alphanumeric characters (except hyphens and underscores)
     * with underscores.
     */
    public static String safeName(String name) {
        if (name == null || name.isEmpty()) {
            return "unnamed";
        }
        return name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
