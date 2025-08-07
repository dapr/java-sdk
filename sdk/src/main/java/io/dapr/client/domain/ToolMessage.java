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
 * Tool message containing results from function/tool calls.
 * Used to provide the response from a tool execution back to the AI model.
 */
public class ToolMessage implements ConversationMessage {

  private String toolId;
  private String name;
  private final List<ConversationMessageContent> content;

  /**
   * Creates a tool message with content.
   *
   * @param content the content containing the tool execution result
   */
  public ToolMessage(List<ConversationMessageContent> content) {
    this.content = List.copyOf(content);
  }

  @Override
  public ConversationMessageRole getRole() {
    return ConversationMessageRole.TOOL;
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * Sets the tool identifier.
   *
   * @param toolId the tool identifier to set
   * @return this instance for method chaining
   */
  public ToolMessage setToolId(String toolId) {
    this.toolId = toolId;
    return this;
  }

  /**
   * Sets the name of the tool participant.
   *
   * @param name the name to set
   * @return this instance for method chaining
   */
  public ToolMessage setName(String name) {
    this.name = name;
    return this;
  }

  @Override
  public List<ConversationMessageContent> getContent() {
    return content;
  }

  public String getToolId() {
    return toolId;
  }
}
