/*
 * Copyright 2023 The Dapr Authors
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

package io.dapr.springboot;

class DaprTopicBulkSubscribe {
  private boolean enabled;
  private Integer maxMessagesCount;
  private Integer maxAwaitDurationMs;

  DaprTopicBulkSubscribe(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public Integer getMaxAwaitDurationMs() {
    return maxAwaitDurationMs;
  }

  public Integer getMaxMessagesCount() {
    return maxMessagesCount;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public void setMaxAwaitDurationMs(int maxAwaitDurationMs) {
    if (maxAwaitDurationMs < 0) {
      throw new IllegalArgumentException("maxAwaitDurationMs cannot be negative");
    }
    this.maxAwaitDurationMs = maxAwaitDurationMs;
  }

  public void setMaxMessagesCount(int maxMessagesCount) {
    if (maxMessagesCount < 1) {
      throw new IllegalArgumentException("maxMessagesCount must be greater than 0");
    }
    this.maxMessagesCount = maxMessagesCount;
  }
}
