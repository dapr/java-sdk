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

package io.dapr.workflows.task.exception;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompositeTaskFailedExceptionTest {

  private static final Exception FIRST = new IllegalStateException("first task failed");
  private static final Exception SECOND = new IllegalArgumentException("second task failed");

  @Test
  public void shouldHoldNoExceptionsWhenBuiltEmpty() {
    CompositeTaskFailedException exception = new CompositeTaskFailedException();

    assertTrue(exception.getExceptions().isEmpty());
    assertNull(exception.getMessage());
  }

  @Test
  public void shouldHoldTheTaskFailuresItWasBuiltWith() {
    CompositeTaskFailedException exception =
        new CompositeTaskFailedException(Arrays.asList(FIRST, SECOND));

    assertEquals(Arrays.asList(FIRST, SECOND), exception.getExceptions());
  }

  @Test
  public void shouldKeepTheMessageAlongsideTheTaskFailures() {
    CompositeTaskFailedException exception =
        new CompositeTaskFailedException("2 tasks failed", Arrays.asList(FIRST, SECOND));

    assertEquals("2 tasks failed", exception.getMessage());
    assertEquals(2, exception.getExceptions().size());
  }

  @Test
  public void shouldKeepTheCause() {
    CompositeTaskFailedException fromMessageAndCause =
        new CompositeTaskFailedException("2 tasks failed", FIRST, Arrays.asList(FIRST, SECOND));

    assertEquals("2 tasks failed", fromMessageAndCause.getMessage());
    assertSame(FIRST, fromMessageAndCause.getCause());

    CompositeTaskFailedException fromCauseOnly =
        new CompositeTaskFailedException(FIRST, Arrays.asList(FIRST, SECOND));

    assertSame(FIRST, fromCauseOnly.getCause());
    assertEquals(2, fromCauseOnly.getExceptions().size());
  }

  @Test
  public void shouldHonourSuppressionAndWritableStackTraceFlags() {
    CompositeTaskFailedException exception = new CompositeTaskFailedException(
        "2 tasks failed", FIRST, false, false, Arrays.asList(FIRST, SECOND));

    exception.addSuppressed(SECOND);

    assertEquals(0, exception.getSuppressed().length, "suppression was disabled");
    assertEquals(0, exception.getStackTrace().length, "the stack trace was not writable");
    assertEquals(2, exception.getExceptions().size());
  }

  @Test
  public void shouldReturnACopySoCallersCannotMutateTheFailureList() {
    List<Exception> original = new ArrayList<>(Arrays.asList(FIRST, SECOND));
    CompositeTaskFailedException exception = new CompositeTaskFailedException("2 tasks failed", original);

    List<Exception> returned = exception.getExceptions();
    returned.clear();

    assertNotSame(original, returned);
    assertEquals(2, exception.getExceptions().size(), "clearing the returned list must not empty the exception");
  }
}
