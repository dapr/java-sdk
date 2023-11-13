/*
 * Copyright 2021 The Dapr Authors
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

import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.InvokeMethodRequest;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.utils.TypeRef;
import io.dapr.v1.CommonProtos;
import io.dapr.v1.DaprGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.io.IOException;
import java.util.stream.Stream;

import reactor.util.context.ContextView;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnableRuleMigrationSupport
public class DaprClientGrpcTelemetryTest {

  private static final Metadata.Key<byte[]> GRPC_TRACE_BIN_KEY = Metadata.Key.of(Headers.GRPC_TRACE_BIN,
      Metadata.BINARY_BYTE_MARSHALLER);

  private static final Metadata.Key<String> TRACEPARENT_KEY = Metadata.Key.of("traceparent",
      Metadata.ASCII_STRING_MARSHALLER);

  private static final Metadata.Key<String> TRACESTATE_KEY = Metadata.Key.of("tracestate",
      Metadata.ASCII_STRING_MARSHALLER);

  /**
   * This rule manages automatic graceful shutdown for the registered servers and channels at the
   * end of test.
   */
  @Rule
  public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private DaprClient client;

  public static Stream<Arguments> data() {
    return Stream.of(
        Arguments.of(
              "00-0af7651916cd43dd8448eb211c80319c-b9c7c989f97918e1-01",
              "congo=ucfJifl5GOE,rojo=00f067aa0ba902b7",
           true
        ),
      Arguments.of(
null,
        null,
        false
      ),
      Arguments.of(
        null,
        "congo=ucfJifl5GOE,rojo=00f067aa0ba902b7",
        false
      ),
      Arguments.of(
        "00-0af7651916cd43dd8448eb211c80319c-b9c7c989f97918e1-01",
        null,
        true
      ),
      Arguments.of(
        "BAD FORMAT",
        null,
        false
      ),
      Arguments.of(
        "00-0af7651916cd43dd8448eb211c80319c-b9c7c989f97918e1-01",
        "INVALID",
        false
      )
    );
  }

  public void setup(String traceparent, String tracestate, boolean expectGrpcTraceBin) throws IOException {
    DaprGrpc.DaprImplBase daprImplBase = new DaprGrpc.DaprImplBase() {

      public void invokeService(io.dapr.v1.DaprProtos.InvokeServiceRequest request,
          io.grpc.stub.StreamObserver<io.dapr.v1.CommonProtos.InvokeResponse> responseObserver) {
        responseObserver.onNext(CommonProtos.InvokeResponse.getDefaultInstance());
        responseObserver.onCompleted();
      }

    };

    ServerServiceDefinition service = ServerInterceptors.intercept(daprImplBase, new ServerInterceptor() {
      @Override
      public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
          Metadata metadata,
          ServerCallHandler<ReqT, RespT> serverCallHandler) {

        assertEquals(traceparent, metadata.get(TRACEPARENT_KEY));
        assertEquals(tracestate, metadata.get(TRACESTATE_KEY));
        assertEquals((metadata.get(GRPC_TRACE_BIN_KEY) != null), expectGrpcTraceBin);
        return serverCallHandler.startCall(serverCall, metadata);
      }
    });

    // Generate a unique in-process server name.
    String serverName = InProcessServerBuilder.generateName();
    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanup.register(InProcessServerBuilder.forName(serverName).directExecutor()
        .addService(service)
        .build().start());

    // Create a client channel and register for automatic graceful shutdown.
    ManagedChannel channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
    DaprGrpc.DaprStub asyncStub = DaprGrpc.newStub(channel);
    client = new DaprClientGrpc(
        new GrpcChannelFacade(channel), asyncStub, new DefaultObjectSerializer(), new DefaultObjectSerializer());
  }

  public void setup() throws IOException {
    DaprGrpc.DaprImplBase daprImplBase = new DaprGrpc.DaprImplBase() {

      public void invokeService(io.dapr.v1.DaprProtos.InvokeServiceRequest request,
                                io.grpc.stub.StreamObserver<io.dapr.v1.CommonProtos.InvokeResponse> responseObserver) {
        responseObserver.onNext(CommonProtos.InvokeResponse.getDefaultInstance());
        responseObserver.onCompleted();
      }

    };

    ServerServiceDefinition service = ServerInterceptors.intercept(daprImplBase, new ServerInterceptor() {
      @Override
      public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
                                                                   Metadata metadata,
                                                                   ServerCallHandler<ReqT, RespT> serverCallHandler) {

          assertNull(metadata.get(TRACEPARENT_KEY));
          assertNull(metadata.get(TRACESTATE_KEY));
          assertNull(metadata.get(GRPC_TRACE_BIN_KEY));
          return serverCallHandler.startCall(serverCall, metadata);

      }
    });

    // Generate a unique in-process server name.
    String serverName = InProcessServerBuilder.generateName();
    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanup.register(InProcessServerBuilder.forName(serverName).directExecutor()
      .addService(service)
      .build().start());

    // Create a client channel and register for automatic graceful shutdown.
    ManagedChannel channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
    DaprGrpc.DaprStub asyncStub = DaprGrpc.newStub(channel);
    client = new DaprClientGrpc(
      new GrpcChannelFacade(channel), asyncStub, new DefaultObjectSerializer(), new DefaultObjectSerializer());
  }

  @ParameterizedTest
  @MethodSource("data")
  public void invokeServiceVoidWithTracingTest(String traceparent, String tracestate, boolean expectGrpcTraceBin) throws IOException {
    // setup server
    setup(traceparent, tracestate, expectGrpcTraceBin);

    Context context = Context.empty();
      if (traceparent != null) {
        context = context.put("traceparent", traceparent);
      }
      if (tracestate != null) {
        context = context.put("tracestate", tracestate);
      }

    final Context contextCopy = context;
    InvokeMethodRequest req = new InvokeMethodRequest("appId", "method")
        .setBody("request")
        .setHttpExtension(HttpExtension.NONE);
    Mono<Void> result = this.client.invokeMethod(req, TypeRef.get(Void.class))
        .contextWrite(it -> it.putAll(contextCopy));
    result.block();
  }

  @Test
  public void invokeServiceVoidWithTracingTestAndEmptyContext() throws IOException {
    // setup server
    setup();

    Context context = null;

    final Context contextCopy = context;
    InvokeMethodRequest req = new InvokeMethodRequest("appId", "method")
      .setBody("request")
      .setHttpExtension(HttpExtension.NONE);
    Mono<Void> result = this.client.invokeMethod(req, TypeRef.get(Void.class))
      .contextWrite(it -> it.putAll(contextCopy == null ? (ContextView) Context.empty() : contextCopy));
    result.block();
  }
}
