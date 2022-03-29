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
