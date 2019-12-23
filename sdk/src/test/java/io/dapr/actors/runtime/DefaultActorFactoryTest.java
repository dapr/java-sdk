/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import io.dapr.actors.ActorId;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 * Testing the default constructor of an Actor.
 */
public class DefaultActorFactoryTest {

  /**
   * A compliant implementation of Actor to be used in the tests below.
   */
  static class MyActor extends AbstractActor implements Actor {

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
  static class InvalidActor extends AbstractActor implements Actor {
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

    Assert.assertEquals(actorId, actor.actorId);
    Assert.assertNotNull(actor.context);
  }

  /**
   * Class is not an actor.
   */
  @Test
  public void noValidConstructor() {
    DefaultActorFactory<InvalidActor> factory = new DefaultActorFactory<>();

    ActorId actorId = ActorId.createRandom();
    InvalidActor actor = factory.createActor(createActorRuntimeContext(InvalidActor.class), actorId);

    Assert.assertNull(actor);
  }

  private static <T extends AbstractActor> ActorRuntimeContext<T> createActorRuntimeContext(Class<T> clazz) {
    return new ActorRuntimeContext(
        mock(ActorRuntime.class),
        mock(ActorStateSerializer.class),
        mock(ActorFactory.class),
        ActorTypeInformation.create(clazz),
        mock(AppToDaprAsyncClient.class));
  }

}
