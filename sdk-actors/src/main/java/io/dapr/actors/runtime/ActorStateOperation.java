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
