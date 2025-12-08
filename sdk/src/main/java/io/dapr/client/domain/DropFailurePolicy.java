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

/**
 * A failure policy that drops the job upon failure without retrying.
 * This implementation of {@link FailurePolicy} immediately discards failed jobs
 * instead of retrying them.
 */
public class DropFailurePolicy implements FailurePolicy {

  /**
   * Returns the type of failure policy.
   *
   * @return {@link FailurePolicyType#DROP}
   */
  @Override
  public FailurePolicyType getFailurePolicyType() {
    return FailurePolicyType.DROP;
  }
}
