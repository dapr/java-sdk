package io.dapr.actors.runtime.reentrancy;

import org.junit.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ActorReentrancyContextTest {

  private static final String ACTOR_ID = "Actor";
  private static final String ACTOR_TYPE = "MyActor";

  @Test
  public void TestReentrancyContextReturnsEmptyIfNoContextSet() {
    final ActorReentrancyContext context = new ActorReentrancyContext();

    final Optional<String> reentrancyId = context.getReentrancyId(ACTOR_ID, ACTOR_TYPE);
    assertFalse(reentrancyId.isPresent());
  }

  @Test
  public void TestReentrancyContextReturnsIdAfterTracking() {
    final ActorReentrancyContext context = new ActorReentrancyContext();

    for (int i = 0; i < 10; i++) {
      context.trackReentrancy(ACTOR_ID + i, ACTOR_TYPE, String.valueOf(i));
    }

    for (int i = 0; i < 10; i++) {
      final Optional<String> reentrancyId = context.getReentrancyId(ACTOR_ID + i, ACTOR_TYPE);
      assertTrue(reentrancyId.isPresent());
      assertEquals(String.valueOf(i), reentrancyId.get());
    }
  }

  @Test
  public void TestReentrancyContextReleasesIds() {
    final ActorReentrancyContext context = new ActorReentrancyContext();
    final String fixedId = UUID.randomUUID().toString();

    final Optional<String> emptyId = context.getReentrancyId(ACTOR_ID, ACTOR_TYPE);
    assertFalse(emptyId.isPresent());

    context.trackReentrancy(ACTOR_ID, ACTOR_TYPE, fixedId);

    final Optional<String> reentrancyId = context.getReentrancyId(ACTOR_ID, ACTOR_TYPE);
    assertTrue(reentrancyId.isPresent());
    assertEquals(fixedId, reentrancyId.get());

    context.releaseReentrancy(ACTOR_ID, ACTOR_TYPE, fixedId);

    final Optional<String> releasedId = context.getReentrancyId(ACTOR_ID, ACTOR_TYPE);
    assertFalse(releasedId.isPresent());
  }

  @Test
  public void TestReentrancyContextDoesNotReleaseUntilFullyDecremented() {
    final ActorReentrancyContext context = new ActorReentrancyContext();
    final String fixedId = UUID.randomUUID().toString();

    context.trackReentrancy(ACTOR_ID, ACTOR_TYPE, fixedId);
    context.trackReentrancy(ACTOR_ID, ACTOR_TYPE, fixedId);

    final Optional<String> reentrancyId = context.getReentrancyId(ACTOR_ID, ACTOR_TYPE);
    assertTrue(reentrancyId.isPresent());
    assertEquals(fixedId, reentrancyId.get());

    context.releaseReentrancy(ACTOR_ID, ACTOR_TYPE, fixedId);

    final Optional<String> reentrancyId2 = context.getReentrancyId(ACTOR_ID, ACTOR_TYPE);
    assertTrue(reentrancyId2.isPresent());
    assertEquals(fixedId, reentrancyId2.get());

    context.releaseReentrancy(ACTOR_ID, ACTOR_TYPE, fixedId);

    final Optional<String> releasedId = context.getReentrancyId(ACTOR_ID, ACTOR_TYPE);
    assertFalse(releasedId.isPresent());
  }

  @Test(expected = IllegalStateException.class)
  public void TestReentrancyContextThrowsWithConflictingTrackingRequests() {
    final ActorReentrancyContext context = new ActorReentrancyContext();
    context.trackReentrancy(ACTOR_ID, ACTOR_TYPE, UUID.randomUUID().toString());
    context.trackReentrancy(ACTOR_ID, ACTOR_TYPE, UUID.randomUUID().toString());

    fail("Should have thrown an IllegalStateException");
  }

  @Test(expected = IllegalStateException.class)
  public void TestReentrancyContextThrowsWithConflictingReleaseRequests() {
    final ActorReentrancyContext context = new ActorReentrancyContext();
    context.trackReentrancy(ACTOR_ID, ACTOR_TYPE, UUID.randomUUID().toString());
    context.releaseReentrancy(ACTOR_ID, ACTOR_TYPE, UUID.randomUUID().toString());

    fail("Should have thrown an IllegalStateException");
  }
}