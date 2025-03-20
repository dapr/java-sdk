package io.dapr.client.domain;

/**
 * Represents an input message for a conversation with an LLM.
 */
public class ConversationInput {

  private final String content;

  private ConversationRole role;

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
  public ConversationRole getRole() {
    return role;
  }

  /**
   * Sets the role for LLM to assume.
   *
   * @param role The role to assign to the message.
   * @return this.
   */
  public ConversationInput setRole(ConversationRole role) {
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
