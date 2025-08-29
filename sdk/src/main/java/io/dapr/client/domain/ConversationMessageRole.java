/*
 * Copyright 2022 The Dapr Authors
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

/**
 * Enum representing the different roles a conversation message can have.
 */
public enum ConversationMessageRole {
  /**
   * System message that sets the behavior or context for the conversation.
   */
  SYSTEM,

  /**
   * User message containing input from the human user.
   */
  USER,

  /**
   * Assistant message containing responses from the AI model.
   */
  ASSISTANT,

  /**
   * Tool message containing results from function/tool calls.
   */
  TOOL,

  /**
   * Developer message for development and debugging purposes.
   */
  DEVELOPER
}
