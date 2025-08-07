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
import io.dapr.client.domain.ConversationInputAlpha2;
import io.dapr.client.domain.ConversationMessageContent;
import io.dapr.client.domain.ConversationRequestAlpha2;
import io.dapr.client.domain.ConversationResponseAlpha2;
import io.dapr.client.domain.ConversationResultAlpha2;
import io.dapr.client.domain.ConversationResultChoices;
import io.dapr.client.domain.ConversationTools;
import io.dapr.client.domain.ConversationToolsFunction;
import io.dapr.client.domain.SystemMessage;
import io.dapr.client.domain.UserMessage;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToolsCallDemo {
  /**
   * The main method to demonstrate conversation AI with tools/function calling.
   *
   * @param args Input arguments (unused).
   */
  public static void main(String[] args) {
    try (DaprPreviewClient client = new DaprClientBuilder().buildPreviewClient()) {
      System.out.println("Demonstrating Conversation AI with Tools/Function Calling");

      // Create system message to set context
      SystemMessage systemMessage = new SystemMessage(List.of(
          new ConversationMessageContent("You are a helpful weather assistant. Use the provided tools to get weather information.")
      ));

      // Create user message asking for weather
      UserMessage userMessage = new UserMessage(List.of(
          new ConversationMessageContent("What's the weather like in San Francisco?")
      ));

      // Create conversation input with messages
      ConversationInputAlpha2 conversationInput = new ConversationInputAlpha2(List.of(systemMessage, userMessage));

      // Define function parameters for the weather tool
      Map<String, Object> functionParams = new HashMap<>();
      functionParams.put("location", "string");
      functionParams.put("unit", "string");

      // Create the weather function definition
      ConversationToolsFunction weatherFunction = new ConversationToolsFunction("get_current_weather", functionParams);
      weatherFunction.setDescription("Get the current weather for a specified location");

      // Create the tool wrapper
      ConversationTools weatherTool = new ConversationTools(weatherFunction);

      // Create the conversation request with tools
      ConversationRequestAlpha2 request = new ConversationRequestAlpha2("echo", List.of(conversationInput))
              .setContextId("weather-demo-context")
              .setTemperature(0.7d)
              .setTools(List.of(weatherTool));

      // Send the request
      System.out.println("Sending request to AI with weather tool available...");
      Mono<ConversationResponseAlpha2> responseMono = client.converseAlpha2(request);
      ConversationResponseAlpha2 response = responseMono.block();

      // Process and display the response
      if (response != null && response.getOutputs() != null && !response.getOutputs().isEmpty()) {
        ConversationResultAlpha2 result = response.getOutputs().get(0);
        if (result.getChoices() != null && !result.getChoices().isEmpty()) {
          ConversationResultChoices choice = result.getChoices().get(0);

          // Check if the AI wants to call a tool
          if (choice.getMessage() != null && choice.getMessage().getToolCalls() != null) {
            System.out.println("AI requested to call tools:");
            choice.getMessage().getToolCalls().forEach(toolCall -> {
              System.out.printf("Tool: %s, Arguments: %s%n",
                  toolCall.getFunction().getName(),
                  toolCall.getFunction().getArguments());
            });
          }

          // Display the message content if available
          if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
            System.out.printf("AI Response: %s%n", choice.getMessage().getContent());
          }
        }
      }

      System.out.println("Tools call demonstration completed.");

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
