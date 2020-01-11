/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.client;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;
import com.google.protobuf.Empty;
import io.dapr.DaprGrpc;
import io.dapr.DaprProtos;
import io.dapr.client.domain.StateKeyValue;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.Verb;
import io.dapr.utils.ObjectSerializer;
import reactor.core.publisher.Mono;

import java.io.IOException;
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
    return this.publishEvent(topic, event, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<Void> publishEvent(String topic, T event, Map<String, String> metadata) {
    try {
      byte[] byteEvent = objectSerializer.serialize(event);
      Any data = Any.newBuilder().setValue(ByteString.copyFrom(byteEvent)).build();
      // TODO: handle metadata.

      DaprProtos.PublishEventEnvelope envelope = DaprProtos.PublishEventEnvelope.newBuilder()
          .setTopic(topic).setData(data).build();
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
  public <T, R> Mono<T> invokeService(Verb verb, String appId, String method, R request, Map<String, String> metadata, Class<T> clazz) {
    try {
      DaprProtos.InvokeServiceEnvelope envelope = buildInvokeServiceEnvelope(verb.toString(), appId, method, request);
      ListenableFuture<DaprProtos.InvokeServiceResponseEnvelope> futureResponse =
          client.invokeService(envelope);
      return Mono.just(futureResponse).flatMap(f -> {
        try {
          return Mono.just(objectSerializer.deserialize(f.get().getData().getValue().toByteArray(), clazz));
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
  public <T> Mono<T> invokeService(Verb verb, String appId, String method, Map<String, String> metadata, Class<T> clazz) {
    return this.invokeService(verb, appId, method, null, null, clazz);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <R> Mono<Void> invokeService(Verb verb, String appId, String method, R request, Map<String, String> metadata) {
    return this.invokeService(verb, appId, method, request, metadata, byte[].class).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeService(Verb verb, String appId, String method, Map<String, String> metadata) {
    return this.invokeService(verb, appId, method, null, metadata, byte[].class).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<byte[]> invokeService(Verb verb, String appId, String method, byte[] request, Map<String, String> metadata) {
    return this.invokeService(verb, appId, method, request, metadata, byte[].class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<Void> invokeBinding(String name, T request) {
    try {
      byte[] byteRequest = objectSerializer.serialize(request);
      Any data = Any.newBuilder().setValue(ByteString.copyFrom(byteRequest)).build();
      DaprProtos.InvokeBindingEnvelope.Builder builder = DaprProtos.InvokeBindingEnvelope.newBuilder()
          .setName(name)
          .setData(data);
      DaprProtos.InvokeBindingEnvelope envelope = builder.build();
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
  public <T, K> Mono<T> getState(StateKeyValue<K> key, StateOptions stateOptions, Class<T> clazz) {
    try {
      DaprProtos.GetStateEnvelope.Builder builder = DaprProtos.GetStateEnvelope.newBuilder()
          .setKey(key.getKey())
          .setConsistency(stateOptions.getConsistency().getValue());
      DaprProtos.GetStateEnvelope envelope = builder.build();
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
  public <T> Mono<Void> saveStates(List<StateKeyValue<T>> states, StateOptions options) {
    try {
      DaprProtos.StateRequestOptions.Builder optionBuilder = null;
      if (options != null) {
        DaprProtos.StateRetryPolicy.Builder retryPolicyBuilder = null;
        if (options.getRetryPolicy() != null) {
          retryPolicyBuilder = DaprProtos.StateRetryPolicy.newBuilder();
          StateOptions.RetryPolicy retryPolicy = options.getRetryPolicy();
          if (options.getRetryPolicy().getInterval() != null) {
            Duration.Builder durationBuilder = Duration.newBuilder()
                .setNanos(retryPolicy.getInterval().getNano())
                .setSeconds(retryPolicy.getInterval().getSeconds());
            retryPolicyBuilder.setInterval(durationBuilder.build());
          }
          retryPolicyBuilder.setThreshold(objectSerializer.deserialize(retryPolicy.getThreshold(), int.class));
          if (retryPolicy.getPattern() != null) {
            retryPolicyBuilder.setPattern(retryPolicy.getPattern().getValue());
          }
        }

        optionBuilder = DaprProtos.StateRequestOptions.newBuilder();
        if (options.getConcurrency() != null) {
          optionBuilder.setConcurrency(options.getConcurrency().getValue());
        }
        if (options.getConsistency() != null) {
          optionBuilder.setConsistency(options.getConsistency().getValue());
        }
        if (retryPolicyBuilder != null) {
          optionBuilder.setRetryPolicy(retryPolicyBuilder.build());
        }
      }
      DaprProtos.SaveStateEnvelope.Builder builder = DaprProtos.SaveStateEnvelope.newBuilder();
      for (StateKeyValue state : states) {
        byte[] byteState = objectSerializer.serialize(state.getValue());
        Any data = Any.newBuilder().setValue(ByteString.copyFrom(byteState)).build();
        DaprProtos.StateRequest.Builder stateBuilder = DaprProtos.StateRequest.newBuilder()
            .setEtag(state.getEtag())
            .setKey(state.getKey())
            .setValue(data);
        if(optionBuilder != null) {
          stateBuilder.setOptions(optionBuilder.build());
        }
        builder.addRequests(stateBuilder.build());
      }
      DaprProtos.SaveStateEnvelope envelope = builder.build();

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
  public <T> Mono<Void> saveState(String key, String etag, T value, StateOptions options) {
    StateKeyValue<T> state = new StateKeyValue<>(value, key, etag);
    return saveStates(Arrays.asList(state), options);
  }

  /**
   * if stateOptions param is passed it will overrside state.options.
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<Void> deleteState(StateKeyValue<T> state, StateOptions options) {
    try {
      DaprProtos.StateOptions.Builder optionBuilder =  null;

      if (options != null) {
        optionBuilder = DaprProtos.StateOptions.newBuilder();
        DaprProtos.RetryPolicy.Builder retryPolicyBuilder = null;
        if (options.getRetryPolicy() != null) {
          retryPolicyBuilder = DaprProtos.RetryPolicy.newBuilder();
          StateOptions.RetryPolicy retryPolicy = options.getRetryPolicy();
          if (options.getRetryPolicy().getInterval() != null) {
            Duration.Builder durationBuilder = Duration.newBuilder()
                .setNanos(retryPolicy.getInterval().getNano())
                .setSeconds(retryPolicy.getInterval().getSeconds());
            retryPolicyBuilder.setInterval(durationBuilder.build());
          }
          retryPolicyBuilder.setThreshold(objectSerializer.deserialize(retryPolicy.getThreshold(), int.class));
          if (retryPolicy.getPattern() != null) {
            retryPolicyBuilder.setPattern(retryPolicy.getPattern().getValue());
          }
        }

        optionBuilder = DaprProtos.StateOptions.newBuilder();
        if (options.getConcurrency() != null) {
          optionBuilder.setConcurrency(options.getConcurrency().getValue());
        }
        if (options.getConsistency() != null) {
          optionBuilder.setConsistency(options.getConsistency().getValue());
        }
        if (retryPolicyBuilder != null) {
          optionBuilder.setRetryPolicy(retryPolicyBuilder.build());
        }
      }
      DaprProtos.DeleteStateEnvelope.Builder builder = DaprProtos.DeleteStateEnvelope.newBuilder()
          .setEtag(state.getEtag())
          .setKey(state.getKey());
      if (optionBuilder != null) {
        builder.setOptions(optionBuilder.build());
      }

      DaprProtos.DeleteStateEnvelope envelope = builder.build();
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
   *
   * @throws UnsupportedOperationException every time is called.
   */
  @Override
  public Mono<String> invokeActorMethod(String actorType, String actorId, String methodName, String jsonPayload) {
    return Mono.error(new UnsupportedOperationException("Operation not supported for GRPC"));
  }

  /**
   * Operation not supported for GRPC
   *
   * @throws UnsupportedOperationException every time is called.
   */
  @Override
  public Mono<String> getActorState(String actorType, String actorId, String keyName) {
    return Mono.error(new UnsupportedOperationException("Operation not supported for GRPC"));
  }

  /**
   * Operation not supported for GRPC
   *
   * @throws UnsupportedOperationException every time is called.
   */
  @Override
  public Mono<Void> saveActorStateTransactionally(String actorType, String actorId, String data) {
    return Mono.error(new UnsupportedOperationException("Operation not supported for GRPC"));
  }

  /**
   * Operation not supported for GRPC
   *
   * @throws UnsupportedOperationException every time is called.
   */
  @Override
  public Mono<Void> registerActorReminder(String actorType, String actorId, String reminderName, String data) {
    return Mono.error(new UnsupportedOperationException("Operation not supported for GRPC"));
  }

  /**
   * Operation not supported for GRPC
   *
   * @throws UnsupportedOperationException every time is called.
   */
  @Override
  public Mono<Void> unregisterActorReminder(String actorType, String actorId, String reminderName) {
    return Mono.error(new UnsupportedOperationException("Operation not supported for GRPC"));
  }

  /**
   * Operation not supported for GRPC
   *
   * @throws UnsupportedOperationException every time is called.
   */
  @Override
  public Mono<Void> registerActorTimer(String actorType, String actorId, String timerName, String data) {
    return Mono.error(new UnsupportedOperationException("Operation not supported for GRPC"));
  }

  /**
   * Operation not supported for GRPC
   *
   * @throws UnsupportedOperationException every time is called.
   */
  @Override
  public Mono<Void> unregisterActorTimer(String actorType, String actorId, String timerName) {
    return Mono.error(new UnsupportedOperationException("Operation not supported for GRPC"));
  }

  /**
   * Builds the object io.dapr.{@link DaprProtos.InvokeServiceEnvelope} to be send based on the parameters.
   * @param verb
   * @param appId
   * @param method
   * @param request
   * @param <K>
   * @return
   * @throws IOException
   */
  private <K> DaprProtos.InvokeServiceEnvelope buildInvokeServiceEnvelope(
      String verb, String appId, String method, K request) throws IOException {
    byte[] byteRequest = objectSerializer.serialize(request);
    Any data = Any.newBuilder().setValue(ByteString.copyFrom(byteRequest)).build();
    DaprProtos.InvokeServiceEnvelope.Builder envelopeBuilder = DaprProtos.InvokeServiceEnvelope.newBuilder()
        .setId(appId)
        .setMethod(verb)
        .setData(data);
    return envelopeBuilder.build();
  }

}