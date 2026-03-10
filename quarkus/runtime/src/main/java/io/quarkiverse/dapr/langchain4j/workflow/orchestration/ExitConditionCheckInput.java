package io.quarkiverse.dapr.langchain4j.workflow.orchestration;

/**
 * Input for the ExitConditionCheckActivity (used by loop workflows).
 *
 * @param plannerId the planner ID to look up in the registry
 * @param iteration the current loop iteration number
 */
public record ExitConditionCheckInput(String plannerId, int iteration) {
}
