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

package io.dapr.internal.resiliency;

import io.dapr.config.Properties;
import io.dapr.exceptions.DaprException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Retry policy for SDK communication to Dapr API.
 */
public final class RetryPolicy {

  private static final int MIN_BACKOFF_MILLIS = 500;

  private static final int MAX_BACKOFF_SECONDS = 5;

  private final Retry retrySpec;

  public RetryPolicy() {
    this(null);
  }

  public RetryPolicy(Integer maxRetries) {
    this.retrySpec = buildRetrySpec(maxRetries != null ? maxRetries : Properties.MAX_RETRIES.get());
  }

  /**
   * Applies the retry policy to an expected Mono action.
   * @param response Response
   * @param <T> Type expected for the action's response
   * @return action with retry
   */
  public <T> Mono<T> apply(Mono<T> response) {
    if (this.retrySpec == null) {
      return response;
    }

    return response.retryWhen(retrySpec)
        .onErrorMap(throwable -> findDaprException(throwable));
  }

  /**
   * Applies the retry policy to an expected Flux action.
   * @param response Response
   * @param <T> Type expected for the action's response
   * @return action with retry
   */
  public <T> Flux<T> apply(Flux<T> response) {
    if (this.retrySpec == null) {
      return response;
    }

    return response.retryWhen(retrySpec)
        .onErrorMap(throwable -> findDaprException(throwable));
  }

  private static Retry buildRetrySpec(int maxRetries) {
    if (maxRetries == 0) {
      return null;
    }

    if (maxRetries < 0) {
      return Retry.indefinitely()
          .filter(throwable -> isRetryableGrpcError(throwable));
    }

    return Retry.backoff(maxRetries, Duration.ofMillis(MIN_BACKOFF_MILLIS))
        .maxBackoff(Duration.ofSeconds(MAX_BACKOFF_SECONDS))
        .filter(throwable -> isRetryableGrpcError(throwable));
  }

  private static boolean isRetryableGrpcError(Throwable throwable) {
    Status grpcStatus = findGrpcStatusCode(throwable);
    if (grpcStatus == null) {
      return false;
    }

    switch (grpcStatus.getCode()) {
      case DEADLINE_EXCEEDED:
      case UNAVAILABLE:
        return true;
      default:
        return false;
    }
  }

  private static Status findGrpcStatusCode(Throwable throwable) {
    while (throwable != null) {
      if (throwable instanceof StatusRuntimeException) {
        return ((StatusRuntimeException) throwable).getStatus();
      }

      throwable = throwable.getCause();
    }
    return null;
  }

  private static Throwable findDaprException(Throwable throwable) {
    Throwable original = throwable;
    while (throwable != null) {
      if (throwable instanceof DaprException) {
        return throwable;
      }

      throwable = throwable.getCause();
    }
    return original;
  }
}
