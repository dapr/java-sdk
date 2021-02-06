/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors;

public final class ActorUtils {

  /**
   * Finds the actor type name for the given class or interface.
   *
   * @param actorClass Class or interface for Actor Type.
   * @return Name for Actor Type.
   */
  public static String findActorTypeName(Class<?> actorClass) {
    if (actorClass == null) {
      throw new IllegalArgumentException("ActorClass is required.");
    }

    Class<?> node = actorClass;
    while ((node != null) && (node.getAnnotation(ActorType.class) == null)) {
      node = node.getSuperclass();
    }

    if (node == null) {
      // No annotation found in parent classes, so we scan interfaces.
      for (Class<?> interfaze : actorClass.getInterfaces()) {
        if (interfaze.getAnnotation(ActorType.class) != null) {
          node = interfaze;
          break;
        }
      }
    }

    if (node == null) {
      // No ActorType annotation found, so we use the class name.
      return actorClass.getSimpleName();
    }

    ActorType actorTypeAnnotation = node.getAnnotation(ActorType.class);
    return actorTypeAnnotation != null ? actorTypeAnnotation.name() : actorClass.getSimpleName();
  }
}
