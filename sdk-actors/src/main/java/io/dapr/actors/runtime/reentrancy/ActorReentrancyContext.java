/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime.reentrancy;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ActorReentrancyContext {
  private final ConcurrentMap<ReentrancyContextKey, ReentrancyContextValue> activeRequests;

  /**
   * Constructor.
   */
  public ActorReentrancyContext() {
    this.activeRequests = new ConcurrentHashMap<>();
  }

  /**
   * Get the Reentrancy ID for the given id/type if present.
   * @param actorId id to query for.
   * @param actorType type to query for.
   * @return Optional containing the Reentrancy ID if present.
   */
  public Optional<String> getReentrancyId(final String actorId, final String actorType) {
    final ReentrancyContextKey key = new ReentrancyContextKey(actorId, actorType);
    if (activeRequests.containsKey(key)) {
      return Optional.of(activeRequests.get(key).getReentrancyId());
    }
    return Optional.empty();
  }

  /**
   * Track or increment a given Reentrancy ID for actor id/type.
   * @param actorId id to track reentrancy for.
   * @param actorType type to track reentrancy for.
   * @param reentrancyId reentrancy to track.
   */
  public void trackReentrancy(final String actorId, final String actorType, final String reentrancyId) {
    final ReentrancyContextKey key = new ReentrancyContextKey(actorId, actorType);
    if (activeRequests.containsKey(key)) {
      final ReentrancyContextValue value = activeRequests.get(key);
      if (!reentrancyId.equals(value.getReentrancyId())) {
        throw new IllegalStateException("Encountered a conflicting Reentrancy ID");
      }
      value.incrementDepth();
    } else {
      final ReentrancyContextValue value = new ReentrancyContextValue(reentrancyId);
      value.incrementDepth();
      activeRequests.put(key, value);
    }
  }

  /**
   * Stop or decrement a given Reentrancy ID's tracking. If depth hits 0, ID is released.
   * @param actorId id to release.
   * @param actorType type to release.
   * @param reentrancyId reentrancy to be released.
   */
  public void releaseReentrancy(final String actorId, final String actorType, final String reentrancyId) {
    final ReentrancyContextKey key = new ReentrancyContextKey(actorId, actorType);
    if (activeRequests.containsKey(key)) {
      final ReentrancyContextValue value = activeRequests.get(key);
      if (!reentrancyId.equals(value.getReentrancyId())) {
        throw new IllegalStateException("Encountered a conflicting Reentrancy ID");
      }

      value.decrementDepth();
      if (value.getCurrentDepth() == 0) {
        activeRequests.remove(key);
      }
    }
  }
}
