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
