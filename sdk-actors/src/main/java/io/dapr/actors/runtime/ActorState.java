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

import java.time.Instant;

/**
 * Represents a state change for an actor.
 */
final class ActorState<T> {

  /**
   * Name of the state being changed.
   */
  private final String name;

  /**
   * New value for the state being changed.
   */
  private final T value;

  /**
   * Expiration.
   */
  private final Instant expiration;

  /**
   * Creates a new instance of the metadata on actor state.
   *
   * @param name  Name of the state being changed.
   * @param value Value to be set.
   */
  ActorState(String name, T value) {
    this(name, value, null);
  }

  /**
   * Creates a new instance of the metadata on actor state.
   *
   * @param name  Name of the state being changed.
   * @param value Value to be set.
   * @param expiration When the value is set to expire (recommended but accepts null).
   */
  ActorState(String name, T value, Instant expiration) {
    this.name = name;
    this.value = value;
    this.expiration = expiration;
  }

  /**
   * Gets the name of the state being changed.
   *
   * @return Name of the state.
   */
  String getName() {
    return name;
  }

  /**
   * Gets the new value of the state being changed.
   *
   * @return New value.
   */
  T getValue() {
    return value;
  }

  /**
   * Gets the expiration of the state.
   *
   * @return State expiration.
   */
  Instant getExpiration() {
    return expiration;
  }

}
