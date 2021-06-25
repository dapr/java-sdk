/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime.reentrancy;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ReentrancyStack {

  private final AtomicReference<String> storedReentrancyId;
  private final AtomicInteger stackDepth;

  /**
   * Constructor.
   */
  public ReentrancyStack() {
    this.storedReentrancyId = new AtomicReference<>(null);
    this.stackDepth = new AtomicInteger(0);
  }

  /**
   * Determine if a request is in progress or not.
   * @return {@code true} if a request is ongoing, {@code false} otherwise.
   */
  public boolean inProgress() {
    return stackDepth.get() > 0;
  }

  /**
   * Determine if we can start a new request. {@code true} if no request is ongoing
   * or if the reentrancyId matches the currently stored one. {@code false} otherwise.
   * @param reentrancyId to check for a matching request.
   * @return {@code true} if we can start a new request, {@code false} otherwise.
   */
  public boolean isOpen(final String reentrancyId) {
    final String currentReentrancyId = storedReentrancyId.get();
    if (currentReentrancyId != null && currentReentrancyId.equals(reentrancyId)) {
      return true;
    }

    return stackDepth.get() == 0;
  }

  /**
   * Begins a request or increments the tracking stack if the reentrancyId matches.
   * @param reentrancyId to match against incoming requests.
   */
  public void startOrIncreaseStack(final String reentrancyId) {
    final String currentReentrancyId = storedReentrancyId.get();

    if (currentReentrancyId != null && !currentReentrancyId.equals(reentrancyId)) {
      throw new IllegalStateException("Conflicting requests, finish prior request before starting a new one.");
    }

    final int currentStack = stackDepth.get();

    if (reentrancyId == null && currentStack != 0) {
      throw new IllegalStateException("Non reentrant request should not be able to run in parallel.");
    }

    if (currentStack > 0 && !reentrancyId.equals(currentReentrancyId)) {
      throw new IllegalStateException("Conflicting requests, finish prior request before starting a new one.");
    }

    if (currentReentrancyId == null) {
      storedReentrancyId.set(reentrancyId);
    }

    stackDepth.incrementAndGet();
  }

  /**
   * Ends or decrements the stack if the reentrancyId matches the current request.
   * @param reentrancyId to match with.
   */
  public void endOrDecrementStack(final String reentrancyId) {
    final String currentReentrancyId = storedReentrancyId.get();

    if (currentReentrancyId != null && !currentReentrancyId.equals(reentrancyId)
        || currentReentrancyId == null && reentrancyId != null) {
      throw new IllegalStateException("Conflicting requests, finish prior request before starting a new one.");
    }

    if (stackDepth.get() == 0) {
      throw new IllegalStateException("Cannot decrement a stack past 0.");
    }

    final int depth = stackDepth.decrementAndGet();
    if (depth == 0) {
      storedReentrancyId.set(null);
    }
  }
}
