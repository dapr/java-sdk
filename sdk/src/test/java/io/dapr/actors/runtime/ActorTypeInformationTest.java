/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Unit tests for ActorTypeInformation.
 */
public class ActorTypeInformationTest {

  /**
   * Actor interfaced used in this test only.
   */
  private interface MyActor extends Actor {
  }

  /**
   * Checks information for a non-remindable actor.
   */
  @Test
  public void notRemindable() {

    class A extends AbstractActor implements MyActor {
      A() {
        super(null, null);
      }
    }

    ActorTypeInformation info = ActorTypeInformation.create(A.class);
    Assert.assertNotNull(info);
    Assert.assertEquals("A", info.getName());
    Assert.assertEquals(A.class, info.getImplementationClass());
    Assert.assertFalse(info.isAbstractClass());
    Assert.assertFalse(info.isRemindable());
    Assert.assertEquals(1, info.getInterfaces().size());
    Assert.assertTrue(info.getInterfaces().contains(MyActor.class));
  }

  /**
   * Checks information for a remindable actor.
   */
  @Test
  public void remindable() {

    class A extends AbstractActor implements MyActor, Remindable {
      A() {
        super(null, null);
      }

      @Override
      public Class getReminderStateType() {
        return null;
      }

      @Override
      public Mono<Void> receiveReminder(String reminderName, Object state, Duration dueTime, Duration period) {
        return null;
      }
    }

    ActorTypeInformation info = ActorTypeInformation.create(A.class);
    Assert.assertNotNull(info);
    Assert.assertEquals("A", info.getName());
    Assert.assertEquals(A.class, info.getImplementationClass());
    Assert.assertFalse(info.isAbstractClass());
    Assert.assertTrue(info.isRemindable());
    Assert.assertEquals(2, info.getInterfaces().size());
    Assert.assertTrue(info.getInterfaces().contains(Remindable.class));
    Assert.assertTrue(info.getInterfaces().contains(MyActor.class));
  }

  /**
   * Checks information for an actor renamed via annotation.
   */
  @Test
  public void renamedWithAnnotation() {
    @ActorType(Name = "B")
    class A extends AbstractActor implements MyActor {
      A() {
        super(null, null);
      }
    }

    ActorTypeInformation info = ActorTypeInformation.create(A.class);
    Assert.assertNotNull(info);
    Assert.assertEquals("B", info.getName());
    Assert.assertEquals(A.class, info.getImplementationClass());
    Assert.assertFalse(info.isAbstractClass());
    Assert.assertFalse(info.isRemindable());
    Assert.assertEquals(1, info.getInterfaces().size());
    Assert.assertTrue(info.getInterfaces().contains(MyActor.class));
  }

  /**
   * Checks information for an actor is invalid due to an non-actor parent.
   */
  @Test
  public void nonActorParentClass() {
    abstract class MyAbstractClass extends AbstractActor implements MyActor {
      MyAbstractClass() {
        super(null, null);
      }
    }

    class A extends MyAbstractClass {
    }

    ActorTypeInformation info = ActorTypeInformation.tryCreate(A.class);
    Assert.assertNull(info);
  }
}
