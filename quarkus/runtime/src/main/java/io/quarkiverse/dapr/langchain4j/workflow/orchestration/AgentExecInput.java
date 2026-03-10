package io.quarkiverse.dapr.langchain4j.workflow.orchestration;

/**
 * Input for the AgentExecutionActivity.
 *
 * @param plannerId  the planner ID to look up in the registry
 * @param agentIndex the index of the agent in the planner's agent list
 * @param agentRunId the unique ID for this agent execution, must match the child
 *                   AgentRunWorkflow instance ID so raiseEvent() reaches the right workflow
 */
public record AgentExecInput(String plannerId, int agentIndex, String agentRunId) {
}
