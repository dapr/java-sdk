/*
 * Copyright 2026 The Dapr Authors
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

import io.dapr.exceptions.DaprException;
import io.dapr.internal.exceptions.DaprHttpException;
import io.dapr.internal.grpc.DaprClientGrpcInterceptors;
import io.dapr.internal.resiliency.RetryPolicy;
import io.dapr.v1.DaprGrpc;
import io.grpc.Metadata;
import io.grpc.stub.StreamObserver;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.util.context.ContextView;

import java.util.function.Consumer;

/**
 * Turns async gRPC calls to the Dapr sidecar into Reactor types, with the client's interceptors and retry policy.
 */
final class DaprGrpcCalls {

  private final DaprGrpc.DaprStub asyncStub;

  private final DaprClientGrpcInterceptors grpcInterceptors;

  private final RetryPolicy retryPolicy;

  DaprGrpcCalls(
      DaprGrpc.DaprStub asyncStub, DaprClientGrpcInterceptors grpcInterceptors, RetryPolicy retryPolicy) {
    this.asyncStub = asyncStub;
    this.grpcInterceptors = grpcInterceptors;
    this.retryPolicy = retryPolicy;
  }

  /**
   * Populates GRPC client with interceptors for telemetry.
   *
   * @param context Reactor's context.
   * @return Client after adding interceptors.
   */
  DaprGrpc.DaprStub intercept(ContextView context) {
    return this.grpcInterceptors.intercept(this.asyncStub, context);
  }

  /**
   * Populates GRPC client with interceptors for telemetry.
   *
   * @param context Reactor's context.
   * @param metadataConsumer Consumer of gRPC metadata.
   * @return Client after adding interceptors.
   */
  DaprGrpc.DaprStub intercept(ContextView context, Consumer<Metadata> metadataConsumer) {
    return this.grpcInterceptors.intercept(this.asyncStub, context, metadataConsumer);
  }

  <T> Mono<T> createMono(Consumer<StreamObserver<T>> consumer) {
    return this.createMono(null, consumer);
  }

  <T> Mono<T> createMono(Metadata metadata, Consumer<StreamObserver<T>> consumer) {
    return retryPolicy.apply(
        Mono.create(sink -> DaprException.wrap(() -> consumer.accept(
            createStreamObserver(sink, metadata))).run()));
  }

  <T> Flux<T> createFlux(Consumer<StreamObserver<T>> consumer) {
    return this.createFlux(null, consumer);
  }

  <T> Flux<T> createFlux(Metadata metadata, Consumer<StreamObserver<T>> consumer) {
    return retryPolicy.apply(
        Flux.create(sink -> DaprException.wrap(() -> consumer.accept(createStreamObserver(sink, metadata))).run()));
  }

  private <T> StreamObserver<T> createStreamObserver(MonoSink<T> sink, Metadata grpcMetadata) {
    return new StreamObserver<T>() {
      @Override
      public void onNext(T value) {
        sink.success(value);
      }

      @Override
      public void onError(Throwable t) {
        sink.error(DaprException.propagate(DaprHttpException.fromGrpcExecutionException(grpcMetadata, t)));
      }

      @Override
      public void onCompleted() {
        sink.success();
      }
    };
  }

  private <T> StreamObserver<T> createStreamObserver(FluxSink<T> sink, final Metadata grpcMetadata) {
    return new StreamObserver<T>() {
      @Override
      public void onNext(T value) {
        sink.next(value);
      }

      @Override
      public void onError(Throwable t) {
        sink.error(DaprException.propagate(DaprHttpException.fromGrpcExecutionException(grpcMetadata, t)));
      }

      @Override
      public void onCompleted() {
        sink.complete();
      }
    };
  }
}
