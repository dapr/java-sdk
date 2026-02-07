/*
 * Copyright 2026 The Dapr Authors
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

package io.dapr.utils;

import io.dapr.client.domain.ConstantFailurePolicy;
import io.dapr.client.domain.FailurePolicy;
import io.dapr.client.domain.FailurePolicyType;
import io.dapr.v1.CommonProtos;

public class FailurePolicyUtils {
  /**
   * Converts a FailurePolicy to a JobFailurePolicy.
   *
   * @param failurePolicy the failure policy to convert.
   * @return the converted JobFailurePolicy.
   */
  public static CommonProtos.JobFailurePolicy getJobFailurePolicy(FailurePolicy failurePolicy) {
    CommonProtos.JobFailurePolicy.Builder jobFailurePolicyBuilder = CommonProtos.JobFailurePolicy.newBuilder();

    if (failurePolicy.getFailurePolicyType() == FailurePolicyType.DROP) {
      jobFailurePolicyBuilder.setDrop(CommonProtos.JobFailurePolicyDrop.newBuilder().build());
      return jobFailurePolicyBuilder.build();
    }

    CommonProtos.JobFailurePolicyConstant.Builder constantPolicyBuilder =
        CommonProtos.JobFailurePolicyConstant.newBuilder();
    ConstantFailurePolicy jobConstantFailurePolicy = (ConstantFailurePolicy) failurePolicy;

    if (jobConstantFailurePolicy.getMaxRetries() != null) {
      constantPolicyBuilder.setMaxRetries(jobConstantFailurePolicy.getMaxRetries());
    }

    if (jobConstantFailurePolicy.getDurationBetweenRetries() != null) {
      constantPolicyBuilder.setInterval(com.google.protobuf.Duration.newBuilder()
          .setNanos(jobConstantFailurePolicy.getDurationBetweenRetries().getNano()).build());
    }

    jobFailurePolicyBuilder.setConstant(constantPolicyBuilder.build());

    return jobFailurePolicyBuilder.build();
  }

}
