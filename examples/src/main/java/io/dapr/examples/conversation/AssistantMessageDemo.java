/*
 * Copyright 2021 The Dapr Authors
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

package io.dapr.examples.conversation;

import io.dapr.client.DaprClientBuilder;
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
import io.dapr.client.domain.SystemMessage;
import io.dapr.client.domain.ToolMessage;
import io.dapr.client.domain.UserMessage;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class AssistantMessageDemo {
  /**
   * The main method to demonstrate conversation AI with assistant messages and conversation history.
   *
   * @param args Input arguments (unused).
   */
  public static void main(String[] args) {
    try (DaprPreviewClient client = new DaprClientBuilder().buildPreviewClient()) {
      System.out.println("Demonstrating Conversation AI with Assistant Messages and Conversation History");

      // Create a conversation history with multiple message types
      List<ConversationMessage> conversationHistory = new ArrayList<>();

      // 1. System message to set context
      SystemMessage systemMessage = new SystemMessage(List.of(
          new ConversationMessageContent("You are a helpful assistant that can help with weather queries.")
      ));
      systemMessage.setName("WeatherBot");
      conversationHistory.add(systemMessage);

      // 2. Initial user greeting
      UserMessage greeting = new UserMessage(List.of(
          new ConversationMessageContent("Hello! I need help with weather information.")
      ));
      greeting.setName("User123");
      conversationHistory.add(greeting);

      // 3. Assistant response with tool call
      AssistantMessage assistantResponse = new AssistantMessage(
          List.of(new ConversationMessageContent("I'll help you with weather information. Let me check the weather for you.")),
          List.of(new ConversationToolCalls(
              new ConversationToolCallsOfFunction("get_weather", "{\"location\": \"San Francisco\", \"unit\": \"fahrenheit\"}")
          ))
      );
      assistantResponse.setName("WeatherBot");
      conversationHistory.add(assistantResponse);

      // 4. Tool response (simulating weather API response)
      ToolMessage toolResponse = new ToolMessage(List.of(
          new ConversationMessageContent("{\"temperature\": \"72F\", \"condition\": \"sunny\", \"humidity\": \"65%\"}")
      ));
      toolResponse.setName("weather_api");
      conversationHistory.add(toolResponse);

      // 5. Current user question
      UserMessage currentQuestion = new UserMessage(List.of(
          new ConversationMessageContent("Based on that weather data, should I wear a jacket today?")
      ));
      currentQuestion.setName("User123");
      conversationHistory.add(currentQuestion);

      // Create conversation input with the full history
      ConversationInputAlpha2 conversationInput = new ConversationInputAlpha2(conversationHistory);
      conversationInput.setScrubPii(false);

      // Create the conversation request
      ConversationRequestAlpha2 request = new ConversationRequestAlpha2("echo", List.of(conversationInput))
              .setContextId("assistant-demo-context")
              .setTemperature(0.8d);

      // Send the request
      System.out.println("Sending conversation with assistant messages and history...");
      System.out.println("Conversation includes:");
      System.out.println("- System message (context setting)");
      System.out.println("- User greeting");
      System.out.println("- Assistant response with tool call");
      System.out.println("- Tool response with weather data");
      System.out.println("- User follow-up question");

      Mono<ConversationResponseAlpha2> responseMono = client.converseAlpha2(request);
      ConversationResponseAlpha2 response = responseMono.block();

      // Process and display the response
      if (response != null && response.getOutputs() != null && !response.getOutputs().isEmpty()) {
        ConversationResultAlpha2 result = response.getOutputs().get(0);
        if (result.getChoices() != null && !result.getChoices().isEmpty()) {
          ConversationResultChoices choice = result.getChoices().get(0);

          if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
            System.out.printf("Assistant Response: %s%n", choice.getMessage().getContent());
          }

          // Check for additional tool calls in the response
          if (choice.getMessage() != null && choice.getMessage().getToolCalls() != null) {
            System.out.println("Assistant requested additional tool calls:");
            choice.getMessage().getToolCalls().forEach(toolCall -> {
              System.out.printf("Tool: %s, Arguments: %s%n",
                  toolCall.getFunction().getName(),
                  toolCall.getFunction().getArguments());
            });
          }
        }
      }

      System.out.println("Assistant message demonstration completed.");

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
