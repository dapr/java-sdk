/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

/**
 * Represents a state change for an actor.
 */
public final class ActorStateChange {

  /**
   * Name of the state being changed.
   */
  private final String stateName;

  /**
   * New value for the state being changed.
   */
  private final Object value;

  /**
   * Type of change {@link ActorStateChangeKind}.
   */
  private final ActorStateChangeKind changeKind;

  /**
   * Creates an actor state change.
   *
   * @param stateName  Name of the state being changed.
   * @param value      New value for the state being changed.
   * @param changeKind Kind of change.
   */
  ActorStateChange(String stateName, Object value, ActorStateChangeKind changeKind) {
    this.stateName = stateName;
    this.value = value;
    this.changeKind = changeKind;
  }

  /**
   * Gets the name of the state being changed.
   *
   * @return Name of the state.
   */
  String getStateName() {
    return stateName;
  }

  /**
   * Gets the new value of the state being changed.
   *
   * @return New value.
   */
  Object getValue() {
    return value;
  }

  /**
   * Gets the kind of change.
   *
   * @return Kind of change.
   */
  ActorStateChangeKind getChangeKind() {
    return changeKind;
  }

}
