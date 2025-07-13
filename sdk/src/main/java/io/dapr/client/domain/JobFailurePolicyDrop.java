package io.dapr.client.domain;

/**
 * A failure policy that drops the job upon failure without retrying.
 * <p>
 * This implementation of {@link FailurePolicy} immediately discards failed jobs
 * instead of retrying them.
 */
public class JobFailurePolicyDrop implements FailurePolicy {

  /**
   * Returns the type of failure policy.
   *
   * @return {@link FailurePolicyKind#DROP}
   */
  @Override
  public FailurePolicyKind getFailurePolicyKind() {
    return FailurePolicyKind.DROP;
  }
}
