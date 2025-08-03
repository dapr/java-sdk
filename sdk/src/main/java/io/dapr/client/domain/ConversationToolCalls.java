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

/**
 * Represents a tool call request sent from the LLM to the client to execute.
 */
public class ConversationToolCalls {

  private final String id;
  private final ConversationToolCallsFunction function;

  /**
   * Constructor.
   *
   * @param id the unique identifier for the tool call (optional)
   * @param function the function to call
   */
  public ConversationToolCalls(String id, ConversationToolCallsFunction function) {
    this.id = id;
    this.function = function;
  }

  /**
   * Constructor without ID.
   *
   * @param function the function to call
   */
  public ConversationToolCalls(ConversationToolCallsFunction function) {
    this(null, function);
  }

  /**
   * Gets the unique identifier for the tool call.
   *
   * @return the tool call ID, or null if not provided
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the function to call.
   *
   * @return the function details
   */
  public ConversationToolCallsFunction getFunction() {
    return function;
  }
}
