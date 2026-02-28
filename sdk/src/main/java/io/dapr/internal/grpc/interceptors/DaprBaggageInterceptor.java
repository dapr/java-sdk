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

import io.dapr.client.Headers;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import reactor.util.context.ContextView;

/**
 * Injects W3C Baggage header into gRPC metadata from Reactor's context.
 */
public class DaprBaggageInterceptor implements ClientInterceptor {

  private static final Metadata.Key<String> BAGGAGE_KEY =
      Metadata.Key.of(Headers.BAGGAGE, Metadata.ASCII_STRING_MARSHALLER);

  private final ContextView context;

  /**
   * Creates an instance of the baggage interceptor for gRPC.
   *
   * @param context Reactor's context
   */
  public DaprBaggageInterceptor(ContextView context) {
    this.context = context;
  }

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> methodDescriptor,
      CallOptions callOptions,
      Channel channel) {
    ClientCall<ReqT, RespT> clientCall = channel.newCall(methodDescriptor, callOptions);
    return new ForwardingClientCall.SimpleForwardingClientCall<>(clientCall) {
      @Override
      public void start(final Listener<RespT> responseListener, final Metadata metadata) {
        if (context != null && context.hasKey(Headers.BAGGAGE)) {
          String baggageValue = context.get(Headers.BAGGAGE).toString();
          if (baggageValue != null && !baggageValue.isEmpty()) {
            metadata.put(BAGGAGE_KEY, baggageValue);
          }
        }
        super.start(responseListener, metadata);
      }
    };
  }
}
