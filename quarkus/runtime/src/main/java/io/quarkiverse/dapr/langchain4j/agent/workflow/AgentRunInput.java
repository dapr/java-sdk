package io.quarkiverse.dapr.langchain4j.agent.workflow;

/**
 * Input record for {@link AgentRunWorkflow}.
 *
 * @param agentRunId     unique ID correlating the Dapr Workflow instance to its in-memory
 *                       {@link io.quarkiverse.dapr.langchain4j.agent.AgentRunContext}
 * @param agentName      human-readable name from {@code @Agent(name)} (or class+method for CDI
 *                       beans), used for observability in the Dapr workflow history
 * @param userMessage    the {@code @UserMessage} template text (CDI bean path) or the first
 *                       rendered user message from the {@code ChatRequest} (AiService path);
 *                       may be {@code null} when started by an orchestration activity
 * @param systemMessage  the {@code @SystemMessage} template text (CDI bean path) or the
 *                       rendered system message from the {@code ChatRequest} (AiService path);
 *                       may be {@code null}
 */
public record AgentRunInput(String agentRunId, String agentName, String userMessage, String systemMessage) {
}