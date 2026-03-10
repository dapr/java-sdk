package io.quarkiverse.dapr.langchain4j.agent.workflow;

import java.util.List;

import io.quarkiverse.dapr.langchain4j.agent.activities.LlmCallOutput;
import io.quarkiverse.dapr.langchain4j.agent.activities.ToolCallOutput;

/**
 * Aggregated output of a completed {@link AgentRunWorkflow}. Set as the Dapr
 * workflow custom status after every activity so observers can follow execution
 * progress in real time, and reflects the final state once {@code "done"} is received.
 *
 * @param agentName  human-readable name of the {@code @Agent} that was executed
 * @param toolCalls  ordered list of tool calls made by the agent, each with its
 *                   input arguments and return value
 * @param llmCalls   ordered list of LLM calls made by the agent, each with the
 *                   model method name and the response text
 */
public record AgentRunOutput(
        String agentName,
        List<ToolCallOutput> toolCalls,
        List<LlmCallOutput> llmCalls) {
}