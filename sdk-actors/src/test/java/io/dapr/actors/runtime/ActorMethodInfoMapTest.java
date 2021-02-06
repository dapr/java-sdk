/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;

/**
 * Unit tests for ActorMethodInfoMap.
 */
public class ActorMethodInfoMapTest {

  @Test
  public void normalUsage() {
    ArrayList<Class<?>> interfaceTypes = new ArrayList<>();
    interfaceTypes.add(TestActor.class);
    ActorMethodInfoMap m = new ActorMethodInfoMap(interfaceTypes);

    try {
      Method m1 = m.get("getData");
      Assert.assertEquals("getData", m1.getName());
      Class c = m1.getReturnType();
      Assert.assertEquals(c.getClass(), String.class.getClass());
      Parameter[] p = m1.getParameters();
      Assert.assertEquals(p[0].getType().getClass(), String.class.getClass());
    } catch (Exception e) {
      Assert.fail("Exception not expected.");
    }
  }

  @Test(expected = NoSuchMethodException.class)
  public void lookUpNonExistingMethod() throws NoSuchMethodException {
    ArrayList<Class<?>> interfaceTypes = new ArrayList<>();
    interfaceTypes.add(TestActor.class);
    ActorMethodInfoMap m = new ActorMethodInfoMap(interfaceTypes);

    m.get("thisMethodDoesNotExist");
  }

  /**
   * Only used for this test.
   */
  public interface TestActor {
    String getData(String key);
  }
}
