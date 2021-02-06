/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

/**
 * Represents a state operation for actor.
 *
 */
final class ActorStateOperation {

  /**
   * Name of the operation.
   */
  private String operationType;

  /**
   * Key for the state to be persisted.
   */
  private String key;

  /**
   * Value of the state to be persisted.
   */
  private Object value;

  /**
   * Instantiates a new Actor Timer.
   *
   * @param operationType Type of state operation.
   * @param key Key to be persisted.
   * @param value Value to be persisted.
   */
  ActorStateOperation(String operationType,
                      String key,
                      Object value) {
    this.operationType = operationType;
    this.key = key;
    this.value = value;
  }

  /**
   * Gets the type of state operation.
   *
   * @return State operation.
   */
  public String getOperationType() {
    return operationType;
  }

  /**
   * Gets the key to be persisted.
   *
   * @return Key to be persisted.
   */
  public String getKey() {
    return key;
  }

  /**
   * Gets the value to be persisted.
   *
   * @return Value to be persisted.
   */
  public Object getValue() {
    return value;
  }
}
