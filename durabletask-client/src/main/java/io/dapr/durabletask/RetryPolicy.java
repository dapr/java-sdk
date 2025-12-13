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

package io.dapr.durabletask;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Objects;

/**
 * A declarative retry policy that can be configured for activity or sub-orchestration calls.
 */
public final class RetryPolicy {

  private int maxNumberOfAttempts;
  private Duration firstRetryInterval;
  private double backoffCoefficient = 1.0;
  private Duration maxRetryInterval = Duration.ZERO;
  private Duration retryTimeout = Duration.ZERO;

  /**
   * Creates a new {@code RetryPolicy} object.
   *
   * @param maxNumberOfAttempts the maximum number of task invocation attempts; must be 1 or greater
   * @param firstRetryInterval  the amount of time to delay between the first and second attempt
   * @throws IllegalArgumentException if {@code maxNumberOfAttempts} is zero or negative
   */
  public RetryPolicy(int maxNumberOfAttempts, Duration firstRetryInterval) {
    this.setMaxNumberOfAttempts(maxNumberOfAttempts);
    this.setFirstRetryInterval(firstRetryInterval);
  }

  /**
   * Sets the maximum number of task invocation attempts; must be 1 or greater.
   *
   * <p>This value represents the number of times to attempt to execute the task. It does <em>not</em> represent
   * the maximum number of times to <em>retry</em> the task. This is why the number must be 1 or greater.</p>
   *
   * @param maxNumberOfAttempts the maximum number of attempts; must be 1 or greater
   * @return this retry policy object
   * @throws IllegalArgumentException if {@code maxNumberOfAttempts} is zero or negative
   */
  public RetryPolicy setMaxNumberOfAttempts(int maxNumberOfAttempts) {
    if (maxNumberOfAttempts <= 0) {
      throw new IllegalArgumentException("The value for maxNumberOfAttempts must be greater than zero.");
    }
    this.maxNumberOfAttempts = maxNumberOfAttempts;
    return this;
  }

  /**
   * Sets the amount of time to delay between the first and second attempt.
   *
   * @param firstRetryInterval the amount of time to delay between the first and second attempt
   * @return this retry policy object
   * @throws IllegalArgumentException if {@code firstRetryInterval} is {@code null}, zero, or negative.
   */
  public RetryPolicy setFirstRetryInterval(Duration firstRetryInterval) {
    if (firstRetryInterval == null) {
      throw new IllegalArgumentException("firstRetryInterval cannot be null.");
    }
    if (firstRetryInterval.isZero() || firstRetryInterval.isNegative()) {
      throw new IllegalArgumentException("The value for firstRetryInterval must be greater than zero.");
    }
    this.firstRetryInterval = firstRetryInterval;
    return this;
  }

  /**
   * Sets the exponential backoff coefficient used to determine the delay between subsequent retries.
   * Must be 1.0 or greater.
   *
   * <p>To avoid extremely long delays between retries, consider also specifying a maximum retry interval using the
   * {@link #setMaxRetryInterval} method.</p>
   *
   * @param backoffCoefficient the exponential backoff coefficient
   * @return this retry policy object
   * @throws IllegalArgumentException if {@code backoffCoefficient} is less than 1.0
   */
  public RetryPolicy setBackoffCoefficient(double backoffCoefficient) {
    if (backoffCoefficient < 1.0) {
      throw new IllegalArgumentException("The value for backoffCoefficient must be greater or equal to 1.0.");
    }
    this.backoffCoefficient = backoffCoefficient;
    return this;
  }

  /**
   * Sets the maximum time to delay between attempts.
   *
   * <p>It's recommended to set a maximum retry interval whenever using a backoff coefficient that's greater than the
   * default of 1.0.</p>
   *
   * @param maxRetryInterval the maximum time to delay between attempts or {@code null} to remove the maximum retry
   *                         interval
   * @return this retry policy object
   */
  public RetryPolicy setMaxRetryInterval(@Nullable Duration maxRetryInterval) {
    if (maxRetryInterval != null && maxRetryInterval.compareTo(this.firstRetryInterval) < 0) {
      throw new IllegalArgumentException("The value for maxRetryInterval must be greater than or equal to the value "
          + "for firstRetryInterval.");
    }
    this.maxRetryInterval = maxRetryInterval;
    return this;
  }

  /**
   * Sets the overall timeout for retries, regardless of the retry count.
   *
   * @param retryTimeout the overall timeout for retries
   * @return this retry policy object
   */
  public RetryPolicy setRetryTimeout(Duration retryTimeout) {
    if (retryTimeout == null || retryTimeout.compareTo(this.firstRetryInterval) < 0) {
      throw new IllegalArgumentException("The value for retryTimeout cannot be null and must be greater than or equal "
          + "to the value for firstRetryInterval.");
    }
    this.retryTimeout = retryTimeout;
    return this;
  }

  /**
   * Gets the configured maximum number of task invocation attempts.
   *
   * @return the configured maximum number of task invocation attempts.
   */
  public int getMaxNumberOfAttempts() {
    return this.maxNumberOfAttempts;
  }

  /**
   * Gets the configured amount of time to delay between the first and second attempt.
   *
   * @return the configured amount of time to delay between the first and second attempt
   */
  public Duration getFirstRetryInterval() {
    return this.firstRetryInterval;
  }

  /**
   * Gets the configured exponential backoff coefficient used to determine the delay between subsequent retries.
   *
   * @return the configured exponential backoff coefficient used to determine the delay between subsequent retries
   */
  public double getBackoffCoefficient() {
    return this.backoffCoefficient;
  }

  /**
   * Gets the configured maximum time to delay between attempts.
   *
   * @return the configured maximum time to delay between attempts
   */
  public Duration getMaxRetryInterval() {
    return this.maxRetryInterval;
  }

  /**
   * Gets the configured overall timeout for retries.
   *
   * @return the configured overall timeout for retries
   */
  public Duration getRetryTimeout() {
    return this.retryTimeout;
  }
}
