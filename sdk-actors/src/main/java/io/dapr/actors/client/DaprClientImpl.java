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

package io.dapr.actors.client;

import com.google.protobuf.ByteString;
import io.dapr.client.resiliency.ResiliencyOptions;
import io.dapr.exceptions.DaprException;
import io.dapr.internal.grpc.DaprClientGrpcInterceptors;
import io.dapr.internal.resiliency.RetryPolicy;
import io.dapr.internal.resiliency.TimeoutPolicy;
import io.dapr.v1.DaprActorsProtos;
import io.dapr.v1.DaprGrpc;
import io.grpc.stub.StreamObserver;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * A DaprClient over GRPC for Actor.
 */
class DaprClientImpl implements DaprClient {

  /**
   * Retry policy for SDK calls to Dapr API.
   */
  private final RetryPolicy retryPolicy;

  /**
   * The async gRPC stub.
   */
  private final DaprGrpc.DaprStub client;

  /**
   * gRPC client interceptors.
   */
  private final DaprClientGrpcInterceptors grpcInterceptors;

  /**
   * Metadata for actor invocation requests.
   */
  private final Map<String, String> metadata;

  /**
   * Internal constructor.
   *
   * @param grpcClient Dapr's GRPC client.
   * @param metadata gRPC metadata or HTTP headers for actor server to receive.
   * @param resiliencyOptions Client resiliency options (optional).
   * @param daprApiToken Dapr API token (optional).
   */
  DaprClientImpl(
      DaprGrpc.DaprStub grpcClient,
      Map<String, String> metadata,
      ResiliencyOptions resiliencyOptions,
      String daprApiToken) {
    this.client = grpcClient;
    this.grpcInterceptors = new DaprClientGrpcInterceptors(daprApiToken,
        new TimeoutPolicy(resiliencyOptions == null ? null : resiliencyOptions.getTimeout()));
    this.retryPolicy = new RetryPolicy(
        resiliencyOptions == null ? null : resiliencyOptions.getMaxRetries());
    this.metadata = metadata == null ? Map.of() : metadata;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<byte[]> invoke(String actorType, String actorId, String methodName, byte[] jsonPayload) {
    DaprActorsProtos.InvokeActorRequest req =
        DaprActorsProtos.InvokeActorRequest.newBuilder()
            .setActorType(actorType)
            .setActorId(actorId)
            .setMethod(methodName)
            .putAllMetadata(this.metadata)
            .setData(jsonPayload == null ? ByteString.EMPTY : ByteString.copyFrom(jsonPayload))
            .build();
    return Mono.deferContextual(
        context -> this.<DaprActorsProtos.InvokeActorResponse>createMono(
            it -> this.grpcInterceptors.intercept(client, context).invokeActor(req, it)
        )
    ).map(r -> r.getData().toByteArray());
  }

  private <T> Mono<T> createMono(Consumer<StreamObserver<T>> consumer) {
    return retryPolicy.apply(
        Mono.create(sink -> DaprException.wrap(() -> consumer.accept(createStreamObserver(sink))).run()));
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
