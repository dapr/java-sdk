/*
 * Copyright 2021 The Dapr Authors
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

package io.dapr.actors.runtime;

/**
 * Represents the configuration for the Actor Type.
 */
public class ActorReentrancyConfig {

  private volatile Boolean enabled;

  private volatile Integer maxStackDepth;

  /**
   * Instantiates a new config for the Actor Reentrancy.
   */
  ActorReentrancyConfig() {
  }

  /**
   * Gets whether reentrancy is enabled.
   *
   * @return Whether reentrancy is enabled.
   */
  public Boolean getEnabled() {
    return enabled;
  }

  /**
   * Sets whether reentrancy should be enabled.
   *
   * @param enabled Whether reentrancy should be enabled.
   * @return This instance.
   */
  public ActorReentrancyConfig setEnabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * Gets the number of max stack depth.
   *
   * @return The number of max stack depth.
   */
  public Integer getMaxStackDepth() {
    return maxStackDepth;
  }

  /**
   * Sets the number of max stack depth.
   *
   * @param maxStackDepth The number of max stack depth.
   * @return This instance.
   */
  public ActorReentrancyConfig setMaxStackDepth(Integer maxStackDepth) {
    this.maxStackDepth = maxStackDepth;
    return this;
  }

}
