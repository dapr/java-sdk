/*
 * Copyright 2024 The Dapr Authors
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

package io.dapr.internal.grpc.interceptors;

import io.dapr.internal.resiliency.TimeoutPolicy;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;

/**
 * Class to be used as part of your service's client stub interceptor to include timeout.
 */
public class DaprTimeoutInterceptor implements ClientInterceptor {

  private final TimeoutPolicy timeoutPolicy;

  public DaprTimeoutInterceptor(TimeoutPolicy timeoutPolicy) {
    this.timeoutPolicy = timeoutPolicy;
  }

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> methodDescriptor,
      CallOptions options,
      Channel channel) {
    if (timeoutPolicy == null) {
      return channel.newCall(methodDescriptor, options);
    }

    return channel.newCall(methodDescriptor, timeoutPolicy.apply(options));
  }

}
