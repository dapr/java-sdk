package io.dapr.client.domain;

import java.util.List;

/**
 * Represents a conversation configuration with details about component name,
 * conversation inputs, context identifier, PII scrubbing, and temperature control.
 */
public class ConversationRequest {

  private final String llmName;
  private final List<ConversationInput> daprConversationInputs;
  private String contextId;
  private boolean scrubPii;
  private double temperature;

  /**
   * Constructs a DaprConversation with a component name and conversation inputs.
   *
   * @param llmName The name of the LLM component. See a list of all available conversation components
   *                @see <a href="https://docs.dapr.io/reference/components-reference/supported-conversation/"></a>
   * @param conversationInputs    the list of Dapr conversation inputs
   */
  public ConversationRequest(String llmName, List<ConversationInput> conversationInputs) {
    this.llmName = llmName;
    this.daprConversationInputs = conversationInputs;
  }

  /**
   * Gets the conversation component name.
   *
   * @return the conversation component name
   */
  public String getLlmName() {
    return llmName;
  }

  /**
   * Gets the list of Dapr conversation inputs.
   *
   * @return the list of conversation inputs
   */
  public List<ConversationInput> getConversationInputs() {
    return daprConversationInputs;
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