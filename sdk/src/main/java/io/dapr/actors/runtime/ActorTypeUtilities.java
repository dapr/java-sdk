/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.runtime;

import java.util.Arrays;

/**
 * Utility class to extract information on Actor type.
 */
final class ActorTypeUtilities {

  /**
   * Gets all interfaces that extend Actor.
   *
   * @param clazz Actor class.
   * @return Array of Actor interfaces.
   */
  public static Class[] getActorInterfaces(Class clazz) {
    if (clazz == null) {
      return new Class[0];
    }

    return Arrays.stream(clazz.getInterfaces())
        .filter(t -> Actor.class.isAssignableFrom(t))
        .filter(t -> getNonActorParentClass(t) == null)
        .toArray(Class[]::new);
  }

  /**
   * Determines if given class is an Actor interface.
   *
   * @param clazz Actor interface candidate.
   * @return Whether this is an Actor interface.
   */
  public static boolean isActorInterface(Class clazz) {
    return (clazz != null) && clazz.isInterface() && (getNonActorParentClass(clazz) == null);
  }

  /**
   * Determines whether this is an Actor class.
   *
   * @param clazz Actor class candidate.
   * @return Whether this is an Actor class.
   */
  public static boolean isActor(Class clazz) {
    if (clazz == null) {
      return false;
    }

    return AbstractActor.class.isAssignableFrom(clazz);
  }

  /**
   * Determines whether this is an remindable Actor.
   *
   * @param clazz Actor class.
   * @return Whether this is an remindable Actor.
   */
  public static boolean isRemindableActor(Class clazz) {
    return (clazz != null) && isActor(clazz) && (Arrays.stream(clazz.getInterfaces()).filter(t -> t.equals(Remindable.class)).count() > 0);
  }

  /**
   * Returns the parent class if it is not the {@link AbstractActor} parent
   * class.
   *
   * @param clazz Actor class.
   * @return Parent class or null if it is {@link AbstractActor}.
   */
  public static Class getNonActorParentClass(Class clazz) {
    if (clazz == null) {
      return null;
    }

    Class[] items = Arrays.stream(clazz.getInterfaces()).filter(t -> !t.equals(Actor.class)).toArray(Class[]::new);
    if (items.length == 0) {
      return clazz;
    }

    for (Class c : items) {
      Class nonActorParent = getNonActorParentClass(c);
      if (nonActorParent != null) {
        return nonActorParent;
      }
    }

    return null;
  }
}
