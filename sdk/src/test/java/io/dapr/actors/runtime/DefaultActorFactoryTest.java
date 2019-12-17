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

    ActorService actorService;

    ActorId actorId;

    public MyActor(ActorService actorService, ActorId actorId) {
      this.actorService = actorService;
      this.actorId = actorId;
    }
  }

  /**
   * A non-compliant implementation of Actor to be used in the tests below.
   */
  static class InvalidActor extends AbstractActor {
  }

  /**
   * Happy case.
   */
  @Test
  public void happyActor() {
    DefaultActorFactory<MyActor> factory = new DefaultActorFactory(ActorTypeInformation.tryCreate(MyActor.class));

    ActorId actorId = ActorId.createRandom();
    MyActor actor = factory.createActor(mock(ActorService.class), actorId);

    Assert.assertEquals(actorId, actor.actorId);
    Assert.assertNotNull(actor.actorService);
  }

  /**
   * Class is not an actor.
   */
  @Test
  public void noValidConstructor() {
    DefaultActorFactory<InvalidActor> factory = new DefaultActorFactory(ActorTypeInformation.tryCreate(InvalidActor.class));

    ActorId actorId = ActorId.createRandom();
    InvalidActor actor = factory.createActor(mock(ActorService.class), actorId);

    Assert.assertNull(actor);
  }

}
