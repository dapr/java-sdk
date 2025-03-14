package io.dapr.ai.client;

import java.util.Collections;
import java.util.List;

/**
 * Response from the Dapr Conversation API.
 */
public class DaprConversationResponse {

  private String contextId;

  private final List<DaprConversationOutput> daprConversationOutputs;

  /**
   * Constructor.
   *
   * @param contextId context id supplied to LLM.
   * @param daprConversationOutputs outputs from the LLM.
   */
  public DaprConversationResponse(String contextId, List<DaprConversationOutput> daprConversationOutputs) {
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
   * @return List{@link DaprConversationOutput}.
   */
  public List<DaprConversationOutput> getDaprConversationOutputs() {
    return Collections.unmodifiableList(this.daprConversationOutputs);
  }
}
