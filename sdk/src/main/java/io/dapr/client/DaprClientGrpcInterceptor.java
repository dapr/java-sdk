/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr;

import io.dapr.utils.Constants;
import io.dapr.utils.Properties;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.MethodDescriptor;

/**
 * A dapr grpc client call interceptor.
 */
public class DaprClientGrpcInterceptor implements ClientInterceptor {
  
  /**
   * {@inheritDoc}
   */
  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT,
                                                            RespT> methodDescriptor,
                                                            CallOptions callOptions, Channel channel) {
    return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(channel.newCall(methodDescriptor, 
                                                                                            callOptions)) {
        @Override
        public void start(final Listener<RespT> responseListener, final Metadata headers) {
            String daprApiToken = Properties.getStringOrDefault(Constants.DAPR_API_TOKEN,
                                                                Constants.DAPR_API_TOKEN, null);
        if (daprApiToken != null) {
            headers.put(Key.of(Constants.DAPR_API_TOKEN_HEADER, Metadata.ASCII_STRING_MARSHALLER), daprApiToken);
        }
        super.start(responseListener, headers);
        }
      };
  }
}