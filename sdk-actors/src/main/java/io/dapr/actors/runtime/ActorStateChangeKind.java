/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

/**
 * Represents an actor's state change.
 */
public enum ActorStateChangeKind {

  /**
   * No change in state.
   */
  NONE(""),

  /**
   * State needs to be added.
   */
  ADD("upsert"),

  /**
   * State needs to be updated.
   */
  UPDATE("upsert"),

  /**
   * State needs to be removed.
   */
  REMOVE("delete");

  /**
   * Operation name in Dapr's state management.
   */
  private final String daprStateChangeOperation;

  /**
   * Creates a kind of actor state change.
   *
   * @param daprStateChangeOperation Equivalent operation name Dapr's state management
   */
  ActorStateChangeKind(String daprStateChangeOperation) {
    this.daprStateChangeOperation = daprStateChangeOperation;
  }

  /**
   * Gets equivalent operation name Dapr's state management.
   *
   * @return Equivalent operation name Dapr's state management
   */
  String getDaprStateChangeOperation() {
    return daprStateChangeOperation;
  }

}
