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
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

import java.util.function.Consumer;

/**
 * Consumes gRPC metadata.
 */
public class DaprMetadataReceiverInterceptor implements ClientInterceptor {

  private final Consumer<Metadata> metadataConsumer;

  /**
   * Creates an instance of the consumer for gRPC metadata.
   * @param metadataConsumer gRPC metadata consumer
   */
  public DaprMetadataReceiverInterceptor(Consumer<Metadata> metadataConsumer) {
    this.metadataConsumer = metadataConsumer;
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
        final ClientCall.Listener<RespT>  headerListener =
            new ForwardingClientCallListener.SimpleForwardingClientCallListener<>(responseListener) {
              @Override
              public void onHeaders(Metadata headers) {
                responseListener.onHeaders(headers);
                if (metadataConsumer != null) {
                  metadataConsumer.accept(headers);
                }
              }
            };
        super.start(headerListener, metadata);
      }
    };
  }
}
