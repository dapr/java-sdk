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

import javax.annotation.Nullable;

import java.time.Duration;

public final class WorkflowTaskRetryPolicy {

  private final Integer maxNumberOfAttempts;
  private final Duration firstRetryInterval;
  private final Double backoffCoefficient;
  private final Duration maxRetryInterval;
  private final Duration retryTimeout;

  /**
   * Constructor for WorkflowTaskRetryPolicy.
   * @param maxNumberOfAttempts Maximum number of attempts to retry the workflow.
   * @param firstRetryInterval Interval to wait before the first retry.
   * @param backoffCoefficient Coefficient to increase the retry interval.
   * @param maxRetryInterval Maximum interval to wait between retries.
   * @param retryTimeout Timeout for the whole retry process.
   */
  public WorkflowTaskRetryPolicy(
      Integer maxNumberOfAttempts,
      Duration firstRetryInterval,
      Double backoffCoefficient,
      Duration maxRetryInterval,
      Duration retryTimeout
  ) {
    this.maxNumberOfAttempts = maxNumberOfAttempts;
    this.firstRetryInterval = firstRetryInterval;
    this.backoffCoefficient = backoffCoefficient;
    this.maxRetryInterval = maxRetryInterval;
    this.retryTimeout = retryTimeout;
  }

  public int getMaxNumberOfAttempts() {
    return maxNumberOfAttempts;
  }

  public Duration getFirstRetryInterval() {
    return firstRetryInterval;
  }

  public double getBackoffCoefficient() {
    return backoffCoefficient;
  }

  public Duration getMaxRetryInterval() {
    return maxRetryInterval;
  }

  public Duration getRetryTimeout() {
    return retryTimeout;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private Integer maxNumberOfAttempts;
    private Duration firstRetryInterval;
    private Double backoffCoefficient = 1.0;
    private Duration maxRetryInterval;
    private Duration retryTimeout;

    private Builder() {
    }

    /**
     * Build the WorkflowTaskRetryPolicy.
     * @return WorkflowTaskRetryPolicy
     */
    public WorkflowTaskRetryPolicy build() {
      return new WorkflowTaskRetryPolicy(
          this.maxNumberOfAttempts,
          this.firstRetryInterval,
          this.backoffCoefficient,
          this.maxRetryInterval,
          this.retryTimeout
      );
    }

    /**
     * Set the maximum number of attempts to retry the workflow.
     * @param maxNumberOfAttempts Maximum number
     * @return This builder
     */
    public Builder setMaxNumberOfAttempts(int maxNumberOfAttempts) {
      if (maxNumberOfAttempts <= 0) {
        throw new IllegalArgumentException("The value for maxNumberOfAttempts must be greater than zero.");
      }

      this.maxNumberOfAttempts = maxNumberOfAttempts;

      return this;
    }

    /**
     * Set the interval to wait before the first retry.
     * @param firstRetryInterval Interval
     * @return This builder
     */
    public Builder setFirstRetryInterval(Duration firstRetryInterval) {
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
     * Set the backoff coefficient.
     * @param backoffCoefficient Double value
     * @return This builder
     */
    public Builder setBackoffCoefficient(double backoffCoefficient) {
      if (backoffCoefficient < 1.0) {
        throw new IllegalArgumentException("The value for backoffCoefficient must be greater or equal to 1.0.");
      }

      this.backoffCoefficient = backoffCoefficient;

      return this;
    }

    /**
     * Set the maximum interval to wait between retries.
     * @param maxRetryInterval Maximum interval
     * @return This builder
     */
    public Builder setMaxRetryInterval(@Nullable Duration maxRetryInterval) {
      if (maxRetryInterval != null && maxRetryInterval.compareTo(this.firstRetryInterval) < 0) {
        throw new IllegalArgumentException(
            "The value for maxRetryInterval must be greater than or equal to the value for firstRetryInterval.");
      }

      this.maxRetryInterval = maxRetryInterval;

      return this;
    }

    /**
     * Set the maximum retry timeout.
     * @param retryTimeout Maximum retry timeout
     * @return This builder
     */
    public Builder setRetryTimeout(Duration retryTimeout) {
      if (retryTimeout == null || retryTimeout.compareTo(this.firstRetryInterval) < 0) {
        throw new IllegalArgumentException(
            "The value for retryTimeout cannot be null and"
                    + " must be greater than or equal to the value for firstRetryInterval.");
      }

      this.retryTimeout = retryTimeout;

      return this;
    }
  }

}
