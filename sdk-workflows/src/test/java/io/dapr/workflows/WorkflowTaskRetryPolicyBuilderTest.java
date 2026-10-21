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

package io.dapr.workflows;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the argument checks on {@link WorkflowTaskRetryPolicy.Builder} and the null coercion the
 * policy applies, which decides whether the executor schedules a retry timer at all.
 */
public class WorkflowTaskRetryPolicyBuilderTest {

  private static final Duration FIRST_INTERVAL = Duration.ofSeconds(5);

  @Test
  public void shouldKeepEveryValueThatWasSet() {
    WorkflowTaskRetryPolicy policy = WorkflowTaskRetryPolicy.newBuilder()
        .setMaxNumberOfAttempts(4)
        .setFirstRetryInterval(FIRST_INTERVAL)
        .setBackoffCoefficient(2.0)
        .setMaxRetryInterval(Duration.ofMinutes(1))
        .setRetryTimeout(Duration.ofMinutes(10))
        .build();

    assertEquals(4, policy.getMaxNumberOfAttempts());
    assertEquals(FIRST_INTERVAL, policy.getFirstRetryInterval());
    assertEquals(2.0, policy.getBackoffCoefficient());
    assertEquals(Duration.ofMinutes(1), policy.getMaxRetryInterval());
    assertEquals(Duration.ofMinutes(10), policy.getRetryTimeout());
  }

  @Test
  public void shouldRejectAnAttemptCountThatIsNotPositive() {
    WorkflowTaskRetryPolicy.Builder builder = WorkflowTaskRetryPolicy.newBuilder();

    assertThrows(IllegalArgumentException.class, () -> builder.setMaxNumberOfAttempts(0));
    assertThrows(IllegalArgumentException.class, () -> builder.setMaxNumberOfAttempts(-1));
  }

  @Test
  public void shouldRejectAFirstIntervalThatIsMissingOrNotPositive() {
    WorkflowTaskRetryPolicy.Builder builder = WorkflowTaskRetryPolicy.newBuilder();

    assertThrows(IllegalArgumentException.class, () -> builder.setFirstRetryInterval(null));
    assertThrows(IllegalArgumentException.class, () -> builder.setFirstRetryInterval(Duration.ZERO));
    assertThrows(IllegalArgumentException.class, () -> builder.setFirstRetryInterval(Duration.ofSeconds(-1)));
  }

  @Test
  public void shouldRejectABackoffCoefficientBelowOne() {
    WorkflowTaskRetryPolicy.Builder builder = WorkflowTaskRetryPolicy.newBuilder();

    IllegalArgumentException thrown =
        assertThrows(IllegalArgumentException.class, () -> builder.setBackoffCoefficient(0.5));
    assertTrue(thrown.getMessage().contains("1.0"), thrown.getMessage());

    builder.setBackoffCoefficient(1.0);
  }

  @Test
  public void shouldRejectAMaxIntervalShorterThanTheFirstOne() {
    WorkflowTaskRetryPolicy.Builder builder = WorkflowTaskRetryPolicy.newBuilder()
        .setFirstRetryInterval(FIRST_INTERVAL);

    assertThrows(IllegalArgumentException.class, () -> builder.setMaxRetryInterval(Duration.ofSeconds(1)));

    builder.setMaxRetryInterval(FIRST_INTERVAL);
  }

  @Test
  public void shouldRejectARetryTimeoutThatIsMissingOrShorterThanTheFirstInterval() {
    WorkflowTaskRetryPolicy.Builder builder = WorkflowTaskRetryPolicy.newBuilder()
        .setFirstRetryInterval(FIRST_INTERVAL);

    assertThrows(IllegalArgumentException.class, () -> builder.setRetryTimeout(null));
    assertThrows(IllegalArgumentException.class, () -> builder.setRetryTimeout(Duration.ofSeconds(1)));
  }

  @Test
  public void shouldTurnUnsetIntervalsIntoZeroRatherThanNull() {
    WorkflowTaskRetryPolicy policy = WorkflowTaskRetryPolicy.newBuilder()
        .setMaxNumberOfAttempts(2)
        .setFirstRetryInterval(FIRST_INTERVAL)
        .build();

    assertEquals(Duration.ZERO, policy.getMaxRetryInterval(),
        "a null max interval reaching the executor would suppress the retry timer entirely");
    assertEquals(Duration.ZERO, policy.getRetryTimeout());
  }

  @Test
  public void shouldAllowClearingTheMaxIntervalBackToUnset() {
    WorkflowTaskRetryPolicy policy = WorkflowTaskRetryPolicy.newBuilder()
        .setMaxNumberOfAttempts(2)
        .setFirstRetryInterval(FIRST_INTERVAL)
        .setMaxRetryInterval(Duration.ofMinutes(1))
        .setMaxRetryInterval(null)
        .build();

    assertEquals(Duration.ZERO, policy.getMaxRetryInterval());
  }
}
