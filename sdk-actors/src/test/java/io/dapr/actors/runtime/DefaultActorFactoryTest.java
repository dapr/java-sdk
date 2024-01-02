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
import io.dapr.serializer.DaprObjectSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * Testing the default constructor of an Actor.
 */
public class DefaultActorFactoryTest {

  /**
   * A compliant implementation of Actor to be used in the tests below.
   */
  static class MyActor extends AbstractActor {

    ActorRuntimeContext<MyActor> context;

    ActorId actorId;

    public MyActor(ActorRuntimeContext<MyActor> context, ActorId actorId) {
      super(context, actorId);
      this.context = context;
      this.actorId = actorId;
    }
  }

  /**
   * A non-compliant implementation of Actor to be used in the tests below.
   */
  static class InvalidActor extends AbstractActor {
    InvalidActor() {
      super(null, null);
    }
  }

  /**
   * Happy case.
   */
  @Test
  public void happyActor() {
    DefaultActorFactory<MyActor> factory = new DefaultActorFactory<>();

    ActorId actorId = ActorId.createRandom();
    MyActor actor = factory.createActor(createActorRuntimeContext(MyActor.class), actorId);

    Assertions.assertEquals(actorId, actor.actorId);
    Assertions.assertNotNull(actor.context);
  }

  /**
   * Class is not an actor.
   */
  @Test
  public void noValidConstructor() {
    DefaultActorFactory<InvalidActor> factory = new DefaultActorFactory<>();

    ActorId actorId = ActorId.createRandom();

    assertThrows(RuntimeException.class, () ->
    factory.createActor(createActorRuntimeContext(InvalidActor.class), actorId));
  }

  private static <T extends AbstractActor> ActorRuntimeContext<T> createActorRuntimeContext(Class<T> clazz) {
    return new ActorRuntimeContext(
        mock(ActorRuntime.class),
        mock(DaprObjectSerializer.class),
        mock(ActorFactory.class),
        ActorTypeInformation.create(clazz),
        mock(DaprClient.class),
        mock(DaprStateAsyncProvider.class));
  }

}
