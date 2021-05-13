/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime.reentrancy;

import java.util.concurrent.atomic.AtomicInteger;

class ReentrancyContextValue {
  private final String reentrancyId;
  private final AtomicInteger reentrantStackDepth;

  /**
   * Constructor.
   * @param reentrancyId that is being tracked.
   */
  ReentrancyContextValue(final String reentrancyId) {
    this.reentrancyId = reentrancyId;
    this.reentrantStackDepth = new AtomicInteger(0);
  }

  /**
   * Get the Reentrancy ID.
   * @return the Reentrancy ID.
   */
  String getReentrancyId() {
    return reentrancyId;
  }

  /**
   * Increment the reentrant depth.
   */
  void incrementDepth() {
    reentrantStackDepth.incrementAndGet();
  }

  /**
   * Decrement the reentrant depth.
   */
  void decrementDepth() {
    reentrantStackDepth.decrementAndGet();
  }

  /**
   * Get the reentrant depth.
   * @return the reentrant depth.
   */
  int getCurrentDepth() {
    return reentrantStackDepth.get();
  }
}
