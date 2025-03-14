package io.dapr.ai.client;

/**
 * Represents an input message for a conversation with an LLM.
 */
public class DaprConversationInput {

  private final String content;

  private DaprConversationRole role;

  private boolean scrubPii;

  public DaprConversationInput(String content) {
    this.content = content;
  }

  /**
   * Retrieves the content of the conversation input.
   *
   * @return The content to be sent to the LLM.
   */
  public String getContent() {
    return content;
  }

  /**
   * Retrieves the role associated with the conversation input.
   *
   * @return this.
   */
  public DaprConversationRole getRole() {
    return role;
  }

  /**
   * Sets the role associated with the conversation input.
   *
   * @param role The role to assign to the message.
   * @return this.
   */
  public DaprConversationInput setRole(DaprConversationRole role) {
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
   * Sets whether to scrub Personally Identifiable Information (PII) before sending to the LLM.
   *
   * @param scrubPii A boolean indicating whether to remove PII.
   * @return this.
   */
  public DaprConversationInput setScrubPii(boolean scrubPii) {
    this.scrubPii = scrubPii;
    return this;
  }
}
