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
