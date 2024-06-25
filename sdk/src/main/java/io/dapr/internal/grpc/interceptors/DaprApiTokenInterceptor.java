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
import io.dapr.config.Properties;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

/**
 * Class to be used as part of your service's client stub interceptor to include Dapr tokens.
 */
public class DaprApiTokenInterceptor implements ClientInterceptor {

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> methodDescriptor,
      CallOptions options,
      Channel channel) {
    ClientCall<ReqT, RespT> clientCall = channel.newCall(methodDescriptor, options);
    return new ForwardingClientCall.SimpleForwardingClientCall<>(clientCall) {
      @Override
      public void start(final Listener<RespT> responseListener, final Metadata metadata) {
        String daprApiToken = Properties.API_TOKEN.get();
        if (daprApiToken != null) {
          metadata.put(Metadata.Key.of(Headers.DAPR_API_TOKEN, Metadata.ASCII_STRING_MARSHALLER), daprApiToken);
        }
        super.start(responseListener, metadata);
      }
    };
  }

}
