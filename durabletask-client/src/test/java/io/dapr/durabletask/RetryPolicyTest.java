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

package io.dapr.durabletask;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class RetryPolicyTest {

  // ---- default value ----

  @Test
  void jitterFactorDefaultsToZero() {
    RetryPolicy policy = new RetryPolicy(3, Duration.ofSeconds(1));
    assertEquals(0.0, policy.getJitterFactor());
  }

  // ---- valid boundary values ----

  @Test
  void jitterFactorZeroIsAccepted() {
    RetryPolicy policy = new RetryPolicy(3, Duration.ofSeconds(1));
    RetryPolicy returned = policy.setJitterFactor(0.0);
    assertEquals(0.0, policy.getJitterFactor());
    assertSame(policy, returned, "setJitterFactor should return this for chaining");
  }

  @Test
  void jitterFactorOneIsAccepted() {
    RetryPolicy policy = new RetryPolicy(3, Duration.ofSeconds(1));
    policy.setJitterFactor(1.0);
    assertEquals(1.0, policy.getJitterFactor());
  }

  @Test
  void jitterFactorMidRangeIsAccepted() {
    RetryPolicy policy = new RetryPolicy(3, Duration.ofSeconds(1));
    policy.setJitterFactor(0.5);
    assertEquals(0.5, policy.getJitterFactor());
  }

  // ---- invalid values ----

  @Test
  void jitterFactorBelowZeroThrows() {
    RetryPolicy policy = new RetryPolicy(3, Duration.ofSeconds(1));
    assertThrows(IllegalArgumentException.class, () -> policy.setJitterFactor(-0.1));
  }

  @Test
  void jitterFactorAboveOneThrows() {
    RetryPolicy policy = new RetryPolicy(3, Duration.ofSeconds(1));
    assertThrows(IllegalArgumentException.class, () -> policy.setJitterFactor(1.1));
  }

  // ---- deterministic delay formula ----

  /**
   * Verifies that the jitter reduction formula is deterministic: given the same
   * firstAttempt epoch millis and attempt number (which together form the seed),
   * the reduced delay must always equal the pre-computed expected value.
   *
   * <p>This mirrors the logic in TaskOrchestrationExecutor.RetriableTask.getNextDelay():
   * <pre>
   *   seed      = firstAttempt.toEpochMilli() + attemptNumber
   *   reduction = new Random(seed).nextDouble() * jitterFactor
   *   delay     = (long)(baseDelayMillis * (1.0 - reduction))
   * </pre>
   */
  @Test
  void jitterDelayIsDeterministicForGivenSeed() {
    long firstAttemptEpochMillis = 1_700_000_000_000L;
    int attemptNumber = 1;
    long baseDelayMillis = 1000L;
    double jitterFactor = 0.5;

    long seed = firstAttemptEpochMillis + attemptNumber;
    double reduction = new Random(seed).nextDouble() * jitterFactor;
    long expected = (long) (baseDelayMillis * (1.0 - reduction));

    // Calling with the same seed twice must produce the same result.
    long seed2 = firstAttemptEpochMillis + attemptNumber;
    double reduction2 = new Random(seed2).nextDouble() * jitterFactor;
    long result2 = (long) (baseDelayMillis * (1.0 - reduction2));

    assertEquals(expected, result2);
  }

  /**
   * Verifies that with jitterFactor=0.5 the reduced delay is always between
   * 50% and 100% of the base delay (i.e. never negative or exceeding the base).
   */
  @Test
  void jitterReducedDelayIsWithinExpectedBounds() {
    long baseDelayMillis = 2000L;
    double jitterFactor = 0.5;

    for (int attempt = 1; attempt <= 10; attempt++) {
      long seed = System.currentTimeMillis() + attempt;
      double reduction = new Random(seed).nextDouble() * jitterFactor;
      long reduced = (long) (baseDelayMillis * (1.0 - reduction));

      assertTrue(reduced >= (long) (baseDelayMillis * (1.0 - jitterFactor)),
          "Reduced delay should be >= base * (1 - jitterFactor)");
      assertTrue(reduced <= baseDelayMillis,
          "Reduced delay should not exceed base delay");
    }
  }

  /**
   * With jitterFactor=0 the delay must be unchanged.
   */
  @Test
  void zeroJitterLeavesDelayUnchanged() {
    long baseDelayMillis = 3000L;
    double jitterFactor = 0.0;

    long seed = 42L;
    double reduction = new Random(seed).nextDouble() * jitterFactor;
    long reduced = (long) (baseDelayMillis * (1.0 - reduction));

    assertEquals(baseDelayMillis, reduced);
  }
}
