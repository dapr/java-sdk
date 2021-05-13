/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.config;

import javax.annotation.Nullable;

public class ActorReentrancyConfig {
  private final boolean enabled;
  private final Integer maxStackDepth;

  /**
   * Constructor.
   * @param enabled boolean stating if reentrancy is required or not
   * @param maxStackDepth optional integer that sets the max stack depth for reentrancy, can be null
   */
  public ActorReentrancyConfig(final boolean enabled, @Nullable final Integer maxStackDepth) {
    this.enabled = enabled;
    this.maxStackDepth = maxStackDepth;
  }

  /**
   * Get whether reentrancy is enabled or not.
   * @return {@code boolean} stating if reentrancy is enabled
   */
  public boolean getEnabled() {
    return enabled;
  }

  /**
   * Get the max stack depth for reentrancy if specified, can be null (default max stack depth).
   * @return {@code Integer} specifying max reentrancy depth, null if not set
   */
  public Integer getMaxStackDepth() {
    return maxStackDepth;
  }
}
