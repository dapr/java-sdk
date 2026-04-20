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

public class ConversationResultPromptUsageDetails {
  private final long audioTokens;
  private final long cachedTokens;

  /**
   * Constructor.
   *
   * @param audioTokens  audio input tokens present in the prompt.
   * @param cachedTokens cached tokens present in the prompt.
   */
  public ConversationResultPromptUsageDetails(long audioTokens, long cachedTokens) {
    this.audioTokens = audioTokens;
    this.cachedTokens = cachedTokens;
  }

  /**
   * Audio input tokens present in the prompt.
   *
   * @return audio input tokens present in the prompt.
   */
  public long getAudioTokens() {
    return audioTokens;
  }

  /**
   * Cached tokens present in the prompt.
   *
   * @return cached tokens present in the prompt.
   */
  public long getCachedTokens() {
    return cachedTokens;
  }
}
