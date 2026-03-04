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
 * limitations under the License.
*/

package io.dapr.workflows;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class WorkflowTaskRetryPolicyTest {

  // ---- default value ----

  @Test
  void jitterFactorDefaultsToZero() {
    WorkflowTaskRetryPolicy policy = WorkflowTaskRetryPolicy.newBuilder().build();
    assertEquals(0.0, policy.getJitterFactor());
  }

  /**
   * When the policy is constructed via the all-args constructor with a null
   * jitterFactor (e.g. deserialisation path), getJitterFactor() must still
   * return 0.0 rather than throw a NullPointerException.
   */
  @Test
  void jitterFactorNullInConstructorReturnsZero() {
    WorkflowTaskRetryPolicy policy = new WorkflowTaskRetryPolicy(
        3,
        Duration.ofSeconds(1),
        1.0,
        null,
        null,
        null   // jitterFactor = null
    );
    assertEquals(0.0, policy.getJitterFactor());
  }

  // ---- valid boundary values ----

  @Test
  void jitterFactorZeroIsAccepted() {
    WorkflowTaskRetryPolicy policy = WorkflowTaskRetryPolicy.newBuilder()
        .setJitterFactor(0.0)
        .build();
    assertEquals(0.0, policy.getJitterFactor());
  }

  @Test
  void jitterFactorOneIsAccepted() {
    WorkflowTaskRetryPolicy policy = WorkflowTaskRetryPolicy.newBuilder()
        .setJitterFactor(1.0)
        .build();
    assertEquals(1.0, policy.getJitterFactor());
  }

  @Test
  void jitterFactorMidRangeIsAccepted() {
    WorkflowTaskRetryPolicy policy = WorkflowTaskRetryPolicy.newBuilder()
        .setJitterFactor(0.3)
        .build();
    assertEquals(0.3, policy.getJitterFactor());
  }

  // ---- invalid values ----

  @Test
  void jitterFactorBelowZeroThrows() {
    WorkflowTaskRetryPolicy.Builder builder = WorkflowTaskRetryPolicy.newBuilder();
    assertThrows(IllegalArgumentException.class, () -> builder.setJitterFactor(-0.1));
  }

  @Test
  void jitterFactorAboveOneThrows() {
    WorkflowTaskRetryPolicy.Builder builder = WorkflowTaskRetryPolicy.newBuilder();
    assertThrows(IllegalArgumentException.class, () -> builder.setJitterFactor(1.1));
  }

  // ---- builder chaining ----

  @Test
  void builderReturnsItself() {
    WorkflowTaskRetryPolicy.Builder builder = WorkflowTaskRetryPolicy.newBuilder();
    assertSame(builder, builder.setJitterFactor(0.5));
  }

  // ---- coexistence with other fields ----

  @Test
  void jitterFactorDoesNotAffectOtherFields() {
    WorkflowTaskRetryPolicy policy = WorkflowTaskRetryPolicy.newBuilder()
        .setMaxNumberOfAttempts(5)
        .setFirstRetryInterval(Duration.ofSeconds(2))
        .setBackoffCoefficient(2.0)
        .setJitterFactor(0.25)
        .build();

    assertEquals(5, policy.getMaxNumberOfAttempts());
    assertEquals(Duration.ofSeconds(2), policy.getFirstRetryInterval());
    assertEquals(2.0, policy.getBackoffCoefficient());
    assertEquals(0.25, policy.getJitterFactor());
  }
}
