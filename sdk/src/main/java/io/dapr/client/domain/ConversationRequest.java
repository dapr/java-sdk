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

import java.util.List;

/**
 * Represents a conversation configuration with details about component name,
 * conversation inputs, context identifier, PII scrubbing, and temperature control.
 */
public class ConversationRequest {

  private final String name;
  private final List<ConversationInput> inputs;
  private String contextId;
  private boolean scrubPii;
  private double temperature;

  /**
   * Constructs a DaprConversation with a component name and conversation inputs.
   *
   * @param name The name of the Dapr conversation component. See a list of all available conversation components
   *                @see <a href="https://docs.dapr.io/reference/components-reference/supported-conversation/"></a>
   * @param inputs    the list of Dapr conversation inputs
   */
  public ConversationRequest(String name, List<ConversationInput> inputs) {
    this.name = name;
    this.inputs = inputs;
  }

  /**
   * Gets the conversation component name.
   *
   * @return the conversation component name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the list of Dapr conversation input.
   *
   * @return the list of conversation input
   */
  public List<ConversationInput> getInputs() {
    return inputs;
  }

  /**
   * Gets the context identifier.
   *
   * @return the context identifier
   */
  public String getContextId() {
    return contextId;
  }

  /**
   * Sets the context identifier.
   *
   * @param contextId the context identifier to set
   * @return the current instance of {@link ConversationRequest}
   */
  public ConversationRequest setContextId(String contextId) {
    this.contextId = contextId;
    return this;
  }

  /**
   * Checks if PII scrubbing is enabled.
   *
   * @return true if PII scrubbing is enabled, false otherwise
   */
  public boolean isScrubPii() {
    return scrubPii;
  }

  /**
   * Enable obfuscation of sensitive information returning from the LLM. Optional.
   *
   * @param scrubPii whether to enable PII scrubbing
   * @return the current instance of {@link ConversationRequest}
   */
  public ConversationRequest setScrubPii(boolean scrubPii) {
    this.scrubPii = scrubPii;
    return this;
  }

  /**
   * Gets the temperature of the model. Used to optimize for consistency and creativity. Optional
   *
   * @return the temperature value
   */
  public double getTemperature() {
    return temperature;
  }

  /**
   * Sets the temperature of the model. Used to optimize for consistency and creativity. Optional
   *
   * @param temperature the temperature value to set
   * @return the current instance of {@link ConversationRequest}
   */
  public ConversationRequest setTemperature(double temperature) {
    this.temperature = temperature;
    return this;
  }
}
