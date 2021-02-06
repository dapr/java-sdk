/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import io.dapr.actors.ActorType;
import io.dapr.utils.TypeRef;
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
  @ActorType(name = "MyActorWithAnnotation")
  private interface MyActorAnnotated {
  }

  /**
   * Actor interfaced used in this test only.
   */
  private interface MyActor {
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
      public TypeRef getStateType() {
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
    @ActorType(name = "B")
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
   * Checks information for an actor renamed via annotation at interface.
   */
  @Test
  public void renamedWithAnnotationAtInterface() {
    class A extends AbstractActor implements MyActorAnnotated {
      A() {
        super(null, null);
      }
    }

    ActorTypeInformation info = ActorTypeInformation.create(A.class);
    Assert.assertNotNull(info);
    Assert.assertEquals("MyActorWithAnnotation", info.getName());
    Assert.assertEquals(A.class, info.getImplementationClass());
    Assert.assertFalse(info.isAbstractClass());
    Assert.assertFalse(info.isRemindable());
    Assert.assertEquals(1, info.getInterfaces().size());
    Assert.assertTrue(info.getInterfaces().contains(MyActorAnnotated.class));
  }

  /**
   * Checks information for an actor is invalid due to an non-actor parent.
   */
  @Test
  public void nonActorParentClass() {
    abstract class MyAbstractClass implements MyActor {
    }

    class A extends MyAbstractClass {
    }

    ActorTypeInformation info = ActorTypeInformation.tryCreate(A.class);
    Assert.assertNull(info);
  }
}
