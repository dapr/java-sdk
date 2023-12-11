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

public class APITokenClientInterceptor implements ClientInterceptor {
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
          String daprApiToken = Properties.API_TOKEN.get();
          if (daprApiToken != null) {
            metadata.put(Metadata.Key.of(Headers.DAPR_API_TOKEN, Metadata.ASCII_STRING_MARSHALLER), daprApiToken);
          }
          super.start(responseListener, metadata);
        }
      };
    }
}
