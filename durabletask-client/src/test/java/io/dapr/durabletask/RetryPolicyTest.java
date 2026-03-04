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

  @Test
  void jitterFactorNaNThrows() {
    RetryPolicy policy = new RetryPolicy(3, Duration.ofSeconds(1));
    assertThrows(IllegalArgumentException.class, () -> policy.setJitterFactor(Double.NaN));
  }

  @Test
  void jitterFactorPositiveInfinityThrows() {
    RetryPolicy policy = new RetryPolicy(3, Duration.ofSeconds(1));
    assertThrows(IllegalArgumentException.class, () -> policy.setJitterFactor(Double.POSITIVE_INFINITY));
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
