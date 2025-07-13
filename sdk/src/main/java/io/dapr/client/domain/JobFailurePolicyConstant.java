package io.dapr.client.domain;

import java.time.Duration;

/**
 * A failure policy that applies a constant retry interval for job retries.
 * <p>
 * This implementation of {@link FailurePolicy} retries a job a fixed number of times
 * with a constant delay between each retry attempt.
 */
public class JobFailurePolicyConstant implements FailurePolicy {

  private Integer maxRetries;
  private Duration durationBetweenRetries;

  /**
   * Constructs a {@code JobConstantFailurePolicy} with the specified maximum number of retries.
   *
   * @param maxRetries the maximum number of retries
   */
  public JobFailurePolicyConstant(Integer maxRetries) {
    this.maxRetries = maxRetries;
  }

  /**
   * Constructs a {@code JobConstantFailurePolicy} with the specified duration between retries.
   *
   * @param durationBetweenRetries the duration to wait between retries
   */
  public JobFailurePolicyConstant(Duration durationBetweenRetries) {
    this.durationBetweenRetries = durationBetweenRetries;
  }

  /**
   * Sets the duration to wait between retry attempts.
   *
   * @param durationBetweenRetries the duration between retries
   */
  public JobFailurePolicyConstant setDurationBetweenRetries(Duration durationBetweenRetries) {
    this.durationBetweenRetries = durationBetweenRetries;
    return this;
  }

  /**
   * Sets the maximum number of retries allowed.
   *
   * @param maxRetries the number of retries
   */
  public JobFailurePolicyConstant setMaxRetries(int maxRetries) {
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
   * @return {@link FailurePolicyKind#CONSTANT}
   */
  @Override
  public FailurePolicyKind getFailurePolicyKind() {
    return FailurePolicyKind.CONSTANT;
  }
}
