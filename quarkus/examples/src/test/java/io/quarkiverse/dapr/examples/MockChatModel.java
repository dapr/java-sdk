package io.quarkiverse.dapr.examples;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

/**
 * Mock ChatModel that returns predictable responses for integration testing.
 * Takes priority over the OpenAI ChatModel bean via {@code @Alternative @Priority(1)}.
 */
@Alternative
@Priority(1)
@ApplicationScoped
public class MockChatModel implements ChatModel {

    @Override
    public ChatResponse doChat(ChatRequest request) {
        return ChatResponse.builder()
                .aiMessage(AiMessage.from("Once upon a time, a brave dragon befriended a wizard. "
                        + "Together they embarked on an epic adventure across enchanted lands. "
                        + "Their story became legend, told for generations."))
                .tokenUsage(new TokenUsage(10, 30))
                .finishReason(FinishReason.STOP)
                .build();
    }
}
