package io.dapr.client.domain;

/**
 * Set a failure policy for the job.
 */
public interface FailurePolicy {
  FailurePolicyKind getFailurePolicyKind();
}
