/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime.reentrancy;

class ReentrancyContextKey {
  private final String actorId;
  private final String actorType;

  /**
   * Constructor.
   * @param actorId the actors id.
   * @param actorType the actors type.
   */
  public ReentrancyContextKey(final String actorId, final String actorType) {
    this.actorId = actorId;
    this.actorType = actorType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return actorId.hashCode() * actorType.hashCode();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object other) {
    if (!(other instanceof ReentrancyContextKey)) {
      return false;
    }

    final ReentrancyContextKey otherKey = (ReentrancyContextKey) other;

    return actorId.equals(otherKey.actorId) && actorType.equals(otherKey.actorType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return String.format("%s.%s", actorType, actorId);
  }
}
