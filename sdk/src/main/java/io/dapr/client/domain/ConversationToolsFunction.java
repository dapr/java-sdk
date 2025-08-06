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

import java.util.Map;

/**
 * Represents a function definition for conversation tools.
 */
public class ConversationToolsFunction {

  private String description;
  private final String name;
  private final Map<String, Object> parameters;

  /**
   * Constructor.
   *
   * @param name        the function name
   * @param parameters  the function parameters schema
   */
  public ConversationToolsFunction(String name, Map<String, Object> parameters) {
    this.name = name;
    this.parameters = parameters;
  }

  /**
   * Gets the function name.
   *
   * @return the function name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the function description.
   *
   * @return the function description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the function description.
   *
   * @param description the function description
   * @return this instance for method chaining
   */
  public ConversationToolsFunction setDescription(String description) {
    this.description = description;
    return this;
  }

  /**
   * Gets the function parameters schema.
   *
   * @return the function parameters
   */
  public Map<String, Object> getParameters() {
    return parameters;
  }
}
