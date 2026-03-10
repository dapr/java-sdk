package io.quarkiverse.dapr.langchain4j.agent.activities;

/**
 * Output record returned by {@link LlmCallActivity} after a {@code ChatModel.chat()}
 * call has been executed. Stored in the Dapr workflow history so the full LLM turn
 * (prompt in, response out) is visible without inspecting in-process state.
 *
 * @param methodName  name of the {@code ChatModel} method that was invoked (e.g., {@code "chat"})
 * @param prompt      serialized {@code ChatRequest} messages that were sent to the model;
 *                    extracted from the {@code ChatRequest} argument by
 *                    {@link io.quarkiverse.dapr.langchain4j.agent.DaprLlmCallInterceptor}
 * @param response    AI response text extracted from {@code ChatResponse.aiMessage().text()};
 *                    this is the exact text the model returned to the agent
 */
public record LlmCallOutput(String methodName, String prompt, String response) {
}