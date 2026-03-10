package io.quarkiverse.dapr.langchain4j.agent.activities;

/**
 * Input record for {@link ToolCallActivity}.
 *
 * @param agentRunId  the agent run ID used to look up the {@link io.quarkiverse.dapr.langchain4j.agent.AgentRunContext}
 * @param toolCallId  the unique tool call ID used to look up the pending {@link io.quarkiverse.dapr.langchain4j.agent.AgentRunContext.PendingCall}
 * @param toolName    name of the {@code @Tool}-annotated method being executed; stored in the
 *                    Dapr activity input for observability in the workflow history
 * @param args        string representation of the arguments passed to the tool method
 */
public record ToolCallInput(String agentRunId, String toolCallId, String toolName, String args) {
}