/*
 * Copyright 2026 The Dapr Authors
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

public class ConversationResultCompletionUsage {
  private final long completionTokens;
  private final long promptTokens;
  private final long totalTokens;

  private ConversationResultCompletionUsageDetails completionTokenDetails;
  private ConversationResultPromptUsageDetails promptTokenDetails;

  /**
   * Constructor.
   *
   * @param completionTokens completion tokens used.
   * @param promptTokens     prompt tokens used.
   * @param totalTokens      total tokens used.
   */
  public ConversationResultCompletionUsage(long completionTokens, long promptTokens, long totalTokens) {
    this.completionTokens = completionTokens;
    this.promptTokens = promptTokens;
    this.totalTokens = totalTokens;
  }

  /**
   * Completion tokens used.
   *
   * @return completion tokens used.
   */
  public long getCompletionTokens() {
    return completionTokens;
  }

  /**
   * Prompt tokens used.
   *
   * @return prompt tokens used.
   */
  public long getPromptTokens() {
    return promptTokens;
  }

  /**
   * Total tokens used.
   *
   * @return total tokens used.
   */
  public long getTotalTokens() {
    return totalTokens;
  }

  /**
   * Completion token details.
   *
   * @return the completionTokenDetails
   */
  public ConversationResultCompletionUsageDetails getCompletionTokenDetails() {
    return completionTokenDetails;
  }

  /**
   * Completion token details.
   *
   * @param completionTokenDetails the completionTokenDetails to set
   */
  public void setCompletionTokenDetails(ConversationResultCompletionUsageDetails completionTokenDetails) {
    this.completionTokenDetails = completionTokenDetails;
  }

  /**
   * Prompt token details.
   *
   * @return the promptTokenDetails
   */
  public ConversationResultPromptUsageDetails getPromptTokenDetails() {
    return promptTokenDetails;
  }

  /**
   * Prompt token details.
   *
   * @param promptTokenDetails the promptTokenDetails to set
   */
  public void setPromptTokenDetails(ConversationResultPromptUsageDetails promptTokenDetails) {
    this.promptTokenDetails = promptTokenDetails;
  }
}
