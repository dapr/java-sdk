package io.quarkiverse.dapr.langchain4j.workflow;

/**
 * Marker interface for Dapr-backed agent service implementations.
 * Provides the Dapr Workflow type name used to schedule the orchestration.
 */
public interface DaprAgentService {

    /**
     * Returns the simple class name of the Dapr Workflow to schedule
     * for this orchestration pattern.
     */
    String workflowType();
}
