/*
 * Copyright 2025 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.it.testcontainers.conversations;

import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.AssistantMessage;
import io.dapr.client.domain.ConversationInputAlpha2;
import io.dapr.client.domain.ConversationMessage;
import io.dapr.client.domain.ConversationMessageContent;
import io.dapr.client.domain.ConversationRequestAlpha2;
import io.dapr.client.domain.ConversationResponseAlpha2;
import io.dapr.client.domain.ConversationResultAlpha2;
import io.dapr.client.domain.ConversationResultChoices;
import io.dapr.client.domain.ConversationToolCalls;
import io.dapr.client.domain.ConversationToolCallsOfFunction;
import io.dapr.client.domain.ConversationTools;
import io.dapr.client.domain.ConversationToolsFunction;
import io.dapr.client.domain.DeveloperMessage;
import io.dapr.client.domain.SystemMessage;
import io.dapr.client.domain.ToolMessage;
import io.dapr.client.domain.UserMessage;
import io.dapr.it.testcontainers.DaprPreviewClientConfiguration;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        classes = {
                DaprPreviewClientConfiguration.class,
                TestConversationApplication.class
        }
)
@Testcontainers
@Tag("testcontainers")
public class DaprConversationAlpha2IT {

    private static final Network DAPR_NETWORK = io.dapr.it.testcontainers.TestContainerNetworks.SHARED_NETWORK;
    private static final Random RANDOM = new Random();
    private static final int PORT = RANDOM.nextInt(1000) + 8000;

    @Container
    private static final DaprContainer DAPR_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
            .withAppName("conversation-alpha2-dapr-app")
            .withComponent(new Component("echo", "conversation.echo", "v1", new HashMap<>()))
            .withNetwork(DAPR_NETWORK)
            .withDaprLogLevel(DaprLogLevel.DEBUG)
            .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
            .withAppChannelAddress("host.testcontainers.internal")
            .withAppPort(PORT);

    /**
     * Expose the Dapr port to the host.
     *
     * @param registry the dynamic property registry
     */
    @DynamicPropertySource
    static void daprProperties(DynamicPropertyRegistry registry) {
        registry.add("dapr.http.endpoint", DAPR_CONTAINER::getHttpEndpoint);
        registry.add("dapr.grpc.endpoint", DAPR_CONTAINER::getGrpcEndpoint);
        registry.add("server.port", () -> PORT);
    }

    @Autowired
    private DaprPreviewClient daprPreviewClient;

    @BeforeEach
    public void setUp() {
        org.testcontainers.Testcontainers.exposeHostPorts(PORT);
    }

    @Test
    public void testConverseAlpha2WithUserMessage() {
        // Create a user message
        UserMessage userMessage = new UserMessage(List.of(new ConversationMessageContent("Hello, how are you?")));
        userMessage.setName("TestUser");

        // Create input with the message
        ConversationInputAlpha2 input = new ConversationInputAlpha2(List.of(userMessage));

        ConversationRequestAlpha2 request = new ConversationRequestAlpha2("echo", List.of(input));

        ConversationResponseAlpha2 response = daprPreviewClient.converseAlpha2(request).block();

        assertNotNull(response);
        assertNotNull(response.getOutputs());
        assertEquals(1, response.getOutputs().size());

        ConversationResultAlpha2 result = response.getOutputs().get(0);
        assertNotNull(result.getChoices());
        assertTrue(result.getChoices().size() > 0);

        ConversationResultChoices choice = result.getChoices().get(0);
        assertNotNull(choice.getMessage());
        assertNotNull(choice.getMessage().getContent());
    }

    @Test
    public void testConverseAlpha2WithAllMessageTypes() {
        List<ConversationMessage> messages = new ArrayList<>();

        // System message
        SystemMessage systemMsg = new SystemMessage(List.of(new ConversationMessageContent("You are a helpful assistant.")));
        systemMsg.setName("system");
        messages.add(systemMsg);

        // User message
        UserMessage userMsg = new UserMessage(List.of(new ConversationMessageContent("Hello!")));
        userMsg.setName("user");
        messages.add(userMsg);

        // Assistant message
        AssistantMessage assistantMsg = new AssistantMessage(List.of(new ConversationMessageContent("Hi there!")),
            List.of(new ConversationToolCalls(
                new ConversationToolCallsOfFunction("get_weather", "{\"location\": \"New York\"}"))));
        assistantMsg.setName("assistant");
        messages.add(assistantMsg);

        // Tool message
        ToolMessage toolMsg = new ToolMessage(List.of(new ConversationMessageContent("Weather data: 72F")));
        toolMsg.setName("tool");
        messages.add(toolMsg);

        // Developer message
        DeveloperMessage devMsg = new DeveloperMessage(List.of(new ConversationMessageContent("Debug info")));
        devMsg.setName("developer");
        messages.add(devMsg);

        ConversationInputAlpha2 input = new ConversationInputAlpha2(messages);
        ConversationRequestAlpha2 request = new ConversationRequestAlpha2("echo", List.of(input));

        ConversationResponseAlpha2 response = daprPreviewClient.converseAlpha2(request).block();

        assertNotNull(response);
        assertNotNull(response.getOutputs());
        assertTrue(response.getOutputs().size() > 0);
    }

    @Test
    public void testConverseAlpha2WithScrubPII() {
        // Create a user message with PII
        UserMessage userMessage = new UserMessage(List.of(new ConversationMessageContent("My email is test@example.com and phone is +1234567890")));

        ConversationInputAlpha2 input = new ConversationInputAlpha2(List.of(userMessage));
        input.setScrubPii(true);

        ConversationRequestAlpha2 request = new ConversationRequestAlpha2("echo", List.of(input));
        request.setScrubPii(true);

        ConversationResponseAlpha2 response = daprPreviewClient.converseAlpha2(request).block();

        assertNotNull(response);
        assertNotNull(response.getOutputs());
        assertTrue(response.getOutputs().size() > 0);

        // Verify response structure (actual PII scrubbing depends on echo component implementation)
        ConversationResultChoices choice = response.getOutputs().get(0).getChoices().get(0);
        assertNotNull(choice.getMessage());
        assertNotNull(choice.getMessage().getContent());
    }

    @Test
    public void testConverseAlpha2WithTools() {
        // Create a tool function
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("location", "string");
        parameters.put("unit", "celsius");
        ConversationToolsFunction function = new ConversationToolsFunction("get_weather", parameters);
        function.setDescription("Get current weather information");

        ConversationTools tool = new ConversationTools(function);

        // Create user message
        UserMessage userMessage = new UserMessage(List.of(new ConversationMessageContent("What's the weather like?")));

        ConversationInputAlpha2 input = new ConversationInputAlpha2(List.of(userMessage));

        ConversationRequestAlpha2 request = new ConversationRequestAlpha2("echo", List.of(input));
        request.setTools(List.of(tool));
        request.setToolChoice("auto");

        ConversationResponseAlpha2 response = daprPreviewClient.converseAlpha2(request).block();

        assertNotNull(response);
        assertNotNull(response.getOutputs());
        assertTrue(response.getOutputs().size() > 0);
    }

    @Test
    public void testConverseAlpha2WithMetadataAndParameters() {
        UserMessage userMessage = new UserMessage(List.of(new ConversationMessageContent("Hello world")));

        ConversationInputAlpha2 input = new ConversationInputAlpha2(List.of(userMessage));

        // Set metadata and parameters
        Map<String, String> metadata = new HashMap<>();
        metadata.put("request-id", "test-123");
        metadata.put("source", "integration-test");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("max_tokens", "1000");
        parameters.put("temperature", "0.7");

        ConversationRequestAlpha2 request = new ConversationRequestAlpha2("echo", List.of(input));
        request.setContextId("test-context-123");
        request.setTemperature(0.8);
        request.setMetadata(metadata);
        request.setParameters(parameters);

        ConversationResponseAlpha2 response = daprPreviewClient.converseAlpha2(request).block();

        assertNotNull(response);
        assertNotNull(response.getOutputs());
        assertTrue(response.getOutputs().size() > 0);

        // Verify context ID is handled properly
        // Note: actual context ID behavior depends on echo component implementation
        assertNotNull(response.getContextId());
    }

    @Test
    public void testConverseAlpha2WithAssistantToolCalls() {
        // Create a tool call
        ConversationToolCallsOfFunction toolFunction =
            new ConversationToolCallsOfFunction("get_weather", "{\"location\": \"New York\"}");
        ConversationToolCalls toolCall = new ConversationToolCalls(toolFunction);
        toolCall.setId("call_123");

        // Create assistant message with tool calls
        AssistantMessage assistantMsg = new AssistantMessage(List.of(new ConversationMessageContent("Hi there!")),
            List.of(new ConversationToolCalls(
                new ConversationToolCallsOfFunction("get_weather", "{\"location\": \"New York\"}"))));        // Note: Current implementation doesn't support setting tool calls in constructor
        // This tests the structure and ensures no errors occur

        ConversationInputAlpha2 input = new ConversationInputAlpha2(List.of(assistantMsg));

        ConversationRequestAlpha2 request = new ConversationRequestAlpha2("echo", List.of(input));

        ConversationResponseAlpha2 response = daprPreviewClient.converseAlpha2(request).block();

        assertNotNull(response);
        assertNotNull(response.getOutputs());
        assertTrue(response.getOutputs().size() > 0);
    }

    @Test
    public void testConverseAlpha2WithComplexScenario() {
        List<ConversationMessage> messages = new ArrayList<>();

        // System message setting context
        SystemMessage systemMsg = new SystemMessage(List.of(new ConversationMessageContent("You are a helpful weather assistant.")));
        systemMsg.setName("WeatherBot");
        messages.add(systemMsg);

        // User asking for weather
        UserMessage userMsg = new UserMessage(List.of(new ConversationMessageContent("What's the weather in San Francisco?")));
        userMsg.setName("User123");
        messages.add(userMsg);

        // Assistant response
        AssistantMessage assistantMsg = new AssistantMessage(List.of(new ConversationMessageContent("Hi there!")),
            List.of(new ConversationToolCalls(
                new ConversationToolCallsOfFunction("get_weather", "{\"location\": \"New York\"}"))));
        assistantMsg.setName("WeatherBot");
        messages.add(assistantMsg);

        // Tool response
        ToolMessage toolMsg = new ToolMessage(List.of(new ConversationMessageContent("{\"temperature\": \"68F\", \"condition\": \"sunny\"}")));
        toolMsg.setName("weather_api");
        messages.add(toolMsg);

        ConversationInputAlpha2 input = new ConversationInputAlpha2(messages);
        input.setScrubPii(false);

        // Create tools
        Map<String, Object> functionParams = new HashMap<>();
        functionParams.put("location", "string");
        functionParams.put("unit", "fahrenheit");
        ConversationToolsFunction weatherFunction = new ConversationToolsFunction("get_current_weather",
            functionParams);
        weatherFunction.setDescription("Get current weather for a location");


        ConversationTools weatherTool = new ConversationTools(weatherFunction);

        // Set up complete request
        Map<String, String> metadata = new HashMap<>();
        metadata.put("conversation-type", "weather-query");
        metadata.put("user-session", "session-456");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("max_tokens", "2000");
        parameters.put("response_format", "json");

        ConversationRequestAlpha2 request = new ConversationRequestAlpha2("echo", List.of(input));
        request.setContextId("weather-conversation-789");
        request.setTemperature(0.7);
        request.setScrubPii(false);
        request.setTools(List.of(weatherTool));
        request.setToolChoice("auto");
        request.setMetadata(metadata);
        request.setParameters(parameters);

        ConversationResponseAlpha2 response = daprPreviewClient.converseAlpha2(request).block();

        assertNotNull(response);
        assertNotNull(response.getOutputs());
        assertTrue(response.getOutputs().size() > 0);

        ConversationResultAlpha2 result = response.getOutputs().get(0);
        assertNotNull(result.getChoices());
        assertTrue(result.getChoices().size() > 0);

        ConversationResultChoices choice = result.getChoices().get(0);
        assertNotNull(choice.getFinishReason());
        assertTrue(choice.getIndex() >= 0);

        if (choice.getMessage() != null) {
            assertNotNull(choice.getMessage().getContent());
        }
    }

    @Test
    public void testConverseAlpha2MultipleInputs() {
        // Create multiple conversation inputs
        List<ConversationInputAlpha2> inputs = new ArrayList<>();

        // First input - greeting
        UserMessage greeting = new UserMessage(List.of(new ConversationMessageContent("Hello!")));
        ConversationInputAlpha2 input1 = new ConversationInputAlpha2(List.of(greeting));
        inputs.add(input1);

        // Second input - question
        UserMessage question = new UserMessage(List.of(new ConversationMessageContent("How are you?")));
        ConversationInputAlpha2 input2 = new ConversationInputAlpha2(List.of(question));
        input2.setScrubPii(true);
        inputs.add(input2);

        ConversationRequestAlpha2 request = new ConversationRequestAlpha2("echo", inputs);

        ConversationResponseAlpha2 response = daprPreviewClient.converseAlpha2(request).block();

        assertNotNull(response);
        assertNotNull(response.getOutputs());
        assertTrue(response.getOutputs().size() > 0);

        // Should handle multiple inputs appropriately
        for (ConversationResultAlpha2 result : response.getOutputs()) {
            assertNotNull(result.getChoices());
            assertTrue(result.getChoices().size() > 0);
        }
    }
}
