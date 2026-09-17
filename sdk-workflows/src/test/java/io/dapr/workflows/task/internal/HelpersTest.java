/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.workflows.task.internal;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HelpersTest {

  @Test
  public void shouldReturnTheArgumentWhenItIsNotNull() {
    String argument = "value";

    assertSame(argument, Helpers.throwIfArgumentNull(argument, "name"));
  }

  @Test
  public void shouldNameTheNullArgumentItRejects() {
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
        () -> Helpers.throwIfArgumentNull(null, "instanceId"));

    assertEquals("The argument 'instanceId' was null.", thrown.getMessage());
  }

  @Test
  public void shouldReturnTheArgumentWhenItHasContent() {
    assertEquals("value", Helpers.throwIfArgumentNullOrWhiteSpace("value", "name"));
  }

  @Test
  public void shouldRejectNullEmptyAndWhitespaceOnlyArguments() {
    assertThrows(IllegalArgumentException.class,
        () -> Helpers.throwIfArgumentNullOrWhiteSpace(null, "name"));

    IllegalArgumentException empty = assertThrows(IllegalArgumentException.class,
        () -> Helpers.throwIfArgumentNullOrWhiteSpace("", "name"));
    assertEquals("The argument 'name' was empty or contained only whitespace.", empty.getMessage());

    assertThrows(IllegalArgumentException.class,
        () -> Helpers.throwIfArgumentNullOrWhiteSpace("   ", "name"));
  }

  @Test
  public void shouldOnlyComplainAboutACompletedOrchestrator() {
    Helpers.throwIfOrchestratorComplete(false);

    IllegalStateException thrown = assertThrows(IllegalStateException.class,
        () -> Helpers.throwIfOrchestratorComplete(true));
    assertEquals("The orchestrator has already completed", thrown.getMessage());
  }

  @Test
  public void shouldTreatNullNegativeAndMaxDurationAsWaitingForever() {
    assertTrue(Helpers.isInfiniteTimeout(null));
    assertTrue(Helpers.isInfiniteTimeout(Duration.ofSeconds(-1)));
    assertTrue(Helpers.isInfiniteTimeout(Helpers.maxDuration));

    assertFalse(Helpers.isInfiniteTimeout(Duration.ZERO));
    assertFalse(Helpers.isInfiniteTimeout(Duration.ofSeconds(30)));
  }

  @Test
  public void shouldRaiseToThePowerOfForOrdinaryValues() {
    assertEquals(8.0, Helpers.powExact(2.0, 3.0));
    assertEquals(1.0, Helpers.powExact(5.0, 0.0));
    assertEquals(0.0, Helpers.powExact(0.0, 3.0), "zero short-circuits before Math.pow");
  }

  @Test
  public void shouldFailLoudlyInsteadOfReturningInfinity() {
    ArithmeticException positive = assertThrows(ArithmeticException.class,
        () -> Helpers.powExact(Double.MAX_VALUE, 2.0));
    assertTrue(positive.getMessage().contains("POSITIVE_INFINITY"), positive.getMessage());

    ArithmeticException negative = assertThrows(ArithmeticException.class,
        () -> Helpers.powExact(-Double.MAX_VALUE, 3.0));
    assertTrue(negative.getMessage().contains("NEGATIVE_INFINITY"), negative.getMessage());
  }

  @Test
  public void shouldFailLoudlyInsteadOfUnderflowingToZero() {
    ArithmeticException thrown = assertThrows(ArithmeticException.class,
        () -> Helpers.powExact(Double.MIN_VALUE, 2.0));

    assertTrue(thrown.getMessage().contains("zero"), thrown.getMessage());
  }

  @Test
  public void shouldTreatNullAndEmptyStringsAsEmpty() {
    assertTrue(Helpers.isNullOrEmpty(null));
    assertTrue(Helpers.isNullOrEmpty(""));

    assertFalse(Helpers.isNullOrEmpty(" "));
    assertFalse(Helpers.isNullOrEmpty("value"));
  }
}
