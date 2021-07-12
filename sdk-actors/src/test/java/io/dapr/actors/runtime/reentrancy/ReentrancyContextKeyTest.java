package io.dapr.actors.runtime.reentrancy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ReentrancyContextKeyTest {

  @Test
  public void TestReentrancyContextKeyEquals() {
    final ReentrancyContextKey key = new ReentrancyContextKey("Actor1", "MyActor");
    final ReentrancyContextKey sameKey = new ReentrancyContextKey("Actor1", "MyActor");
    final ReentrancyContextKey diffKey = new ReentrancyContextKey("Actor2", "TheirActor");
    final ReentrancyContextKey diffIdKey = new ReentrancyContextKey("Actor2", "MyActor");
    final ReentrancyContextKey diffTypeKey = new ReentrancyContextKey("Actor1", "TheirActor");

    assertEquals(key, sameKey);
    assertNotEquals(key, diffKey);
    assertNotEquals(key, diffIdKey);
    assertNotEquals(key, diffTypeKey);
    assertNotEquals(key, 1);
    assertNotEquals(key, null);
  }

  @Test
  public void TestReentrancyContextKeyHashesConsistently() {
    final ReentrancyContextKey key = new ReentrancyContextKey("Actor1", "MyActor");
    final ReentrancyContextKey sameKey = new ReentrancyContextKey("Actor1", "MyActor");
    final ReentrancyContextKey diffKey = new ReentrancyContextKey("Actor2", "TheirActor");
    final ReentrancyContextKey diffIdKey = new ReentrancyContextKey("Actor2", "MyActor");
    final ReentrancyContextKey diffTypeKey = new ReentrancyContextKey("Actor1", "TheirActor");

    assertEquals(key.hashCode(), sameKey.hashCode());
    assertNotEquals(key.hashCode(), diffKey.hashCode());
    assertNotEquals(key.hashCode(), diffIdKey.hashCode());
    assertNotEquals(key.hashCode(), diffTypeKey.hashCode());
  }
}