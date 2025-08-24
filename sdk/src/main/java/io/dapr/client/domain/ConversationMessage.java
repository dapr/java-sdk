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
 * Interface representing a conversation message with role-specific content.
 * Supports different message types: system, user, assistant, developer, and tool.
 */
public interface ConversationMessage {

  /**
   * Gets the role of the message sender.
   *
   * @return the message role
   */
  ConversationMessageRole getRole();

  /**
   * Gets the name of the participant in the message.
   *
   * @return the participant name, or null if not specified
   */
  String getName();

  /**
   * Gets the content of the message.
   *
   * @return the message content
   */
  List<ConversationMessageContent> getContent();
}
