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
import io.dapr.actors.ActorTrace;

import java.lang.reflect.Constructor;

/**
 * Instantiates actors by calling their constructor with {@link ActorRuntimeContext} and {@link ActorId}.
 *
 * @param <T> Actor Type to be created.
 */
class DefaultActorFactory<T extends AbstractActor> implements ActorFactory<T> {

  /**
   * Tracing errors, warnings and info logs.
   */
  private static final ActorTrace ACTOR_TRACE = new ActorTrace();

  /**
   * {@inheritDoc}
   */
  @Override
  public T createActor(ActorRuntimeContext<T> actorRuntimeContext, ActorId actorId) {
    try {
      if (actorRuntimeContext == null) {
        return null;
      }

      Constructor<T> constructor = actorRuntimeContext
          .getActorTypeInformation()
          .getImplementationClass()
          .getConstructor(ActorRuntimeContext.class, ActorId.class);
      return constructor.newInstance(actorRuntimeContext, actorId);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      ACTOR_TRACE.writeError(
            actorRuntimeContext.getActorTypeInformation().getName(),
            actorId.toString(),
            "Failed to create actor instance.");
      throw new RuntimeException(e);
    }
  }

}
