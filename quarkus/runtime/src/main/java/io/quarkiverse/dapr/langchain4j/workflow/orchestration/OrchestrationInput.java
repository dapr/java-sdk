package io.quarkiverse.dapr.langchain4j.workflow.orchestration;

/**
 * Input data passed to all Dapr orchestration workflows.
 *
 * @param plannerId         unique planner ID (used to look up the planner in the registry)
 * @param agentCount        number of sub-agents to execute
 * @param maxIterations     maximum loop iterations (only used by LoopOrchestrationWorkflow)
 * @param testExitAtLoopEnd whether to test exit condition at loop end vs. loop start
 */
public record OrchestrationInput(String plannerId, int agentCount, int maxIterations, boolean testExitAtLoopEnd) {
}
