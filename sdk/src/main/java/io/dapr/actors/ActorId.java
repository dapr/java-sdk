/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
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
     * Returns the id of the actor as {link #java.lang.String}
     *
     * @return ActorID as {link #java.lang.String}
     */
    public String getStringId() {
        return this.stringId;
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
     * Determines whether two specified actorIds have the same id.
     *
     * @param id1 The first actorId to compare, or null
     * @param id2 The second actorId to compare, or null.
     * @return true if the id is same for both objects; otherwise, false.
     */
    static public boolean equals(ActorId id1, ActorId id2) {
        if (id1 == null && id2 == null) {
            return true;
        } else if (id2 == null || id1 == null) {
            return false;
        } else {
            return hasEqualContent(id1, id2);
        }
    }

    /**
     * Compares this instance with a specified {link #ActorId} object and
     * indicates whether this instance precedes, follows, or appears in the same
     * position in the sort order as the specified actorId.
     * <p>
     * The comparison is done based on the id if both the instances.
     *
     * @param other The actorId to compare with this instance.
     * @return A 32-bit signed integer that indicates whether this instance
     * precedes, follows, or appears in the same position in the sort order as the
     * other parameter.
     */
    @Override
    public int compareTo(ActorId other) {
        return (other == null) ? 1
                : compareContent(this, other);
    }

    /**
     *
     * @param id1
     * @param id2
     * @return true if the two ActorId's are equal
     */
    static private boolean hasEqualContent(ActorId id1, ActorId id2) {
        return id1.getStringId().equalsIgnoreCase(id2.getStringId());
    }

    /**
     *
     * @param id1
     * @param id2
     * @return -1, 0, or 1 depending on the compare result of the stringId member.
     */
    private int compareContent(ActorId id1, ActorId id2) {
        return id1.getStringId().compareToIgnoreCase(id2.getStringId());
    }

    /**
     *
     * @return The String representation of this ActorId
     */
    @Override
    public String toString() {
        return this.stringId;
    }

    /**
     *
     * @return The hash code of this ActorId
     */
    @Override
    public int hashCode() {
        return this.stringId.hashCode();
    }

    /**
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
}
