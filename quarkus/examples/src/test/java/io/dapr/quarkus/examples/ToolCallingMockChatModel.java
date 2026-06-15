package io.dapr.quarkus.examples;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

/**
 * Stateful mock that drives one tool round-trip, for the durable tool-path test.
 * <p>
 * First turn (tools advertised, no tool result yet): requests {@code getCapital}.
 * Second turn (a tool result is present): echoes that result as the final answer — so the
 * assertion can confirm the tool actually executed via the {@code agent-tool} activity and
 * its output flowed back into the loop.
 * <p>
 * Not globally enabled (no {@code @Priority}); activated only by {@code ToolCallingChatModelProfile}.
 */
@Alternative
@ApplicationScoped
public class ToolCallingMockChatModel implements ChatModel {

    @Override
    public ChatResponse doChat(ChatRequest request) {
        boolean hasTools = request.toolSpecifications() != null && !request.toolSpecifications().isEmpty();
        boolean toolAlreadyRan = request.messages().stream()
                .anyMatch(m -> m instanceof ToolExecutionResultMessage);

        if (hasTools && !toolAlreadyRan) {
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from(ToolExecutionRequest.builder()
                            .id("call-capital")
                            .name("getCapital")
                            .arguments("{\"country\":\"France\"}")
                            .build()))
                    .tokenUsage(new TokenUsage(5, 5))
                    .finishReason(FinishReason.TOOL_EXECUTION)
                    .build();
        }

        String toolText = request.messages().stream()
                .filter(m -> m instanceof ToolExecutionResultMessage)
                .map(m -> ((ToolExecutionResultMessage) m).text())
                .reduce((first, second) -> second)
                .orElse("no tool result");
        return ChatResponse.builder()
                .aiMessage(AiMessage.from("Summary: " + toolText))
                .tokenUsage(new TokenUsage(5, 10))
                .finishReason(FinishReason.STOP)
                .build();
    }
}
