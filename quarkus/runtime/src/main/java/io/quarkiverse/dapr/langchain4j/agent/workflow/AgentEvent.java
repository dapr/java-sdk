package io.quarkiverse.dapr.langchain4j.agent.workflow;

/**
 * External event sent to {@link AgentRunWorkflow} via {@code DaprWorkflowClient.raiseEvent()}.
 * <p>
 * Two event types are used:
 * <ul>
 *   <li>{@code "tool-call"} — a {@code @Tool}-annotated method was intercepted; the workflow
 *       should schedule a {@link io.quarkiverse.dapr.langchain4j.agent.activities.ToolCallActivity}.</li>
 *   <li>{@code "done"} — the agent has finished executing; the workflow should terminate.</li>
 * </ul>
 *
 * @param type       event discriminator: {@code "tool-call"} or {@code "done"}
 * @param toolCallId unique ID for this tool call (null for "done" events)
 * @param toolName   name of the tool method being called (null for "done" events)
 * @param args       serialized arguments (reserved for future use; null for now)
 */
public record AgentEvent(
        String type,
        String toolCallId,
        String toolName,
        String args) {
}