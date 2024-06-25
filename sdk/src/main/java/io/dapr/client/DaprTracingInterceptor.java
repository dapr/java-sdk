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

package io.dapr.client;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;
import reactor.util.context.ContextView;

/**
 * Injects tracing headers to gRPC metadata.
 */
public class DaprTracingInterceptor implements ClientInterceptor {

  private final io.dapr.internal.grpc.interceptors.DaprTracingInterceptor delegate;

  /**
   * Creates an instance of the injector for gRPC context from Reactor's context.
   * @param context Reactor's context
   */
  public DaprTracingInterceptor(ContextView context) {
    this.delegate = new io.dapr.internal.grpc.interceptors.DaprTracingInterceptor(context);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> methodDescriptor,
      CallOptions callOptions,
      Channel channel) {
    return this.delegate.interceptCall(methodDescriptor, callOptions, channel);
  }
}
