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

import java.util.List;

/**
 * Alpha2 response from the Dapr Conversation API with enhanced features.
 */
public class ConversationResponseAlpha2 {

  private final String contextId;
  private final List<ConversationResultAlpha2> outputs;

  /**
   * Constructor.
   *
   * @param contextId context id supplied to LLM.
   * @param outputs outputs from the LLM (Alpha2 format).
   */
  public ConversationResponseAlpha2(String contextId, List<ConversationResultAlpha2> outputs) {
    this.contextId = contextId;
    this.outputs = List.copyOf(outputs);
  }

  /**
   * The ID of an existing chat (like in ChatGPT).
   *
   * @return String identifier.
   */
  public String getContextId() {
    return this.contextId;
  }

  /**
   * Get list of conversation outputs (Alpha2 format).
   *
   * @return List{@link ConversationResultAlpha2}.
   */
  public List<ConversationResultAlpha2> getOutputs() {
    return this.outputs;
  }
}
