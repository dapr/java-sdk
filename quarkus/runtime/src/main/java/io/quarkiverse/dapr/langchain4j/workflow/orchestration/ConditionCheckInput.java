package io.quarkiverse.dapr.langchain4j.workflow.orchestration;

/**
 * Input for the ConditionCheckActivity (used by conditional workflows).
 *
 * @param plannerId  the planner ID to look up in the registry
 * @param agentIndex the index of the agent whose condition should be evaluated
 */
public record ConditionCheckInput(String plannerId, int agentIndex) {
}
