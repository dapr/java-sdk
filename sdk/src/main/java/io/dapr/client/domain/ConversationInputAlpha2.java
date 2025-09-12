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
 * Represents an Alpha2 input for conversation with enhanced message support.
 */
public class ConversationInputAlpha2 {

  private final List<ConversationMessage> messages;
  private boolean scrubPii;

  /**
   * Constructor.
   *
   * @param messages the list of conversation messages
   */
  public ConversationInputAlpha2(List<ConversationMessage> messages) {
    this.messages = List.copyOf(messages);
  }

  /**
   * Gets the list of conversation messages.
   *
   * @return the list of messages
   */
  public List<ConversationMessage> getMessages() {
    return messages;
  }

  /**
   * Checks if Personally Identifiable Information (PII) should be scrubbed before sending to the LLM.
   *
   * @return {@code true} if PII should be scrubbed, {@code false} otherwise.
   */
  public boolean isScrubPii() {
    return scrubPii;
  }

  /**
   * Enable obfuscation of sensitive information present in the content field. Optional
   *
   * @param scrubPii A boolean indicating whether to remove PII.
   * @return this.
   */
  public ConversationInputAlpha2 setScrubPii(boolean scrubPii) {
    this.scrubPii = scrubPii;
    return this;
  }
}
