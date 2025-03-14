package io.dapr.ai.client;

import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Defines client operations for managing Dapr AI instances.
 */
interface DaprAiClient {

  /**
   * Method to call the Dapr Converse API.
   *
   * @param conversationComponentName name for the conversation component.
   * @param daprConversationInputs prompts that are part of the conversation.
   * @param contextId identifier of an existing chat (like in ChatGPT)
   * @param scrubPii data that comes from the LLM.
   * @param temperature to optimize from creativity or predictability.
   * @return @ConversationResponse.
   */
  Mono<DaprConversationResponse> converse(
      String conversationComponentName,
      List<DaprConversationInput> daprConversationInputs,
      String contextId,
      boolean scrubPii,
      double temperature);

  /**
   * Method to call the Dapr Converse API.
   *
   * @param conversationComponentName name for the conversation component.
   * @param daprConversationInputs prompts that are part of the conversation.
   * @return @ConversationResponse.
   */
  Mono<DaprConversationResponse> converse(
      String conversationComponentName,
      List<DaprConversationInput> daprConversationInputs);
}
