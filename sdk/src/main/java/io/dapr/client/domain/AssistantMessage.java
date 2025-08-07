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

package io.dapr.client.domain;

import java.util.List;

/**
 * Assistant message containing responses from the AI model.
 * Can include regular content and/or tool calls that the model wants to make.
 */
public class AssistantMessage implements ConversationMessage {

  private String name;
  private final List<ConversationMessageContent> content;
  private final List<ConversationToolCalls> toolCalls;

  /**
   * Creates an assistant message with content and optional tool calls.
   * @param content the content of the assistant message.
   * @param toolCalls the tool calls requested by the assistant.
   */
  public AssistantMessage(List<ConversationMessageContent> content, List<ConversationToolCalls> toolCalls) {
    this.content = List.copyOf(content);
    this.toolCalls = List.copyOf(toolCalls);
  }

  @Override
  public ConversationMessageRole getRole() {
    return ConversationMessageRole.ASSISTANT;
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * Sets the name of the assistant participant.
   *
   * @param name the name to set
   * @return this instance for method chaining
   */
  public AssistantMessage setName(String name) {
    this.name = name;
    return this;
  }

  @Override
  public List<ConversationMessageContent> getContent() {
    return content;
  }

  public List<ConversationToolCalls> getToolCalls() {
    return toolCalls;
  }
}
