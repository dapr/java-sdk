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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import reactor.util.context.ContextView;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
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

  @Parameterized.Parameter
  public Scenario scenario;

  @Parameterized.Parameters
  public static Collection<Scenario[]> data() {
    return Arrays.asList(new Scenario[][]{
        {
            new Scenario() {{
              traceparent = "00-0af7651916cd43dd8448eb211c80319c-b9c7c989f97918e1-01";
              tracestate = "congo=ucfJifl5GOE,rojo=00f067aa0ba902b7";
              expectGrpcTraceBin = true;
            }}
        },
        {
            new Scenario() {{
              traceparent = null;
              tracestate = null;
              expectGrpcTraceBin = false;
            }}
        },
        {
            new Scenario() {{
              traceparent = null;
              tracestate = "congo=ucfJifl5GOE,rojo=00f067aa0ba902b7";
              expectGrpcTraceBin = false;
            }}
        },
        {
            new Scenario() {{
              traceparent = "00-0af7651916cd43dd8448eb211c80319c-b9c7c989f97918e1-01";
              tracestate = null;
              expectGrpcTraceBin = true;
            }},
        },
        {
            new Scenario() {{
              traceparent = "BAD FORMAT";
              tracestate = null;
              expectGrpcTraceBin = false;
            }},
        },
        {
            new Scenario() {{
              traceparent = "00-0af7651916cd43dd8448eb211c80319c-b9c7c989f97918e1-01";
              tracestate = "INVALID";
              expectGrpcTraceBin = false;
            }},
        },
        {
            null
        }
    });
  }

  @Before
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
        if (scenario == null) {
          assertNull(metadata.get(TRACEPARENT_KEY));
          assertNull(metadata.get(TRACESTATE_KEY));
          assertNull(metadata.get(GRPC_TRACE_BIN_KEY));
          return serverCallHandler.startCall(serverCall, metadata);
        }

        assertEquals(scenario.traceparent, metadata.get(TRACEPARENT_KEY));
        assertEquals(scenario.tracestate, metadata.get(TRACESTATE_KEY));
        assertTrue((metadata.get(GRPC_TRACE_BIN_KEY) != null) == scenario.expectGrpcTraceBin);
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
    Closeable closeableChannel = () -> {
      if (channel != null && !channel.isShutdown()) {
        channel.shutdown();
      }
    };
    DaprGrpc.DaprStub asyncStub = DaprGrpc.newStub(channel);
    client = new DaprClientGrpc(
        closeableChannel, asyncStub, new DefaultObjectSerializer(), new DefaultObjectSerializer());
  }

  @Test
  public void invokeServiceVoidWithTracingTest() {
    Context context = null;
    if (scenario != null) {
      context = Context.empty();
      if (scenario.traceparent != null) {
        context = context.put("traceparent", scenario.traceparent);
      }
      if (scenario.tracestate != null) {
        context = context.put("tracestate", scenario.tracestate);
      }
    }
    final Context contextCopy = context;
    InvokeMethodRequest req = new InvokeMethodRequest("appId", "method")
        .setBody("request")
        .setHttpExtension(HttpExtension.NONE);
    Mono<Void> result = this.client.invokeMethod(req, TypeRef.get(Void.class))
        .contextWrite(it -> it.putAll(contextCopy == null ? (ContextView) Context.empty() : contextCopy));
    result.block();
  }

  @After
  public void tearDown() throws Exception {
    client.close();
  }

  public static class Scenario {
    public String traceparent;
    public String tracestate;
    public boolean expectGrpcTraceBin;
  }
}
