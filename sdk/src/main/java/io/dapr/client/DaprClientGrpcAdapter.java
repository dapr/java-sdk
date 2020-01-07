/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.client;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Empty;
import io.dapr.DaprGrpc;
import io.dapr.DaprProtos;
import io.dapr.client.domain.StateKeyValue;
import io.dapr.client.domain.StateOptions;
import io.dapr.utils.ObjectSerializer;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * An adapter for the GRPC Client.
 *
 * @see io.dapr.DaprGrpc
 * @see io.dapr.client.DaprClient
 */
class DaprClientGrpcAdapter implements DaprClient {

  /**
   * The GRPC client to be used
   *
   * @see io.dapr.DaprGrpc.DaprFutureStub
   */
  private DaprGrpc.DaprFutureStub client;
  /**
   * A utitlity class for serialize and deserialize the messages sent and retrived by the client.
   */
  private ObjectSerializer objectSerializer;

  /**
   * Default access level constructor, in order to create an instance of this class use io.dapr.client.DaprClientBuilder
   *
   * @param futureClient
   * @see io.dapr.client.DaprClientBuilder
   */
  DaprClientGrpcAdapter(DaprGrpc.DaprFutureStub futureClient) {
    client = futureClient;
    objectSerializer = new ObjectSerializer();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<Void> publishEvent(String topic, T event) {
    try {
      String serializedEvent = objectSerializer.serialize(event);
      Map<String, String> mapEvent = new HashMap<>();
      mapEvent.put("Topic", topic);
      mapEvent.put("Data", serializedEvent);

      byte[] byteEvent = objectSerializer.serialize(mapEvent).getBytes();

      DaprProtos.PublishEventEnvelope envelope = DaprProtos.PublishEventEnvelope.parseFrom(byteEvent);
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

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, K> Mono<T> invokeService(String verb, String appId, String method, K request, Class<T> clazz) {
    try {
      Map<String, String> mapMessage = new HashMap<>();
      mapMessage.put("Id", objectSerializer.serialize(appId));
      mapMessage.put("Method", objectSerializer.serialize(verb));
      mapMessage.put("Data", objectSerializer.serialize(request));

      DaprProtos.InvokeServiceEnvelope envelope =
          DaprProtos.InvokeServiceEnvelope.parseFrom(objectSerializer.serialize(mapMessage).getBytes());
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

  /**
   * Operation not supported for GRPC
   * @throws UnsupportedOperationException every time is called.
   */
  public <T> Mono<Void> invokeService(String verb, String appId, String method, T request) {
    return Mono.error(new UnsupportedOperationException("Operation not supported for GRPC"));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<Void> invokeBinding(String name, T request) {
    try {
      Map<String, String> mapMessage = new HashMap<>();
      mapMessage.put("Name", name);
      mapMessage.put("Data", objectSerializer.serialize(request));
      DaprProtos.InvokeBindingEnvelope envelope =
          DaprProtos.InvokeBindingEnvelope.parseFrom(objectSerializer.serialize(mapMessage).getBytes());
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

  /**
   * {@inheritDoc}
   */
  @Override
  public<T, K> Mono<T> getState(StateKeyValue<K> key, StateOptions stateOptions, Class<T> clazz) {
    try {
      Map<String, String> request = new HashMap<>();
      request.put("Key", key.getKey());
      request.put("Consistency", stateOptions.getConsistency());
      String serializedRequest = objectSerializer.serialize(request);
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

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<Void> saveStates(List<StateKeyValue<T>> states) {
    try {
      String serializedRequest = objectSerializer.serialize(states);
      Map<String, String> mapStates = new HashMap<>();
      mapStates.put("Requests", serializedRequest);
      byte[] byteRequests = objectSerializer.serialize(mapStates).getBytes();
      DaprProtos.SaveStateEnvelope envelope = DaprProtos.SaveStateEnvelope.parseFrom(byteRequests);
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
  public <T> Mono<Void> saveState(String key, String etag, T value) {
    StateKeyValue<T> state = new StateKeyValue<>(value, key, etag);
    return saveStates(Arrays.asList(state));
  }

  /**
   * if stateOptions param is passed it will overrside state.options.
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<Void> deleteState(StateKeyValue<T> state, StateOptions stateOptions) {
    try {
      if (stateOptions != null)  {
        state.setOptions(stateOptions);
      }
      String serializedState = objectSerializer.serialize(state);
      DaprProtos.DeleteStateEnvelope envelope = DaprProtos.DeleteStateEnvelope.parseFrom(serializedState.getBytes());
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

  /**
   * Operation not supported for GRPC
   * @throws UnsupportedOperationException every time is called.
   */
  @Override
  public Mono<String> invokeActorMethod(String actorType, String actorId, String methodName, String jsonPayload) {
    return Mono.error(new UnsupportedOperationException("Operation not supported for GRPC"));
  }

  @Override
  public Mono<String> getActorState(String actorType, String actorId, String keyName) {
    return Mono.error(new UnsupportedOperationException("Operation not supported for GRPC"));
  }

  @Override
  public Mono<Void> saveStateTransactionally(String actorType, String actorId, String data) {
    return Mono.error(new UnsupportedOperationException("Operation not supported for GRPC"));
  }

  @Override
  public Mono<Void> registerReminder(String actorType, String actorId, String reminderName, String data) {
    return Mono.error(new UnsupportedOperationException("Operation not supported for GRPC"));
  }

  @Override
  public Mono<Void> unregisterReminder(String actorType, String actorId, String reminderName) {
    return Mono.error(new UnsupportedOperationException("Operation not supported for GRPC"));
  }

  @Override
  public Mono<Void> registerTimer(String actorType, String actorId, String timerName, String data) {
    return Mono.error(new UnsupportedOperationException("Operation not supported for GRPC"));
  }

  @Override
  public Mono<Void> unregisterTimer(String actorType, String actorId, String timerName) {
    return Mono.error(new UnsupportedOperationException("Operation not supported for GRPC"));
  }
}