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

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

/**
 * Class to be used as part of your service's client stub interceptor to include Dapr App Id metadata.
 */
public class DaprAppIdInterceptor implements ClientInterceptor {

  private final Metadata extraHeaders;

  public DaprAppIdInterceptor(String appId) {
    this.extraHeaders = buildMetadata(appId);
  }

  private static final Metadata buildMetadata(String appId) {
    if (appId == null) {
      return null;
    }

    Metadata headers = new Metadata();
    headers.put(Metadata.Key.of("dapr-app-id", Metadata.ASCII_STRING_MARSHALLER), appId);
    return headers;
  }

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
      MethodDescriptor<ReqT, RespT> methodDescriptor,
      CallOptions options,
      Channel channel) {
    ClientCall<ReqT, RespT> clientCall = channel.newCall(methodDescriptor, options);
    final Metadata extraHeaders = this.extraHeaders;
    return new ForwardingClientCall.SimpleForwardingClientCall<>(clientCall) {
      @Override
      public void start(ClientCall.Listener<RespT> responseListener, Metadata headers) {
        if (extraHeaders != null) {
          headers.merge(extraHeaders);
        }
        super.start(responseListener, headers);
      }
    };
  }

}
