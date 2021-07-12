/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.client;

import com.google.protobuf.ByteString;
import io.dapr.actors.runtime.ActorInvocationContext;
import io.dapr.config.Properties;
import io.dapr.exceptions.DaprException;
import io.dapr.internal.opencensus.GrpcWrapper;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprProtos;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.stub.StreamObserver;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.util.context.Context;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * A DaprClient over GRPC for Actor.
 */
class DaprGrpcClient implements DaprClient {

  /**
   * The async gRPC stub.
   */
  private DaprGrpc.DaprStub client;

  /**
   * Internal constructor.
   *
   * @param grpcClient Dapr's GRPC client.
   */
  DaprGrpcClient(DaprGrpc.DaprStub grpcClient) {
    this.client = intercept(grpcClient);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<byte[]> invoke(String actorType, String actorId, String methodName, byte[] jsonPayload) {
    return invoke(actorType, actorId, methodName, jsonPayload, new ActorInvocationContext());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<byte[]> invoke(String actorType,
                             String actorId,
                             String methodName,
                             byte[] jsonPayload,
                             ActorInvocationContext invocationContext) {
    DaprProtos.InvokeActorRequest req =
        DaprProtos.InvokeActorRequest.newBuilder()
            .setActorType(actorType)
            .setActorId(actorId)
            .setMethod(methodName)
            .setData(jsonPayload == null ? ByteString.EMPTY : ByteString.copyFrom(jsonPayload))
            .build();
    return Mono.subscriberContext().flatMap(
        context -> this.<DaprProtos.InvokeActorResponse>createMono(
            it -> intercept(context, client).invokeActor(req, it)
        )
    ).map(r -> r.getData().toByteArray());
  }

  /**
   * Populates GRPC client with interceptors.
   *
   * @param client GRPC client for Dapr.
   * @return Client after adding interceptors.
   */
  private static DaprGrpc.DaprStub intercept(DaprGrpc.DaprStub client) {
    ClientInterceptor interceptor = new ClientInterceptor() {
      @Override
      public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
          MethodDescriptor<ReqT, RespT> methodDescriptor,
          CallOptions callOptions,
          Channel channel) {
        ClientCall<ReqT, RespT> clientCall = channel.newCall(methodDescriptor, callOptions);
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(clientCall) {
          @Override
          public void start(final Listener<RespT> responseListener, final Metadata metadata) {
            String daprApiToken = Properties.API_TOKEN.get();
            if (daprApiToken != null) {
              metadata.put(Metadata.Key.of("dapr-api-token", Metadata.ASCII_STRING_MARSHALLER), daprApiToken);
            }

            super.start(responseListener, metadata);
          }
        };
      }
    };
    return client.withInterceptors(interceptor);
  }

  /**
   * Populates GRPC client with interceptors for telemetry.
   *
   * @param context Reactor's context.
   * @param client GRPC client for Dapr.
   * @return Client after adding interceptors.
   */
  private static DaprGrpc.DaprStub intercept(Context context, DaprGrpc.DaprStub client) {
    return GrpcWrapper.intercept(context, client);
  }

  private <T> Mono<T> createMono(Consumer<StreamObserver<T>> consumer) {
    return Mono.create(sink -> DaprException.wrap(() -> consumer.accept(createStreamObserver(sink))).run());
  }

  private <T> StreamObserver<T> createStreamObserver(MonoSink<T> sink) {
    return new StreamObserver<T>() {
      @Override
      public void onNext(T value) {
        sink.success(value);
      }

      @Override
      public void onError(Throwable t) {
        sink.error(DaprException.propagate(new ExecutionException(t)));
      }

      @Override
      public void onCompleted() {
        sink.success();
      }
    };
  }
}
