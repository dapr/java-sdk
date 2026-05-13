/*
 * Copyright 2026 The Dapr Authors
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

package io.dapr.quarkus.langchain4j.chatmodel;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.dapr.client.DaprPreviewClient;
import io.dapr.client.domain.ConversationInputAlpha2;
import io.dapr.client.domain.ConversationMessage;
import io.dapr.client.domain.ConversationMessageContent;
import io.dapr.client.domain.ConversationRequestAlpha2;
import io.dapr.client.domain.ConversationResponseAlpha2;
import io.dapr.client.domain.ConversationResultChoices;
import io.dapr.client.domain.ConversationResultMessage;
import io.dapr.client.domain.ConversationToolCalls;
import io.dapr.client.domain.ConversationToolCallsOfFunction;
import io.dapr.client.domain.ConversationTools;
import io.dapr.client.domain.ConversationToolsFunction;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LangChain4j {@link ChatModel} implementation backed by the Dapr Conversation API.
 *
 * <p>This allows swapping LLM providers (OpenAI, Anthropic, etc.) by changing the
 * Dapr component configuration — no Java code changes needed.
 */
public class DaprConversationChatModel implements ChatModel {

  private static final Logger LOG = Logger.getLogger(DaprConversationChatModel.class);

  private final DaprPreviewClient client;
  private final String componentName;
  private final double temperature;

  /**
   * Creates a new DaprConversationChatModel.
   *
   * @param client        the Dapr preview client
   * @param componentName the Dapr conversation component name
   * @param temperature   the temperature for generation
   */
  public DaprConversationChatModel(DaprPreviewClient client,
      String componentName, double temperature) {
    this.client = client;
    this.componentName = componentName;
    this.temperature = temperature;
  }

  @Override
  public ChatResponse chat(ChatRequest chatRequest) {
    List<ConversationMessage> daprMessages = new ArrayList<>();

    // Convert LangChain4j messages to Dapr messages
    for (ChatMessage msg : chatRequest.messages()) {
      daprMessages.add(toDaprMessage(msg));
    }

    ConversationInputAlpha2 input = new ConversationInputAlpha2(daprMessages);

    ConversationRequestAlpha2 request = new ConversationRequestAlpha2(
        componentName, List.of(input));
    request.setTemperature(temperature);

    // Convert tool specifications if present
    if (chatRequest.toolSpecifications() != null && !chatRequest.toolSpecifications().isEmpty()) {
      List<ConversationTools> tools = new ArrayList<>();
      for (ToolSpecification spec : chatRequest.toolSpecifications()) {
        tools.add(toDaprTool(spec));
      }
      request.setTools(tools);
      request.setToolChoice("auto");
    }

    LOG.debugf("Sending conversation request to Dapr component '%s' with %d messages",
        componentName, daprMessages.size());

    ConversationResponseAlpha2 response = client.converseAlpha2(request).block();

    return fromDaprResponse(response);
  }

  private ConversationMessage toDaprMessage(ChatMessage msg) {
    if (msg instanceof SystemMessage sm) {
      return new io.dapr.client.domain.SystemMessage(
          List.of(new ConversationMessageContent(sm.text())));
    }
    if (msg instanceof UserMessage um) {
      return new io.dapr.client.domain.UserMessage(
          List.of(new ConversationMessageContent(um.singleText())));
    }
    if (msg instanceof AiMessage ai) {
      List<ConversationToolCalls> toolCalls = null;
      if (ai.hasToolExecutionRequests()) {
        toolCalls = new ArrayList<>();
        for (var req : ai.toolExecutionRequests()) {
          ConversationToolCalls tc = new ConversationToolCalls(
              new ConversationToolCallsOfFunction(req.name(), req.arguments()));
          tc.setId(req.id());
          toolCalls.add(tc);
        }
      }
      String text = ai.text() != null ? ai.text() : "";
      return new io.dapr.client.domain.AssistantMessage(
          List.of(new ConversationMessageContent(text)), toolCalls);
    }
    if (msg instanceof ToolExecutionResultMessage tr) {
      io.dapr.client.domain.ToolMessage tm = new io.dapr.client.domain.ToolMessage(
          List.of(new ConversationMessageContent(tr.text())));
      tm.setToolId(tr.id());
      return tm;
    }
    throw new IllegalArgumentException("Unsupported message type: " + msg.getClass());
  }

  private ConversationTools toDaprTool(ToolSpecification spec) {
    // Use ToolSpecification's JSON Schema representation directly
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("type", "object");
    // TODO: map ToolSpecification.parameters() to JSON Schema when available
    ConversationToolsFunction fn = new ConversationToolsFunction(spec.name(), parameters);
    if (spec.description() != null) {
      fn.setDescription(spec.description());
    }
    return new ConversationTools(fn);
  }

  private ChatResponse fromDaprResponse(ConversationResponseAlpha2 response) {
    if (response == null || response.getOutputs() == null || response.getOutputs().isEmpty()) {
      return ChatResponse.builder()
          .aiMessage(AiMessage.from(""))
          .build();
    }

    var result = response.getOutputs().get(0);
    if (result.getChoices() == null || result.getChoices().isEmpty()) {
      return ChatResponse.builder()
          .aiMessage(AiMessage.from(""))
          .build();
    }

    ConversationResultChoices choice = result.getChoices().get(0);
    ConversationResultMessage message = choice.getMessage();

    // Check for tool calls
    if (message != null && message.hasToolCalls()) {
      List<dev.langchain4j.agent.tool.ToolExecutionRequest> requests = new ArrayList<>();
      for (ConversationToolCalls tc : message.getToolCalls()) {
        requests.add(dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
            .id(tc.getId())
            .name(tc.getFunction().getName())
            .arguments(tc.getFunction().getArguments())
            .build());
      }
      return ChatResponse.builder()
          .aiMessage(AiMessage.from(requests))
          .build();
    }

    // Text response
    String content = message != null && message.getContent() != null
        ? message.getContent() : "";
    return ChatResponse.builder()
        .aiMessage(AiMessage.from(content))
        .build();
  }
}
