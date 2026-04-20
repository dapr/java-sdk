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
 * Alpha2 result for conversation output with enhanced choice-based structure.
 */
public class ConversationResultAlpha2 {

  private final List<ConversationResultChoices> choices;
  private final String model;
  private final ConversationResultCompletionUsage usage;

  /**
   * Constructor.
   *
   * @param choices the list of conversation result choices.
   * @param model the model used for the conversation.
   * @param usage the usage of the model.
   */
  public ConversationResultAlpha2(List<ConversationResultChoices> choices,
                                  String model,
                                  ConversationResultCompletionUsage usage) {
    this.choices = List.copyOf(choices);
    this.model = model;
    this.usage = usage;
  }

  /**
   * Gets the list of conversation result choices.
   *
   * @return the list of conversation result choices
   */
  public List<ConversationResultChoices> getChoices() {
    return choices;
  }

  /**
   * Gets the model used for the conversation.
   *
   * @return the model used for the conversation.
   */
  public String getModel() {
    return model;
  }

  /**
   * Gets the usage of the model.
   *
   * @return the usage of the model.
   */
  public ConversationResultCompletionUsage getUsage() {
    return usage;
  }
}
