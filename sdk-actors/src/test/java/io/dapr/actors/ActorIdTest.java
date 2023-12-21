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
package io.dapr.actors;


import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for ActorId.
 */
public class ActorIdTest {

  @Test
  public void initializeNewActorIdObjectWithNullId() {
    assertThrows(IllegalArgumentException.class, () ->{ActorId actorId = new ActorId(null);});
  }

  @Test
  public void getId() {
    String id = "123";
    ActorId actorId = new ActorId(id);
    assertEquals(id, actorId.toString());
  }

  @Test
  public void verifyToString() {
    String id = "123";
    ActorId actorId = new ActorId(id);
    assertEquals(id, actorId.toString());
  }

  @Test
  public void verifyEqualsByObject() {
    List<Wrapper> values = createEqualsTestValues();
    for (Wrapper w : values) {
      assertEquals(w.expectedResult, w.item1.equals(w.item2));
    }
  }

  @Test
  public void verifyEqualsByActorId() {
    List<Wrapper> values = createEqualsTestValues();
    for (Wrapper w : values) {
      ActorId a1 = (ActorId) w.item1;
      Object a2 =  w.item2;
      assertEquals(w.expectedResult, a1.equals(a2));
    }
  }

  @Test
  public void verifyCompareTo() {
    List<Wrapper> values = createComparesToTestValues();
    for (Wrapper w : values) {
      ActorId a1 = (ActorId) w.item1;
      ActorId a2 = (ActorId) w.item2;
      assertEquals(w.expectedResult, a1.compareTo(a2));
    }
  }

  private List<Wrapper> createEqualsTestValues() {
    List<Wrapper> list = new ArrayList<Wrapper>();
    list.add(new Wrapper(new ActorId("1"), null, false));
    list.add(new Wrapper(new ActorId("1"), new ActorId("1"), true));
    list.add(new Wrapper(new ActorId("1"), new Object(), false));
    list.add(new Wrapper(new ActorId("1"), new ActorId("2"), false));

    return list;
  }

  private List<Wrapper> createComparesToTestValues() {
    List<Wrapper> list = new ArrayList<Wrapper>();
    list.add(new Wrapper(new ActorId("1"), null, 1));
    list.add(new Wrapper(new ActorId("1"), new ActorId("1"), 0));
    list.add(new Wrapper(new ActorId("1"), new ActorId("2"), -1));
    list.add(new Wrapper(new ActorId("2"), new ActorId("1"), 1));

    return list;
  }

  class Wrapper<T> {

    public Object item1;
    public Object item2;
    public T expectedResult;

    public Wrapper(Object i, Object j, T e) {
      this.item1 = i;
      this.item2 = j;
      this.expectedResult = e;
    }
  }
}
