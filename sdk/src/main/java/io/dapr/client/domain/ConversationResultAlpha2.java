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

  /**
   * Constructor.
   *
   * @param choices the list of conversation result choices.
   */
  public ConversationResultAlpha2(List<ConversationResultChoices> choices) {
    this.choices = List.copyOf(choices);
  }

  /**
   * Gets the list of conversation result choices.
   *
   * @return the list of conversation result choices
   */
  public List<ConversationResultChoices> getChoices() {
    return choices;
  }
}
