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
