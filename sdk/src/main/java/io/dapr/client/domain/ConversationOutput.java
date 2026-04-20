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

import java.util.Map;

/**
 * Returns the conversation output.
 */
public class ConversationOutput {

  private final String result;

  private final Map<String, byte[]> parameters;

  /**
   * Constructor.
   *
   * @param result result for one of the conversation input.
   * @param parameters all custom fields.
   */
  public ConversationOutput(String result, Map<String, byte[]> parameters) {
    this.result = result;
    this.parameters = Map.copyOf(parameters);
  }

  /**
   * Result for the one conversation input.
   *
   * @return result output from the LLM.
   */
  public String getResult() {
    return this.result;
  }

  /**
   * Parameters for all custom fields.
   *
   * @return parameters.
   */
  public Map<String, byte[]> getParameters() {
    return this.parameters;
  }
}
