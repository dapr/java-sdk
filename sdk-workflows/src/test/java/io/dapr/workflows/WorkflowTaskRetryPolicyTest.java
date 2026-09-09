/*
 * Copyright 2026 The Dapr Authors
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

/**
 * Builder validation carried over from DefaultWorkflowContextTest, which was deleted along with
 * the DefaultWorkflowContext adapter. These assertions were never about the adapter; they test
 * WorkflowTaskRetryPolicy itself.
 */
public class WorkflowTaskRetryPolicyTest {

  @Test
  public void retryTimeoutIsRetained() {
    WorkflowTaskRetryPolicy policy = WorkflowTaskRetryPolicy.newBuilder()
        .setMaxNumberOfAttempts(1)
        .setFirstRetryInterval(Duration.ofSeconds(10))
        .setRetryTimeout(Duration.ofSeconds(10))
        .build();

    assertEquals(Duration.ofSeconds(10), policy.getRetryTimeout());
  }

  @Test
  public void nullRetryTimeoutIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> WorkflowTaskRetryPolicy.newBuilder()
        .setMaxNumberOfAttempts(1)
        .setFirstRetryInterval(Duration.ofSeconds(10))
        .setRetryTimeout(null)
        .build());
  }

  @Test
  public void retryTimeoutBelowFirstRetryIntervalIsRejectedByTheBuilder() {
    assertThrows(IllegalArgumentException.class, () -> WorkflowTaskRetryPolicy.newBuilder()
        .setMaxNumberOfAttempts(1)
        .setFirstRetryInterval(Duration.ofSeconds(10))
        .setRetryTimeout(Duration.ofSeconds(9))
        .build());
  }

  /**
   * The durable task client's RetryPolicy validated in its setters, and the deleted adapter routed
   * every policy through them. These pin that validation to the constructors, so an invalid policy
   * still fails fast at construction instead of NPE-ing part-way through a replay or silently
   * producing wrong retry timing.
   */
  @Test
  public void nonPositiveMaxNumberOfAttemptsIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new WorkflowTaskRetryPolicy(0, Duration.ofSeconds(1)));
    assertThrows(IllegalArgumentException.class, () -> new WorkflowTaskRetryPolicy(-1, Duration.ofSeconds(1)));
  }

  @Test
  public void nullMaxNumberOfAttemptsIsRejected() {
    assertThrows(IllegalArgumentException.class,
        () -> new WorkflowTaskRetryPolicy(null, Duration.ofSeconds(1), 1.0, null, null));
  }

  @Test
  public void nullOrNonPositiveFirstRetryIntervalIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new WorkflowTaskRetryPolicy(1, null));
    assertThrows(IllegalArgumentException.class, () -> new WorkflowTaskRetryPolicy(1, Duration.ZERO));
    assertThrows(IllegalArgumentException.class, () -> new WorkflowTaskRetryPolicy(1, Duration.ofSeconds(-1)));
  }

  @Test
  public void backoffCoefficientBelowOneIsRejected() {
    assertThrows(IllegalArgumentException.class,
        () -> new WorkflowTaskRetryPolicy(1, Duration.ofSeconds(1), 0.5, null, null));
    assertThrows(IllegalArgumentException.class,
        () -> new WorkflowTaskRetryPolicy(1, Duration.ofSeconds(1), null, null, null));
  }

  @Test
  public void maxRetryIntervalBelowFirstRetryIntervalIsRejected() {
    assertThrows(IllegalArgumentException.class,
        () -> new WorkflowTaskRetryPolicy(1, Duration.ofSeconds(10), 1.0, Duration.ofSeconds(9), null));
  }

  @Test
  public void retryTimeoutBelowFirstRetryIntervalIsRejectedByTheConstructor() {
    assertThrows(IllegalArgumentException.class,
        () -> new WorkflowTaskRetryPolicy(1, Duration.ofSeconds(10), 1.0, null, Duration.ofSeconds(9)));
  }

  /**
   * Only null means "unset". An explicitly supplied ZERO is a real value and v1 rejected it, since
   * ZERO is always below a valid firstRetryInterval. Accepting it here would silently uncap retries
   * and would disagree with the Builder, which rejects it.
   */
  @Test
  public void anExplicitZeroOptionalDurationIsRejected() {
    assertThrows(IllegalArgumentException.class,
        () -> new WorkflowTaskRetryPolicy(1, Duration.ofSeconds(10), 1.0, Duration.ZERO, null));
    assertThrows(IllegalArgumentException.class,
        () -> new WorkflowTaskRetryPolicy(1, Duration.ofSeconds(10), 1.0, null, Duration.ZERO));
  }

  /**
   * Null is the "unset" signal and is coerced to ZERO, which is what the executor reads as
   * "no maximum interval" / "no overall timeout".
   */
  @Test
  public void nullOptionalDurationsMeanUnsetAndBecomeZero() {
    WorkflowTaskRetryPolicy policy =
        new WorkflowTaskRetryPolicy(1, Duration.ofSeconds(10), 1.0, null, null);

    assertEquals(Duration.ZERO, policy.getMaxRetryInterval());
    assertEquals(Duration.ZERO, policy.getRetryTimeout());
  }

  /**
   * The two-argument constructor replaces the durable task client's RetryPolicy(int, Duration),
   * whose maxRetryInterval and retryTimeout defaulted to ZERO. A null here reaches the executor
   * and suppresses the retry timer, so the defaults must match exactly.
   */
  @Test
  public void twoArgConstructorMatchesTheDurableTaskDefaults() {
    WorkflowTaskRetryPolicy policy = new WorkflowTaskRetryPolicy(3, Duration.ofSeconds(5));

    assertEquals(3, policy.getMaxNumberOfAttempts());
    assertEquals(Duration.ofSeconds(5), policy.getFirstRetryInterval());
    assertEquals(1.0, policy.getBackoffCoefficient());
    assertEquals(Duration.ZERO, policy.getMaxRetryInterval());
    assertEquals(Duration.ZERO, policy.getRetryTimeout());
  }
}
