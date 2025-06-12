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

package io.dapr.actors;

import java.util.UUID;

/**
 * The ActorId represents the identity of an actor within an actor service.
 */
public class ActorId extends Object implements Comparable<ActorId> {

  /**
   * The ID of the actor as a String.
   */
  private final String stringId;

  /**
   * An error message for an invalid constructor arg.
   */
  private final String errorMsg = "actor needs to be initialized with an id!";

  /**
   * Initializes a new instance of the ActorId class with the id passed in.
   *
   * @param id Value for actor id
   */
  public ActorId(String id) {
    if (id != null) {
      this.stringId = id;
    } else {
      throw new IllegalArgumentException(errorMsg);
    }
  }

  /**
   * Returns the String representation of this Actor's identifier.
   *
   * @return The String representation of this ActorId
   */
  @Override
  public String toString() {
    return this.stringId;
  }

  /**
   * Compares this instance with a specified {link #ActorId} object and
   * indicates whether this instance precedes, follows, or appears in the same
   * position in the sort order as the specified actorId.
   * The comparison is done based on the id if both the instances.
   *
   * @param other The actorId to compare with this instance.
   * @return A 32-bit signed integer that indicates whether this instance
   *     precedes, follows, or appears in the same position in the sort order as the
   *     other parameter.
   */
  @Override
  public int compareTo(ActorId other) {
    return (other == null) ? 1
        : compareContent(this, other);
  }

  /**
   * Calculates the hash code for this ActorId.
   *
   * @return The hash code of this ActorId.
   */
  @Override
  public int hashCode() {
    return this.stringId.hashCode();
  }

  /**
   * Compare if the content of two ids are the same.
   *
   * @param id1 One identifier.
   * @param id2 Another identifier.
   * @return -1, 0, or 1 depending on the compare result of the stringId member.
   */
  private int compareContent(ActorId id1, ActorId id2) {
    return id1.stringId.compareTo(id2.stringId);
  }

  /**
   * Checks if this instance is equals to the other instance.
   *
   * @return true if the 2 ActorId's are equal.
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }

    return hasEqualContent(this, (ActorId) obj);
  }

  /**
   * Creates a new ActorId with a random id.
   *
   * @return A new ActorId with a random id.
   */
  public static ActorId createRandom() {
    UUID id = UUID.randomUUID();
    return new ActorId(id.toString());
  }

  /**
   * Compares if two actors have the same content.
   *
   * @param id1 One identifier.
   * @param id2 Another identifier.
   * @return true if the two ActorId's are equal
   */
  private static boolean hasEqualContent(ActorId id1, ActorId id2) {
    return id1.stringId.equals(id2.stringId);
  }
}
