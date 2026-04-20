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
import io.dapr.client.domain.UserMessage;
import reactor.core.publisher.Mono;

import java.util.List;

public class UserMessageDemo {
  /**
   * The main method to start the client.
   *
   * @param args Input arguments (unused).
   */
  public static void main(String[] args) {
    try (DaprPreviewClient client = new DaprClientBuilder().buildPreviewClient()) {
      System.out.println("Sending the following input to LLM: Hello How are you? This is the my number 672-123-4567");

      // Create user message with content
      UserMessage userMessage = new UserMessage(List.of(new ConversationMessageContent("Hello How are you? "
              + "This is the my number 672-123-4567")));

      // Create conversation input with the user message
      ConversationInputAlpha2 daprConversationInput = new ConversationInputAlpha2(List.of(userMessage));

      // Define the JSON schema for the response format
      String responseSchema = """
          {
            "type": "object",
            "properties": {
              "greeting": {
                "type": "string",
                "description": "A friendly greeting response"
              },
              "phone_number_detected": {
                "type": "boolean",
                "description": "Whether a phone number was detected in the input"
              },
              "detected_number": {
                "type": "string",
                "description": "The phone number that was detected, if any"
              }
            },
            "required": ["greeting", "phone_number_detected"],
            "additionalProperties": false
          }
          """;

      // Component name is the name provided in the metadata block of the conversation.yaml file.
      Mono<ConversationResponseAlpha2> responseMono = client.converseAlpha2(new ConversationRequestAlpha2("echo",
          List.of(daprConversationInput))
          .setContextId("contextId")
          .setScrubPii(true)
          .setTemperature(1.1d).setResponseFormat(responseSchema));

      ConversationResponseAlpha2 response = responseMono.block();

      // Extract and print the conversation result
      if (response != null && response.getOutputs() != null && !response.getOutputs().isEmpty()) {
        ConversationResultAlpha2 result = response.getOutputs().get(0);
        UsageUtils.printUsage(result);

        if (result.getChoices() != null && !result.getChoices().isEmpty()) {
          ConversationResultChoices choice = result.getChoices().get(0);
          if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
            System.out.printf("Conversation output: %s", choice.getMessage().getContent());
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
