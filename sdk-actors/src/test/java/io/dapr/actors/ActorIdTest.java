/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */
package io.dapr.actors;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for ActorId.
 */
public class ActorIdTest {

  @Test(expected = IllegalArgumentException.class)
  public void initializeNewActorIdObjectWithNullId() {
    ActorId actorId = new ActorId(null);
  }

  @Test
  public void getId() {
    String id = "123";
    ActorId actorId = new ActorId(id);
    Assert.assertEquals(id, actorId.toString());
  }

  @Test
  public void verifyToString() {
    String id = "123";
    ActorId actorId = new ActorId(id);
    Assert.assertEquals(id, actorId.toString());
  }

  @Test
  public void verifyEqualsByObject() {
    List<Wrapper> values = createEqualsTestValues();
    for (Wrapper w : values) {
      Assert.assertEquals(w.expectedResult, w.item1.equals(w.item2));
    }
  }

  @Test
  public void verifyEqualsByActorId() {
    List<Wrapper> values = createEqualsTestValues();
    for (Wrapper w : values) {
      ActorId a1 = (ActorId) w.item1;
      Object a2 =  w.item2;
      Assert.assertEquals(w.expectedResult, a1.equals(a2));
    }
  }

  @Test
  public void verifyCompareTo() {
    List<Wrapper> values = createComparesToTestValues();
    for (Wrapper w : values) {
      ActorId a1 = (ActorId) w.item1;
      ActorId a2 = (ActorId) w.item2;
      Assert.assertEquals(w.expectedResult, a1.compareTo(a2));
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
