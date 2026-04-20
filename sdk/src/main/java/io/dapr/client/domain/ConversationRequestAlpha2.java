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

import com.google.protobuf.Struct;
import io.dapr.utils.ProtobufUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Represents the Alpha2 conversation configuration with enhanced features including
 * tools, improved message handling, and better compatibility with OpenAI ChatCompletion API.
 */
public class ConversationRequestAlpha2 {

  private final String name;
  private final List<ConversationInputAlpha2> inputs;
  private String contextId;
  private boolean scrubPii;
  private double temperature;
  private List<ConversationTools> tools;
  private String toolChoice;
  private Map<String, Object> parameters;
  private Map<String, String> metadata;
  private Struct responseFormat;
  private Duration promptCacheRetention;

  /**
   * Constructs a ConversationRequestAlpha2 with a component name and conversation inputs.
   *
   * @param name The name of the Dapr conversation component. See a list of all available conversation components
   *                @see <a href="https://docs.dapr.io/reference/components-reference/supported-conversation/"></a>
   * @param inputs    the list of Dapr conversation inputs (Alpha2 format)
   */
  public ConversationRequestAlpha2(String name, List<ConversationInputAlpha2> inputs) {
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
   * Gets the list of Dapr conversation input (Alpha2 format).
   *
   * @return the list of conversation input
   */
  public List<ConversationInputAlpha2> getInputs() {
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
   * @return the current instance of {@link ConversationRequestAlpha2}
   */
  public ConversationRequestAlpha2 setContextId(String contextId) {
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
   * @return the current instance of {@link ConversationRequestAlpha2}
   */
  public ConversationRequestAlpha2 setScrubPii(boolean scrubPii) {
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
   * @return the current instance of {@link ConversationRequestAlpha2}
   */
  public ConversationRequestAlpha2 setTemperature(double temperature) {
    this.temperature = temperature;
    return this;
  }

  /**
   * Gets the tools available to be used by the LLM during the conversation.
   *
   * @return the list of tools
   */
  public List<ConversationTools> getTools() {
    return tools;
  }

  /**
   * Sets the tools available to be used by the LLM during the conversation.
   * These are sent on a per request basis.
   *
   * @param tools the tools to set
   * @return the current instance of {@link ConversationRequestAlpha2}
   */
  public ConversationRequestAlpha2 setTools(List<ConversationTools> tools) {
    this.tools = tools;
    return this;
  }

  /**
   * Gets the tool choice setting which controls which (if any) tool is called by the model.
   *
   * @return the tool choice setting
   */
  public String getToolChoice() {
    return toolChoice;
  }

  /**
   * Sets the tool choice setting which controls which (if any) tool is called by the model.
   * - "none" means the model will not call any tool and instead generates a message
   * - "auto" means the model can pick between generating a message or calling one or more tools
   * - "required" requires one or more functions to be called
   * - Alternatively, a specific tool name may be used here
   *
   * @param toolChoice the tool choice setting to set
   * @return the current instance of {@link ConversationRequestAlpha2}
   */
  public ConversationRequestAlpha2 setToolChoice(String toolChoice) {
    this.toolChoice = toolChoice;
    return this;
  }

  /**
   * Gets the parameters for all custom fields.
   *
   * @return the parameters map
   */
  public Map<String, Object> getParameters() {
    return parameters;
  }

  /**
   * Sets the parameters for all custom fields.
   *
   * @param parameters the parameters to set
   * @return the current instance of {@link ConversationRequestAlpha2}
   */
  public ConversationRequestAlpha2 setParameters(Map<String, Object> parameters) {
    this.parameters = parameters;
    return this;
  }

  /**
   * Gets the metadata passing to conversation components.
   *
   * @return the metadata map
   */
  public Map<String, String> getMetadata() {
    return metadata;
  }

  /**
   * Sets the metadata passing to conversation components.
   *
   * @param metadata the metadata to set
   * @return the current instance of {@link ConversationRequestAlpha2}
   */
  public ConversationRequestAlpha2 setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
    return this;
  }

  /**
   * Gets the response format in JSON-Schema format.
   *
   * @return the response format
   */
  public Struct getResponseFormat() {
    return responseFormat;
  }

  /**
   * Sets the response format in JSON-Schema format.
   * Structured output described using a JSON Schema object.
   * Use this when you want typed structured output.
   * Supported by Deepseek, Google AI, Hugging Face, OpenAI, and Anthropic components
   *
   * @param responseFormat the response format to set
   * @return the current instance of {@link ConversationRequestAlpha2}
   */
  public ConversationRequestAlpha2 setResponseFormat(Struct responseFormat) {
    this.responseFormat = responseFormat;
    return this;
  }

  public ConversationRequestAlpha2 setResponseFormat(String responseFormat) {
    this.responseFormat = ProtobufUtils.jsonToStruct(responseFormat);
    return this;
  }

  /**
   * retention duration for the prompt cache.
   *
   * @return the prompt cache retention duration
   */
  public Duration getPromptCacheRetention() {
    return promptCacheRetention;
  }

  /**
   * Retention duration for the prompt cache.
   * When set, enables extended prompt caching so cached prefixes stay active longer.
   * With OpenAI, supports up to 24 hours.
   * See [OpenAI prompt caching](https://platform.openai.com/docs/guides/prompt-caching#prompt-cache-retention).
   *
   * @param promptCacheRetention the prompt cache retention duration
   * @return the current instance of {@link ConversationRequestAlpha2}
   */
  public ConversationRequestAlpha2 setPromptCacheRetention(Duration promptCacheRetention) {
    this.promptCacheRetention = promptCacheRetention;
    return this;
  }
}
