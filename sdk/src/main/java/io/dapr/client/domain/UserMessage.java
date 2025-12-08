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
 * User message containing input from the human user.
 * Represents questions, requests, or other input from the end user.
 */
public class UserMessage implements ConversationMessage {

  private String name;
  private final List<ConversationMessageContent> content;

  /**
   * Creates a user message with content.
   *
   * @param content the content of the user message
   */
  public UserMessage(List<ConversationMessageContent> content) {
    this.content = List.copyOf(content);
  }

  @Override
  public ConversationMessageRole getRole() {
    return ConversationMessageRole.USER;
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * Sets the name of the user participant.
   *
   * @param name the name to set
   * @return this instance for method chaining
   */
  public UserMessage setName(String name) {
    this.name = name;
    return this;
  }

  @Override
  public List<ConversationMessageContent> getContent() {
    return content;
  }
}
