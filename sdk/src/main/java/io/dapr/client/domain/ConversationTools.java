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
 * Represents tool definitions that can be used during conversation.
 */
public class ConversationTools {

  private final String type;
  private final ConversationFunction function;

  /**
   * Constructor.
   *
   * @param type     the type of tool (e.g., "function")
   * @param function the function definition
   */
  public ConversationTools(String type, ConversationFunction function) {
    this.type = type;
    this.function = function;
  }

  /**
   * Gets the tool type.
   *
   * @return the tool type
   */
  public String getType() {
    return type;
  }

  /**
   * Gets the function definition.
   *
   * @return the function definition
   */
  public ConversationFunction getFunction() {
    return function;
  }
}
