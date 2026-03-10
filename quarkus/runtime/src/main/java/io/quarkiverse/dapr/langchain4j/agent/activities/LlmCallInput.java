package io.quarkiverse.dapr.langchain4j.agent.activities;

/**
 * Input record for {@link LlmCallActivity}, identifying the specific LLM call to execute.
 *
 * @param agentRunId  the ID of the {@code AgentRunWorkflow} instance
 * @param llmCallId   the unique ID of the pending LLM call registered in {@link
 *                    io.quarkiverse.dapr.langchain4j.agent.AgentRunContext}
 * @param methodName  name of the {@code ChatModel} method being called (e.g., {@code "chat"});
 *                    stored in the Dapr activity input for observability in the workflow history
 * @param prompt      string representation of the {@code ChatRequest} messages sent to the LLM;
 *                    extracted by {@link io.quarkiverse.dapr.langchain4j.agent.DaprLlmCallInterceptor}
 *                    and stored in the Dapr activity input so the full prompt is visible in the
 *                    workflow history without needing to inspect in-process state
 */
public record LlmCallInput(String agentRunId, String llmCallId, String methodName, String prompt) {
}