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
import io.dapr.client.domain.ConversationInput;
import io.dapr.client.domain.ConversationRequest;
import io.dapr.client.domain.ConversationResponse;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;

public class DemoConversationAI {
  /**
   * The main method to start the client.
   *
   * @param args Input arguments (unused).
   */
  public static void main(String[] args) {
    try (DaprPreviewClient client = new DaprClientBuilder().buildPreviewClient()) {
      ConversationInput daprConversationInput = new ConversationInput("Hello How are you? "
          + "This is the my number 672-123-4567");

      // Component name is the name provided in the metadata block of the conversation.yaml file.
      Mono<ConversationResponse> responseMono = client.converse(new ConversationRequest("echo",
          new ArrayList<>(Collections.singleton(daprConversationInput)))
            .setContextId("contextId")
            .setScrubPii(true).setTemperature(1.1d));
      ConversationResponse response = responseMono.block();
      System.out.printf("Conversation output: %s", response.getConversationOutpus().get(0).getResult());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}