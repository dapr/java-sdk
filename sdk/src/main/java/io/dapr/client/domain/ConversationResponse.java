package io.dapr.client.domain;

import java.util.Collections;
import java.util.List;

/**
 * Response from the Dapr Conversation API.
 */
public class ConversationResponse {

  private String contextId;

  private final List<ConversationOutput> daprConversationOutputs;

  /**
   * Constructor.
   *
   * @param contextId context id supplied to LLM.
   * @param daprConversationOutputs outputs from the LLM.
   */
  public ConversationResponse(String contextId, List<ConversationOutput> daprConversationOutputs) {
    this.contextId = contextId;
    this.daprConversationOutputs = daprConversationOutputs;
  }

  /**
   *  The ID of an existing chat (like in ChatGPT).
   *
   * @return String identifier.
   */
  public String getContextId() {
    return this.contextId;
  }

  /**
   * Get list of conversation outputs.
   *
   * @return List{@link ConversationOutput}.
   */
  public List<ConversationOutput> getConversationOutpus() {
    return Collections.unmodifiableList(this.daprConversationOutputs);
  }
}
