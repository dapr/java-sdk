package io.dapr.actors.runtime.reentrancy;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ReentrancyStackTest {

  @Test
  public void testReentrancyStackReturnsCorrectInProgress() {
    final ReentrancyStack stack = new ReentrancyStack();

    assertFalse(stack.inProgress());
    stack.startOrIncreaseStack(null);
    assertTrue(stack.inProgress());
  }

  @Test
  public void testReentrancyStackStartsWithReentrancyId() {
    final ReentrancyStack stack = new ReentrancyStack();
    stack.startOrIncreaseStack("1");

    assertTrue(stack.isOpen("1"));
    assertFalse(stack.isOpen("2"));
    assertFalse(stack.isOpen(null));
  }

  @Test
  public void testReentrancyStackAllowsSameReentrantRequestToIncrement() {
    final ReentrancyStack stack = new ReentrancyStack();
    stack.startOrIncreaseStack("1");
    stack.startOrIncreaseStack("1");

    assertTrue(stack.isOpen("1"));
    assertFalse(stack.isOpen("2"));
  }

  @Test
  public void testReentrancyStackEndsWithReentrancyId() {
    final ReentrancyStack stack = new ReentrancyStack();
    stack.startOrIncreaseStack("1");

    assertTrue(stack.isOpen("1"));
    assertFalse(stack.isOpen("2"));
    assertFalse(stack.isOpen(null));

    stack.endOrDecrementStack("1");

    assertTrue(stack.isOpen("1"));
    assertTrue(stack.isOpen("2"));
    assertTrue(stack.isOpen(null));
  }

  @Test
  public void testReentrancyStackEndsWithReentrancyIdAfterAllRequestsFinished() {
    final ReentrancyStack stack = new ReentrancyStack();
    stack.startOrIncreaseStack("1");
    stack.startOrIncreaseStack("1");

    assertTrue(stack.isOpen("1"));
    assertFalse(stack.isOpen("2"));
    assertFalse(stack.isOpen(null));

    stack.endOrDecrementStack("1");

    assertTrue(stack.isOpen("1"));
    assertFalse(stack.isOpen("2"));
    assertFalse(stack.isOpen(null));

    stack.endOrDecrementStack("1");

    assertTrue(stack.isOpen("1"));
    assertTrue(stack.isOpen("2"));
    assertTrue(stack.isOpen(null));
  }

  @Test
  public void testReentrancyStackStartsWithNoReentrancyId() {
    final ReentrancyStack stack = new ReentrancyStack();
    stack.startOrIncreaseStack(null);

    assertFalse(stack.isOpen("1"));
    assertFalse(stack.isOpen(null));
  }

  @Test
  public void testReentrancyStackEndsWithNoReentrancyId() {
    final ReentrancyStack stack = new ReentrancyStack();
    stack.startOrIncreaseStack(null);

    assertFalse(stack.isOpen("1"));
    assertFalse(stack.isOpen(null));

    stack.endOrDecrementStack(null);

    assertTrue(stack.isOpen("1"));
    assertTrue(stack.isOpen(null));
  }

  @Test(expected = IllegalStateException.class)
  public void testReentrancyStackThrowsErrorIfReentrancyIdMismatches() {
    final ReentrancyStack stack = new ReentrancyStack();
    stack.startOrIncreaseStack("1");
    stack.startOrIncreaseStack("2");

    fail("Second request should not be allowed to start.");
  }

  @Test(expected = IllegalStateException.class)
  public void testReentrancyStackThrowsErrorIfReentrancyIdSetAndNullEncountered() {
    final ReentrancyStack stack = new ReentrancyStack();
    stack.startOrIncreaseStack("1");
    stack.startOrIncreaseStack(null);

    fail("Second request should not be allowed to start.");
  }

  @Test(expected = IllegalStateException.class)
  public void testReentrancyStackThrowsErrorIfReentrancyIdNullAndNewIdEncountered() {
    final ReentrancyStack stack = new ReentrancyStack();
    stack.startOrIncreaseStack(null);
    stack.startOrIncreaseStack("1");

    fail("Second request should not be allowed to start.");
  }

  @Test(expected = IllegalStateException.class)
  public void testReentrancyStackThrowsErrorIfNonReentrantRequestTriesToIncrement() {
    final ReentrancyStack stack = new ReentrancyStack();
    stack.startOrIncreaseStack(null);
    stack.startOrIncreaseStack(null);

    fail("Second request should not be allowed to start.");
  }

  @Test(expected = IllegalStateException.class)
  public void testReentrancyStackThrowsErrorIfReentrancyIdMismatchesWhileEnding() {
    final ReentrancyStack stack = new ReentrancyStack();
    stack.startOrIncreaseStack("1");
    stack.endOrDecrementStack("2");

    fail("Second request should not be allowed to end stack.");
  }

  @Test(expected = IllegalStateException.class)
  public void testReentrancyStackThrowsErrorIfReentrancyIdSetAndNullEncounteredWhileEnding() {
    final ReentrancyStack stack = new ReentrancyStack();
    stack.startOrIncreaseStack("1");
    stack.endOrDecrementStack(null);

    fail("Second request should not be allowed to end stack.");
  }

  @Test(expected = IllegalStateException.class)
  public void testReentrancyStackThrowsErrorIfReentrancyIdNullAndNewIdEncounteredWhileEnding() {
    final ReentrancyStack stack = new ReentrancyStack();
    stack.startOrIncreaseStack(null);
    stack.endOrDecrementStack("1");

    fail("Second request should not be allowed to end stack.");
  }

  @Test(expected = IllegalStateException.class)
  public void testReentrancyStackCannotBeDecrementedPastZero() {
    final ReentrancyStack stack = new ReentrancyStack();
    stack.endOrDecrementStack(null);

    fail("Should not be able to decrement past 0.");
  }
}