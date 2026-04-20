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

public class ConversationResultCompletionUsageDetails {
  private final long acceptedPredictionTokens;
  private final long audioTokens;
  private final long reasoningTokens;
  private final long rejectedPredictionTokens;

  /**
   * Constructor.
   *
   * @param acceptedPredictionTokens accepted prediction tokens used.
   * @param audioTokens              audio tokens used.
   * @param reasoningTokens          reasoning tokens used.
   * @param rejectedPredictionTokens rejected prediction tokens used.
   */
  public ConversationResultCompletionUsageDetails(long acceptedPredictionTokens, long audioTokens,
                                                  long reasoningTokens, long rejectedPredictionTokens) {
    this.acceptedPredictionTokens = acceptedPredictionTokens;
    this.audioTokens = audioTokens;
    this.reasoningTokens = reasoningTokens;
    this.rejectedPredictionTokens = rejectedPredictionTokens;
  }

  /**
   * Accepted prediction tokens used.
   *
   * @return accepted prediction tokens used.
   */
  public long getAcceptedPredictionTokens() {
    return acceptedPredictionTokens;
  }

  /**
   * Audio tokens used.
   *
   * @return audio tokens used.
   */
  public long getAudioTokens() {
    return audioTokens;
  }

  /**
   * Reasoning tokens used.
   *
   * @return reasoning tokens used.
   */
  public long getReasoningTokens() {
    return reasoningTokens;
  }

  /**
   * Rejected prediction tokens used.
   *
   * @return rejected prediction tokens used.
   */
  public long getRejectedPredictionTokens() {
    return rejectedPredictionTokens;
  }
}
