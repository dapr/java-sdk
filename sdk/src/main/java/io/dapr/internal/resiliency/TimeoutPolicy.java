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
import io.grpc.CallOptions;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Timeout policy for SDK communication to Dapr API.
 */
public final class TimeoutPolicy {

  private final Duration timeout;

  /**
   * Instantiates a new timeout policy with override value.
   * @param timeout Override timeout value.
   */
  public TimeoutPolicy(Duration timeout) {
    this.timeout = timeout != null ? timeout : Properties.TIMEOUT.get();
  }

  /**
   * Instantiates a new timeout policy with default value.
   */
  public TimeoutPolicy() {
    this(null);
  }

  /**
   * Applies the timeout policy to a gRPC call options.
   * @param options Call options
   * @return Call options with retry policy applied
   */
  public CallOptions apply(CallOptions options) {
    if (this.timeout.isZero() || this.timeout.isNegative()) {
      return options;
    }

    return options.withDeadlineAfter(this.timeout.toMillis(), TimeUnit.MILLISECONDS);
  }
}
