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

import io.dapr.actors.ActorId;

/**
 * Creates an actor of a given type.
 *
 * @param <T> Actor Type to be created.
 */
@FunctionalInterface
public interface ActorFactory<T extends AbstractActor> {

  /**
   * Creates an Actor.
   *
   * @param actorRuntimeContext Actor type's context in the runtime.
   * @param actorId             Actor Id.
   * @return Actor or null it failed.
   */
  T createActor(ActorRuntimeContext<T> actorRuntimeContext, ActorId actorId);
}
