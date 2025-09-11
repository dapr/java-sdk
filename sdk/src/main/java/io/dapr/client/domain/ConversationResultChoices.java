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

/**
 * Represents a conversation result choice with finish reason, index, and message.
 */
public class ConversationResultChoices {

  private final String finishReason;
  private final long index;
  private final ConversationResultMessage message;

  /**
   * Constructor.
   *
   * @param finishReason the reason the model stopped generating tokens
   * @param index the index of the choice in the list of choices
   * @param message the result message
   */
  public ConversationResultChoices(String finishReason, long index, ConversationResultMessage message) {
    this.finishReason = finishReason;
    this.index = index;
    this.message = message;
  }

  /**
   * Gets the reason the model stopped generating tokens.
   * This will be "stop" if the model hit a natural stop point or a provided stop sequence,
   * "length" if the maximum number of tokens specified in the request was reached,
   * "content_filter" if content was omitted due to a flag from content filters,
   * "tool_calls" if the model called a tool.
   *
   * @return the finish reason
   */
  public String getFinishReason() {
    return finishReason;
  }

  /**
   * Gets the index of the choice in the list of choices.
   *
   * @return the index
   */
  public long getIndex() {
    return index;
  }

  /**
   * Gets the result message.
   *
   * @return the message
   */
  public ConversationResultMessage getMessage() {
    return message;
  }
}
