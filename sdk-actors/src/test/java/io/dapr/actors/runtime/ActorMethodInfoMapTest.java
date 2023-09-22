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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertThrows;

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
      Assertions.assertEquals("getData", m1.getName());
      Class c = m1.getReturnType();
      Assertions.assertEquals(c.getClass(), String.class.getClass());
      Parameter[] p = m1.getParameters();
      Assertions.assertEquals(p[0].getType().getClass(), String.class.getClass());
    } catch (Exception e) {
      Assertions.fail("Exception not expected.");
    }
  }

  @Test
  public void lookUpNonExistingMethod() throws NoSuchMethodException {
    ArrayList<Class<?>> interfaceTypes = new ArrayList<>();
    interfaceTypes.add(TestActor.class);
    ActorMethodInfoMap m = new ActorMethodInfoMap(interfaceTypes);

    assertThrows(NoSuchMethodException.class, () ->
    m.get("thisMethodDoesNotExist"));
  }

  /**
   * Only used for this test.
   */
  public interface TestActor {
    String getData(String key);
  }
}
