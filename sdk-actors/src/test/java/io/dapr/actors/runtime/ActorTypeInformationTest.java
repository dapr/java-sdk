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

import io.dapr.actors.ActorType;
import io.dapr.utils.TypeRef;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
    Assertions.assertNotNull(info);
    Assertions.assertEquals("A", info.getName());
    Assertions.assertEquals(A.class, info.getImplementationClass());
    Assertions.assertFalse(info.isAbstractClass());
    Assertions.assertFalse(info.isRemindable());
    Assertions.assertEquals(1, info.getInterfaces().size());
    Assertions.assertTrue(info.getInterfaces().contains(MyActor.class));
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
    Assertions.assertNotNull(info);
    Assertions.assertEquals("A", info.getName());
    Assertions.assertEquals(A.class, info.getImplementationClass());
    Assertions.assertFalse(info.isAbstractClass());
    Assertions.assertTrue(info.isRemindable());
    Assertions.assertEquals(2, info.getInterfaces().size());
    Assertions.assertTrue(info.getInterfaces().contains(Remindable.class));
    Assertions.assertTrue(info.getInterfaces().contains(MyActor.class));
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
    Assertions.assertNotNull(info);
    Assertions.assertEquals("B", info.getName());
    Assertions.assertEquals(A.class, info.getImplementationClass());
    Assertions.assertFalse(info.isAbstractClass());
    Assertions.assertFalse(info.isRemindable());
    Assertions.assertEquals(1, info.getInterfaces().size());
    Assertions.assertTrue(info.getInterfaces().contains(MyActor.class));
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
    Assertions.assertNotNull(info);
    Assertions.assertEquals("MyActorWithAnnotation", info.getName());
    Assertions.assertEquals(A.class, info.getImplementationClass());
    Assertions.assertFalse(info.isAbstractClass());
    Assertions.assertFalse(info.isRemindable());
    Assertions.assertEquals(1, info.getInterfaces().size());
    Assertions.assertTrue(info.getInterfaces().contains(MyActorAnnotated.class));
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
    Assertions.assertNull(info);
  }
}
