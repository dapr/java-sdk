/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import io.dapr.actors.ActorId;
import io.dapr.serializer.DaprObjectSerializer;
import org.junit.Assert;
import org.junit.Test;

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

    Assert.assertEquals(actorId, actor.actorId);
    Assert.assertNotNull(actor.context);
  }

  /**
   * Class is not an actor.
   */
  @Test(expected = RuntimeException.class)
  public void noValidConstructor() {
    DefaultActorFactory<InvalidActor> factory = new DefaultActorFactory<>();

    ActorId actorId = ActorId.createRandom();

    factory.createActor(createActorRuntimeContext(InvalidActor.class), actorId);
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
