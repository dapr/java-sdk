package io.dapr.quarkus.langchain4j.chatmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import io.dapr.client.domain.AssistantMessage;
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.ConversationMessage;
import io.dapr.client.domain.ConversationRequestAlpha2;
import io.dapr.client.domain.ConversationToolCalls;
import io.dapr.client.domain.ConversationToolsFunction;
import reactor.core.publisher.Mono;

class DaprConversationChatModelTest {

    private DaprPreviewClient client;
    private DaprConversationChatModel model;

    @BeforeEach
    void setUp() {
        client = mock(DaprPreviewClient.class);
        when(client.converseAlpha2(any())).thenReturn(Mono.empty());
        model = new DaprConversationChatModel(client, "llm", 0.7);
    }

    @Test
    void toolParametersShouldCarryJsonSchema() {
        ToolSpecification spec = ToolSpecification.builder()
            .name("getWeather")
            .description("Get the weather for a city")
            .parameters(JsonObjectSchema.builder()
                .addStringProperty("city", "The city name")
                .required("city")
                .build())
            .build();

        model.chat(ChatRequest.builder()
            .messages(UserMessage.from("What is the weather in Madrid?"))
            .toolSpecifications(spec)
            .build());

        ConversationToolsFunction fn = capturedRequest().getTools().get(0).getFunction();
        assertThat(fn.getName()).isEqualTo("getWeather");
        assertThat(fn.getDescription()).isEqualTo("Get the weather for a city");

        Map<String, Object> parameters = fn.getParameters();
        assertThat(parameters.get("type")).isEqualTo("object");

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) parameters.get("properties");
        assertThat(properties).containsKey("city");

        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) parameters.get("required");
        assertThat(required).containsExactly("city");
    }

    @Test
    void parameterlessToolShouldFallBackToEmptyObjectSchema() {
        ToolSpecification spec = ToolSpecification.builder()
            .name("ping")
            .build();

        model.chat(ChatRequest.builder()
            .messages(UserMessage.from("ping"))
            .toolSpecifications(spec)
            .build());

        Map<String, Object> parameters =
            capturedRequest().getTools().get(0).getFunction().getParameters();
        assertThat(parameters).containsExactly(entry("type", "object"));
    }

    @Test
    void toolLessAiMessageShouldMapWithoutThrowingAndCarryNoToolCalls() {
        // Regression: a plain-text AiMessage (no tool requests) — e.g. a prior @LoopAgent
        // iteration's final answer carried into the next iteration's history — used to NPE
        // in AssistantMessage's List.copyOf(null).
        model.chat(ChatRequest.builder()
            .messages(
                UserMessage.from("Plan my trip"),
                AiMessage.from("Here is your itinerary."))
            .build());

        AssistantMessage assistant = (AssistantMessage) messageOfRole(
            capturedRequest(), io.dapr.client.domain.ConversationMessageRole.ASSISTANT);
        assertThat(assistant.getContent().get(0).getText()).isEqualTo("Here is your itinerary.");
        // Non-null (so the SDK constructor does not NPE) and empty (so no tool_calls are
        // serialized — an empty repeated proto field is wire-identical to unset).
        assertThat(assistant.getToolCalls()).isNotNull().isEmpty();
    }

    @Test
    void aiMessageWithToolRequestsShouldMapToolCalls() {
        AiMessage withTool = AiMessage.from(ToolExecutionRequest.builder()
            .id("call-1")
            .name("getWeather")
            .arguments("{\"city\":\"Madrid\"}")
            .build());

        model.chat(ChatRequest.builder()
            .messages(
                UserMessage.from("Weather in Madrid?"),
                withTool)
            .build());

        AssistantMessage assistant = (AssistantMessage) messageOfRole(
            capturedRequest(), io.dapr.client.domain.ConversationMessageRole.ASSISTANT);
        assertThat(assistant.getToolCalls()).hasSize(1);
        ConversationToolCalls tc = assistant.getToolCalls().get(0);
        assertThat(tc.getId()).isEqualTo("call-1");
        assertThat(tc.getFunction().getName()).isEqualTo("getWeather");
        assertThat(tc.getFunction().getArguments()).isEqualTo("{\"city\":\"Madrid\"}");
    }

    private static ConversationMessage messageOfRole(
            ConversationRequestAlpha2 request, io.dapr.client.domain.ConversationMessageRole role) {
        return request.getInputs().get(0).getMessages().stream()
            .filter(m -> m.getRole() == role)
            .findFirst()
            .orElseThrow();
    }

    private ConversationRequestAlpha2 capturedRequest() {
        ArgumentCaptor<ConversationRequestAlpha2> captor =
            ArgumentCaptor.forClass(ConversationRequestAlpha2.class);
        verify(client).converseAlpha2(captor.capture());
        return captor.getValue();
    }
}
