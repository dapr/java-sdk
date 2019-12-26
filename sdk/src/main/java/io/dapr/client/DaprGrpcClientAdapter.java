package io.dapr.client;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.*;
import io.dapr.DaprGrpc;
import io.dapr.DaprProtos;
import io.dapr.utils.ObjectSerializer;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import reactor.core.publisher.Mono;

import java.io.IOException;

public class DaprGrpcClientAdapter implements DaprClientAdapter {

  private DaprGrpc.DaprFutureStub client;
  private ObjectSerializer objectSerializer;

  private DaprGrpcClientAdapter(DaprGrpc.DaprFutureStub futureClient) {
    client = futureClient;
    objectSerializer = new ObjectSerializer();
  }

  public static DaprClientAdapter build(String host, int port) {
    if (null == host || "".equals(host.trim())) {
      throw new IllegalStateException("Host must is required.");
    }
    if (port <= 0) {
      throw new IllegalStateException("Invalid port.");
    }
    ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
    return new DaprGrpcClientAdapter(DaprGrpc.newFutureStub(channel));
  }

  public static DaprClientAdapter build(String host) {
    return build(host, 80);
  }

  @Override
  public <T> Mono<Void> publishEvent(T event) {
    try {
      String serializedEvent = objectSerializer.serialize(event);
      DaprProtos.PublishEventEnvelope envelope = DaprProtos.PublishEventEnvelope.parseFrom(serializedEvent.getBytes());
      ListenableFuture<Empty> futureEmpty = client.publishEvent(envelope);
      return Mono.just(futureEmpty).flatMap(f -> {
        try {
          f.get();
        } catch (Exception ex) {
          return Mono.error(ex);
        }
        return Mono.empty();
      });
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  @Override
  public <T, K> Mono<T> invokeService(K request, Class<T> clazz) {
    try {
      String serializedRequest = objectSerializer.serialize(request);
      DaprProtos.InvokeServiceEnvelope envelope =
          DaprProtos.InvokeServiceEnvelope.parseFrom(serializedRequest.getBytes());
      ListenableFuture<DaprProtos.InvokeServiceResponseEnvelope> futureResponse =
          client.invokeService(envelope);
      return Mono.just(futureResponse).flatMap(f -> {
        try {
          return Mono.just(objectSerializer.deserialize(f.get().getData().getValue().toStringUtf8(), clazz));
        } catch (Exception ex) {
          return Mono.error(ex);
        }
      });

    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  @Override
  public <T> Mono<Void> invokeBinding(T request) {
    try {
      String serializedRequest = objectSerializer.serialize(request);
      DaprProtos.InvokeBindingEnvelope envelope =
          DaprProtos.InvokeBindingEnvelope.parseFrom(serializedRequest.getBytes());
      ListenableFuture<Empty> futureEmpty = client.invokeBinding(envelope);
      return Mono.just(futureEmpty).flatMap(f -> {
        try {
          f.get();
        } catch (Exception ex) {
          return Mono.error(ex);
        }
        return Mono.empty();
      });
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  @Override
  public <T, K> Mono<T> getState(K key, Class<T> clazz) {
    try {
      String serializedRequest = objectSerializer.serialize(key);
      DaprProtos.GetStateEnvelope envelope = DaprProtos.GetStateEnvelope.parseFrom(serializedRequest.getBytes());
      ListenableFuture<DaprProtos.GetStateResponseEnvelope> futureResponse = client.getState(envelope);
      return Mono.just(futureResponse).flatMap(f -> {
        try {
          return Mono.just(objectSerializer.deserialize(f.get().getData().getValue().toStringUtf8(), clazz));
        } catch (Exception ex) {
          return Mono.error(ex);
        }
      });
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  @Override
  public <T> Mono<Void> saveState(T state) {
    try {
      String serializedRequest = objectSerializer.serialize(state);
      DaprProtos.SaveStateEnvelope envelope = DaprProtos.SaveStateEnvelope.parseFrom(serializedRequest.getBytes());
      ListenableFuture<Empty> futureEmpty = client.saveState(envelope);
      return Mono.just(futureEmpty).flatMap(f -> {
        try {
          f.get();
        } catch (Exception ex) {
          return Mono.error(ex);
        }
        return Mono.empty();
      });
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  @Override
  public <T> Mono<Void> deleteState(T key) {
    try {
      String serializedRequest = objectSerializer.serialize(key);
      DaprProtos.DeleteStateEnvelope envelope = DaprProtos.DeleteStateEnvelope.parseFrom(serializedRequest.getBytes());
      ListenableFuture<Empty> futureEmpty = client.deleteState(envelope);
      return Mono.just(futureEmpty).flatMap(f -> {
        try {
          f.get();
        } catch (Exception ex) {
          return Mono.error(ex);
        }
        return Mono.empty();
      });
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

}