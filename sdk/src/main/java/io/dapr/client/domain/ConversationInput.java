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

package io.dapr.client.domain;

/**
 * Represents an input message for a conversation with an LLM.
 */
public class ConversationInput {

  private final String content;

  private String role;

  private boolean scrubPii;

  /**
   * Constructor.
   *
   * @param content for the llm.
   */
  public ConversationInput(String content) {
    this.content = content;
  }

  /**
   * The message content to send to the LLM. Required
   *
   * @return The content to be sent to the LLM.
   */
  public String getContent() {
    return content;
  }

  /**
   * The role for the LLM to assume.
   *
   * @return this.
   */
  public String getRole() {
    return role;
  }

  /**
   * Set the role for LLM to assume.
   *
   * @param role The role to assign to the message.
   * @return this.
   */
  public ConversationInput setRole(String role) {
    this.role = role;
    return this;
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
  public ConversationInput setScrubPii(boolean scrubPii) {
    this.scrubPii = scrubPii;
    return this;
  }
}
