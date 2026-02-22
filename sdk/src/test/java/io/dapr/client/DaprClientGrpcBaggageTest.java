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

package io.dapr.client;

import io.dapr.internal.grpc.DaprClientGrpcInterceptors;
import io.dapr.v1.CommonProtos;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprInvokeProtos;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@EnableRuleMigrationSupport
public class DaprClientGrpcBaggageTest {

  private static final Metadata.Key<String> BAGGAGE_KEY =
      Metadata.Key.of(Headers.BAGGAGE, Metadata.ASCII_STRING_MARSHALLER);

  @Rule
  public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private DaprGrpc.DaprStub daprStub;

  @AfterEach
  public void tearDown() {
  }

  @Test
  public void testBaggagePropagated() throws IOException {
    String expectedBaggage = "key1=value1,key2=value2";
    AtomicReference<String> receivedBaggage = new AtomicReference<>();

    setupServer((metadata) -> {
      receivedBaggage.set(metadata.get(BAGGAGE_KEY));
    });

    Context context = Context.empty().put(Headers.BAGGAGE, expectedBaggage);

    Mono<Void> result = invoke()
        .contextWrite(it -> it.putAll((ContextView) context));
    result.block();

    assertEquals(expectedBaggage, receivedBaggage.get());
  }

  @Test
  public void testBaggageNotPropagatedWhenAbsent() throws IOException {
    AtomicReference<String> receivedBaggage = new AtomicReference<>();

    setupServer((metadata) -> {
      receivedBaggage.set(metadata.get(BAGGAGE_KEY));
    });

    Context context = Context.empty();

    Mono<Void> result = invoke()
        .contextWrite(it -> it.putAll((ContextView) context));
    result.block();

    assertNull(receivedBaggage.get());
  }

  @Test
  public void testBaggageWithSingleEntry() throws IOException {
    String expectedBaggage = "userId=alice";
    AtomicReference<String> receivedBaggage = new AtomicReference<>();

    setupServer((metadata) -> {
      receivedBaggage.set(metadata.get(BAGGAGE_KEY));
    });

    Context context = Context.empty().put(Headers.BAGGAGE, expectedBaggage);

    Mono<Void> result = invoke()
        .contextWrite(it -> it.putAll((ContextView) context));
    result.block();

    assertEquals(expectedBaggage, receivedBaggage.get());
  }

  @Test
  public void testBaggageWithTracingContext() throws IOException {
    String expectedBaggage = "key1=value1";
    String traceparent = "00-0af7651916cd43dd8448eb211c80319c-b9c7c989f97918e1-01";
    AtomicReference<String> receivedBaggage = new AtomicReference<>();

    setupServer((metadata) -> {
      receivedBaggage.set(metadata.get(BAGGAGE_KEY));
    });

    Context context = Context.empty()
        .put(Headers.BAGGAGE, expectedBaggage)
        .put("traceparent", traceparent);

    Mono<Void> result = invoke()
        .contextWrite(it -> it.putAll((ContextView) context));
    result.block();

    assertEquals(expectedBaggage, receivedBaggage.get());
  }

  private void setupServer(Consumer<Metadata> metadataAssertions) throws IOException {
    DaprGrpc.DaprImplBase daprImplBase = new DaprGrpc.DaprImplBase() {
      @Override
      public void invokeService(DaprInvokeProtos.InvokeServiceRequest request,
          StreamObserver<CommonProtos.InvokeResponse> responseObserver) {
        responseObserver.onNext(CommonProtos.InvokeResponse.getDefaultInstance());
        responseObserver.onCompleted();
      }
    };

    ServerServiceDefinition service = ServerInterceptors.intercept(daprImplBase, new ServerInterceptor() {
      @Override
      public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
          Metadata metadata,
          ServerCallHandler<ReqT, RespT> serverCallHandler) {
        metadataAssertions.accept(metadata);
        return serverCallHandler.startCall(serverCall, metadata);
      }
    });

    String serverName = InProcessServerBuilder.generateName();
    grpcCleanup.register(InProcessServerBuilder.forName(serverName).directExecutor()
        .addService(service)
        .build().start());

    ManagedChannel channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
    daprStub = DaprGrpc.newStub(channel);
  }

  private Mono<Void> invoke() {
    DaprInvokeProtos.InvokeServiceRequest req =
        DaprInvokeProtos.InvokeServiceRequest.newBuilder().build();
    return Mono.deferContextual(
        context -> this.<CommonProtos.InvokeResponse>createMono(
            it -> new DaprClientGrpcInterceptors().intercept(daprStub, context).invokeService(req, it)
        )
    ).then();
  }

  private <T> Mono<T> createMono(Consumer<StreamObserver<T>> consumer) {
    return Mono.create(sink -> consumer.accept(createStreamObserver(sink)));
  }

  private <T> StreamObserver<T> createStreamObserver(MonoSink<T> sink) {
    return new StreamObserver<T>() {
      @Override
      public void onNext(T value) {
        sink.success(value);
      }

      @Override
      public void onError(Throwable t) {
        sink.error(new ExecutionException(t));
      }

      @Override
      public void onCompleted() {
        sink.success();
      }
    };
  }
}
