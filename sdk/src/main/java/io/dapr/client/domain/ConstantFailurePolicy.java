/*
 * Copyright 2021 The Dapr Authors
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

package io.dapr.client.domain;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.time.Duration;

/**
 * A failure policy that applies a constant retry interval for job retries.
 * This implementation of {@link FailurePolicy} retries a job a fixed number of times
 * with a constant delay between each retry attempt.
 */
@JsonTypeName("CONSTANT")
public class ConstantFailurePolicy implements FailurePolicy {

  private Integer maxRetries;
  private Duration durationBetweenRetries;

  /**
   * Default constructor.
   */
  public ConstantFailurePolicy() {

  }

  /**
   * Constructs a {@code JobConstantFailurePolicy} with the specified maximum number of retries.
   *
   * @param maxRetries the maximum number of retries
   */
  public ConstantFailurePolicy(Integer maxRetries) {
    this.maxRetries = maxRetries;
  }

  /**
   * Constructs a {@code JobConstantFailurePolicy} with the specified duration between retries.
   *
   * @param durationBetweenRetries the duration to wait between retries
   */
  public ConstantFailurePolicy(Duration durationBetweenRetries) {
    this.durationBetweenRetries = durationBetweenRetries;
  }

  /**
   * Sets the duration to wait between retry attempts.
   *
   * @param durationBetweenRetries the duration between retries
   * @return a {@code JobFailurePolicyConstant}.
   */
  public ConstantFailurePolicy setDurationBetweenRetries(Duration durationBetweenRetries) {
    this.durationBetweenRetries = durationBetweenRetries;
    return this;
  }

  /**
   * Sets the maximum number of retries allowed.
   *
   * @param maxRetries the number of retries
   * @return a {@code JobFailurePolicyConstant}.
   */
  public ConstantFailurePolicy setMaxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
    return this;
  }

  /**
   * Returns the configured duration between retry attempts.
   *
   * @return the duration between retries
   */
  public Duration getDurationBetweenRetries() {
    return this.durationBetweenRetries;
  }

  /**
   * Returns the configured maximum number of retries.
   *
   * @return the maximum number of retries
   */
  public Integer getMaxRetries() {
    return this.maxRetries;
  }

  /**
   * Returns the type of failure policy.
   *
   * @return {@link FailurePolicyType#CONSTANT}
   */
  @Override
  public FailurePolicyType getFailurePolicyType() {
    return FailurePolicyType.CONSTANT;
  }
}
