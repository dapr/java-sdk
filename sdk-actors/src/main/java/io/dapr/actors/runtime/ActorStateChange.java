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
   * State being changed.
   */
  private final ActorState state;

  /**
   * Type of change {@link ActorStateChangeKind}.
   */
  private final ActorStateChangeKind changeKind;

  /**
   * Creates an actor state change.
   *
   * @param state     State being changed.
   * @param changeKind Kind of change.
   */
  ActorStateChange(ActorState state, ActorStateChangeKind changeKind) {
    this.state = state;
    this.changeKind = changeKind;
  }

  /**
   * Gets the state being changed.
   *
   * @return state.
   */
  ActorState getState() {
    return state;
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
