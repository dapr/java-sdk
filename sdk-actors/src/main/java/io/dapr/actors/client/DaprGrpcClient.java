/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.client;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import io.dapr.exceptions.DaprException;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprProtos;
import reactor.core.publisher.Mono;

/**
 * A DaprClient over GRPC for Actor.
 */
class DaprGrpcClient implements DaprClient {

  /**
   * The GRPC client to be used.
   *
   * @see DaprGrpc.DaprFutureStub
   */
  private DaprGrpc.DaprFutureStub client;

  /**
   * Internal constructor.
   *
   * @param grpcClient Dapr's GRPC client.
   */
  DaprGrpcClient(DaprGrpc.DaprFutureStub grpcClient) {
    this.client = grpcClient;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<byte[]> invoke(String actorType, String actorId, String methodName, byte[] jsonPayload) {
    return Mono.fromCallable(DaprException.wrap(() -> {
      DaprProtos.InvokeActorRequest req =
          DaprProtos.InvokeActorRequest.newBuilder()
              .setActorType(actorType)
              .setActorId(actorId)
              .setMethod(methodName)
              .setData(jsonPayload == null ? ByteString.EMPTY : ByteString.copyFrom(jsonPayload))
              .build();

      return get(client.invokeActor(req));
    })).map(r -> r.getData().toByteArray());
  }

  private static <V> V get(ListenableFuture<V> future) {
    try {
      return future.get();
    } catch (Exception e) {
      DaprException.wrap(e);
    }

    return null;
  }
}
