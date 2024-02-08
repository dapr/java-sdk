/*
 * Copyright 2023 The Dapr Authors
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

package io.dapr.resiliency;

import io.dapr.internal.resiliency.RetryPolicy;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RetryPolicyTest {

  private static final String SUCCESS_MESSAGE = "It worked!";

  private static final RuntimeException RETRYABLE_EXCEPTION =
      new StatusRuntimeException(Status.DEADLINE_EXCEEDED);

  @Test
  public void zeroRetriesThenError() {
    AtomicInteger callCounter = new AtomicInteger();
    RetryPolicy policy = new RetryPolicy(0);
    Mono<String> action = createActionErrorAndReturn(callCounter, Integer.MAX_VALUE, RETRYABLE_EXCEPTION);

    StepVerifier
            .create(policy.apply(action))
            .expectError(StatusRuntimeException.class)
            .verify();
    assertEquals(1, callCounter.get());
  }

  @Test
  public void zeroRetriesThenSuccess() {
    AtomicInteger callCounter = new AtomicInteger();
    RetryPolicy policy = new RetryPolicy(0);
    Mono<String> action = createActionErrorAndReturn(callCounter, 0, RETRYABLE_EXCEPTION);

    StepVerifier
            .create(policy.apply(action))
            .expectNext(SUCCESS_MESSAGE)
            .expectComplete()
            .verify();
    assertEquals(1, callCounter.get());
  }

  @Test
  public void singleRetryPolicyWithSuccess() {
    AtomicInteger callCounter = new AtomicInteger();
    RetryPolicy policy = new RetryPolicy(1);
    Mono<String> action = createActionErrorAndReturn(callCounter, 0, RETRYABLE_EXCEPTION);

    StepVerifier
            .create(policy.apply(action))
            .expectNext(SUCCESS_MESSAGE)
            .expectComplete()
            .verify();
    assertEquals(1, callCounter.get());
  }

  @Test
  public void twoRetriesThenSuccess() {
    AtomicInteger callCounter = new AtomicInteger();
    RetryPolicy policy = new RetryPolicy(3);
    Mono<String> action = createActionErrorAndReturn(callCounter, 2, RETRYABLE_EXCEPTION);

    StepVerifier
            .create(policy.apply(action))
            .expectNext(SUCCESS_MESSAGE)
            .expectComplete()
            .verify();
    assertEquals(3, callCounter.get());
  }

  @Test
  public void threeRetriesThenError() {
    AtomicInteger callCounter = new AtomicInteger();
    RetryPolicy policy = new RetryPolicy(3);
    Mono<String> action = createActionErrorAndReturn(callCounter, Integer.MAX_VALUE, RETRYABLE_EXCEPTION);

    StepVerifier
            .create(policy.apply(action))
            .expectErrorMatches(e -> Exceptions.isRetryExhausted(e))
            .verify();

    assertEquals(4, callCounter.get());
  }

  @Test
  public void notRetryableException() {
    AtomicInteger callCounter = new AtomicInteger();
    RuntimeException exception = new ArithmeticException();
    RetryPolicy policy = new RetryPolicy(3);
    Mono<String> action = createActionErrorAndReturn(callCounter, Integer.MAX_VALUE, exception);

    StepVerifier
            .create(policy.apply(action))
            .expectError(ArithmeticException.class)
            .verify();

    assertEquals(1, callCounter.get());
  }

  private static Mono<String> createActionErrorAndReturn(
      AtomicInteger callCounter,
      int firstErrors,
      RuntimeException error) {
    return Mono.fromRunnable(() -> {
      if (callCounter.incrementAndGet() <= firstErrors) {
        throw error;
      }
    }).thenReturn(SUCCESS_MESSAGE);
  }
}
