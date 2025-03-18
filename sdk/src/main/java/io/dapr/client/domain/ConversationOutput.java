package io.dapr.client.domain;

import java.util.Collections;
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
    this.parameters = parameters;
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
    return Collections.unmodifiableMap(this.parameters);
  }
}
