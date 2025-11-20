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

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

public class TraceParentClientInterceptor implements ClientInterceptor {

  private Tracer tracer;

  public TraceParentClientInterceptor(Tracer tracer) {
    this.tracer = tracer;
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
          if (tracer == null) {
            super.start(responseListener, metadata);
          }
          Span span = tracer.spanBuilder("dapr.workflow.grpc")
              .setAttribute("method", methodDescriptor.getFullMethodName())
              .setAttribute("options", options.toString())
              .setAttribute("metadata", metadata.toString())
              .startSpan();
          try {
            String traceId = span.getSpanContext().getTraceId();
            String traceState = span.getSpanContext().getTraceState().toString();
            byte[] traceIdBytes = span.getSpanContext().getTraceIdBytes();
            metadata.put(Metadata.Key.of("traceparent", Metadata.ASCII_STRING_MARSHALLER), traceId);
            metadata.put(Metadata.Key.of("tracestate", Metadata.ASCII_STRING_MARSHALLER), traceState);
            metadata.put(Metadata.Key.of("grpc-trace-bin", Metadata.BINARY_BYTE_MARSHALLER), traceIdBytes);
            System.out.println("Trace Id in interceptor: " + traceId);
            System.out.println("Trace State in interceptor: " + traceState);
            System.out.println("Metadata at the interceptor: " + metadata);
            super.start(responseListener, metadata);
          } finally {
            span.end();
          }
        }
    };
  }
}

