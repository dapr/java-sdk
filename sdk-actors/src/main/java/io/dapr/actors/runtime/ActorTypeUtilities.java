/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import java.util.Arrays;

/**
 * Utility class to extract information on Actor type.
 */
final class ActorTypeUtilities {

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
    return (clazz != null)
          && isActor(clazz)
          && (Arrays.stream(clazz.getInterfaces()).filter(t -> t.equals(Remindable.class)).count() > 0);
  }
}
