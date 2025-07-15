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

package io.dapr.workflows.internal;

import io.dapr.client.Headers;
import io.dapr.config.Properties;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

public class ApiTokenClientInterceptor implements ClientInterceptor {

  private Properties properties;

  public ApiTokenClientInterceptor(Properties properties) {
    this.properties = properties;
  }

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> methodDescriptor,
            CallOptions options,
            Channel channel) {
    // TBD: do we need timeout in workflow client?
    ClientCall<ReqT, RespT> clientCall = channel.newCall(methodDescriptor, options);
    return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(clientCall) {
        @Override
        public void start(final Listener<RespT> responseListener, final Metadata metadata) {
            String daprApiToken = properties.getValue(Properties.API_TOKEN);
            if (daprApiToken != null) {
              metadata.put(Metadata.Key.of(Headers.DAPR_API_TOKEN, Metadata.ASCII_STRING_MARSHALLER), daprApiToken);
            }
            super.start(responseListener, metadata);
        }
    };
  }
}

