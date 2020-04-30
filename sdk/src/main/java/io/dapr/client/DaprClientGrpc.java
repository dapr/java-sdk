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
import io.dapr.client.domain.State;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.Verb;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.v1.CommonProtos;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprProtos;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * An adapter for the GRPC Client.
 *
 * @see io.dapr.v1.DaprGrpc
 * @see io.dapr.client.DaprClient
 */
public class DaprClientGrpc implements DaprClient {

  /**
   * The GRPC client to be used.
   *
   * @see io.dapr.v1.DaprGrpc.DaprFutureStub
   */
  private DaprGrpc.DaprFutureStub client;

  /**
   * A utitlity class for serialize and deserialize the transient objects.
   */
  private DaprObjectSerializer objectSerializer;

  /**
   * A utitlity class for serialize and deserialize state objects.
   */
  private DaprObjectSerializer stateSerializer;

  /**
   * Default access level constructor, in order to create an instance of this class use io.dapr.client.DaprClientBuilder
   *
   * @param futureClient     GRPC client
   * @param objectSerializer Serializer for transient request/response objects.
   * @param stateSerializer  Serializer for state objects.
   * @see DaprClientBuilder
   */
  DaprClientGrpc(
      DaprGrpc.DaprFutureStub futureClient,
      DaprObjectSerializer objectSerializer,
      DaprObjectSerializer stateSerializer) {
    this.client = futureClient;
    this.objectSerializer = objectSerializer;
    this.stateSerializer = stateSerializer;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> publishEvent(String topic, Object event) {
    return this.publishEvent(topic, event, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> publishEvent(String topic, Object event, Map<String, String> metadata) {
    try {
      byte[] byteEvent = objectSerializer.serialize(event);
      Any data = Any.newBuilder().setValue(ByteString.copyFrom(byteEvent)).build();
      // TODO: handle metadata.

      DaprProtos.PublishEventEnvelope envelope = DaprProtos.PublishEventEnvelope.newBuilder()
          .setTopic(topic).setData(data).build();

      return Mono.fromCallable(() -> {
        ListenableFuture<Empty> futureEmpty = client.publishEvent(envelope);
        futureEmpty.get();
        return null;
      });
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeService(
      Verb verb,
      String appId,
      String method,
      Object request,
      Map<String, String> metadata,
      Class<T> clazz) {
    try {
      DaprProtos.InvokeServiceRequest envelope = buildInvokeServiceRequest(verb.toString(), appId, method, request);
      return Mono.fromCallable(() -> {
        ListenableFuture<CommonProtos.InvokeResponse> futureResponse =
            client.invokeService(envelope);

        return objectSerializer.deserialize(futureResponse.get().getData().getValue().toByteArray(), clazz);
      });
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeService(
      Verb verb, String appId, String method, Map<String, String> metadata, Class<T> clazz) {
    return this.invokeService(verb, appId, method, null, metadata, clazz);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeService(Verb verb, String appId, String method, Object request, Class<T> clazz) {
    return this.invokeService(verb, appId, method, request, null, clazz);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeService(Verb verb, String appId, String method, Object request) {
    return this.invokeService(verb, appId, method, request, null, byte[].class).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeService(
      Verb verb, String appId, String method, Object request, Map<String, String> metadata) {
    return this.invokeService(verb, appId, method, request, metadata, byte[].class).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeService(
      Verb verb, String appId, String method, Map<String, String> metadata) {
    return this.invokeService(verb, appId, method, null, metadata, byte[].class).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<byte[]> invokeService(
      Verb verb, String appId, String method, byte[] request, Map<String, String> metadata) {
    return this.invokeService(verb, appId, method, request, metadata, byte[].class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeBinding(String name, Object request) {
    return this.invokeBinding(name, request, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeBinding(String name, Object request, Map<String, String> metadata) {
    try {
      byte[] byteRequest = objectSerializer.serialize(request);
      DaprProtos.InvokeBindingEnvelope.Builder builder = DaprProtos.InvokeBindingEnvelope.newBuilder()
          .setName(name);
      if (byteRequest != null) {
        Any data = Any.newBuilder().setValue(ByteString.copyFrom(byteRequest)).build();
        builder.setData(data);
      }
      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }
      DaprProtos.InvokeBindingEnvelope envelope = builder.build();
      return Mono.fromCallable(() -> {
        ListenableFuture<Empty> futureEmpty = client.invokeBinding(envelope);
        futureEmpty.get();
        return null;
      });
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(String stateStoreName, State<T> state, Class<T> clazz) {
    return this.getState(stateStoreName, state.getKey(), state.getEtag(), state.getOptions(), clazz);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(String stateStoreName, String key, Class<T> clazz) {
    return this.getState(stateStoreName, key, null, null, clazz);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(
      String stateStoreName, String key, String etag, StateOptions options, Class<T> clazz) {
    try {
      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      if ((key == null) || (key.trim().isEmpty())) {
        throw new IllegalArgumentException("Key cannot be null or empty.");
      }
      DaprProtos.GetStateEnvelope.Builder builder = DaprProtos.GetStateEnvelope.newBuilder()
          .setStoreName(stateStoreName)
          .setKey(key);
      if (options != null && options.getConsistency() != null) {
        builder.setConsistency(options.getConsistency().getValue());
      }

      DaprProtos.GetStateEnvelope envelope = builder.build();
      return Mono.fromCallable(() -> {
        ListenableFuture<DaprProtos.GetStateResponseEnvelope> futureResponse = client.getState(envelope);
        DaprProtos.GetStateResponseEnvelope response = null;
        try {
          response = futureResponse.get();
        } catch (NullPointerException npe) {
          return null;
        }
        return buildStateKeyValue(response, key, options, clazz);
      });
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  private <T> State<T> buildStateKeyValue(
      DaprProtos.GetStateResponseEnvelope response,
      String requestedKey,
      StateOptions stateOptions,
      Class<T> clazz) throws IOException {
    ByteString payload = response.getData().getValue();
    byte[] data = payload == null ? null : payload.toByteArray();
    T value = stateSerializer.deserialize(data, clazz);
    String etag = response.getEtag();
    String key = requestedKey;
    return new State<>(value, key, etag, stateOptions);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> saveStates(String stateStoreName, List<State<?>> states) {
    try {
      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      DaprProtos.SaveStateEnvelope.Builder builder = DaprProtos.SaveStateEnvelope.newBuilder();
      builder.setStoreName(stateStoreName);
      for (State state : states) {
        builder.addRequests(buildStateRequest(state).build());
      }
      DaprProtos.SaveStateEnvelope envelope = builder.build();

      return Mono.fromCallable(() -> client.saveState(envelope)).flatMap(f -> {
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

  private <T> DaprProtos.StateRequest.Builder buildStateRequest(State<T> state) throws IOException {
    byte[] bytes = stateSerializer.serialize(state.getValue());
    Any data = Any.newBuilder().setValue(ByteString.copyFrom(bytes)).build();
    DaprProtos.StateRequest.Builder stateBuilder = DaprProtos.StateRequest.newBuilder();
    if (state.getEtag() != null) {
      stateBuilder.setEtag(state.getEtag());
    }
    if (data != null) {
      stateBuilder.setValue(data);
    }
    stateBuilder.setKey(state.getKey());
    DaprProtos.StateOptions.Builder optionBuilder = null;
    if (state.getOptions() != null) {
      StateOptions options = state.getOptions();
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
        if (retryPolicy.getThreshold() != null) {
          retryPolicyBuilder.setThreshold(retryPolicy.getThreshold());
        }
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
    if (optionBuilder != null) {
      stateBuilder.setOptions(optionBuilder.build());
    }
    return stateBuilder;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> saveState(String stateStoreName, String key, Object value) {
    return this.saveState(stateStoreName, key, null, value, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> saveState(String stateStoreName, String key, String etag, Object value, StateOptions options) {
    State<?> state = new State<>(value, key, etag, options);
    return this.saveStates(stateStoreName, Arrays.asList(state));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> deleteState(String stateStoreName, String key) {
    return this.deleteState(stateStoreName, key, null, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> deleteState(String stateStoreName, String key, String etag, StateOptions options) {
    try {
      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      if ((key == null) || (key.trim().isEmpty())) {
        throw new IllegalArgumentException("Key cannot be null or empty.");
      }

      DaprProtos.StateOptions.Builder optionBuilder = null;
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
          if (retryPolicy.getThreshold() != null) {
            retryPolicyBuilder.setThreshold(retryPolicy.getThreshold());
          }
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
          .setStoreName(stateStoreName)
          .setKey(key);
      if (etag != null) {
        builder.setEtag(etag);
      }

      if (optionBuilder != null) {
        builder.setOptions(optionBuilder.build());
      }

      DaprProtos.DeleteStateEnvelope envelope = builder.build();
      return Mono.fromCallable(() -> client.deleteState(envelope)).flatMap(f -> {
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
   * Builds the object io.dapr.{@link DaprProtos.InvokeServiceRequest} to be send based on the parameters.
   *
   * @param verb    String that must match HTTP Methods
   * @param appId   The application id to be invoked
   * @param method  The application method to be invoked
   * @param request The body of the request to be send as part of the invokation
   * @param <K>     The Type of the Body
   * @return The object to be sent as part of the invokation.
   * @throws IOException If there's an issue serializing the request.
   */
  private <K> DaprProtos.InvokeServiceRequest buildInvokeServiceRequest(
      String verb, String appId, String method, K request) throws IOException {
    CommonProtos.InvokeRequest.Builder requestBuilder = CommonProtos.InvokeRequest.newBuilder();
    requestBuilder.setMethod(method);
    if (request != null) {
      byte[] byteRequest = objectSerializer.serialize(request);
      Any data = Any.newBuilder().setValue(ByteString.copyFrom(byteRequest)).build();
      requestBuilder.setData(data);
    } else {
      requestBuilder.setData(Any.newBuilder().build());
    }

    CommonProtos.HTTPExtension.Builder httpExtensionBuilder = CommonProtos.HTTPExtension.newBuilder();
    if ((verb != null) && !verb.isEmpty()) {
      httpExtensionBuilder.setVerb(CommonProtos.HTTPExtension.Verb.valueOf(verb.toUpperCase()));
    } else {
      httpExtensionBuilder.setVerb(CommonProtos.HTTPExtension.Verb.NONE);
    }
    requestBuilder.setHttpExtension(httpExtensionBuilder.build());

    DaprProtos.InvokeServiceRequest.Builder envelopeBuilder = DaprProtos.InvokeServiceRequest.newBuilder()
        .setId(appId)
        .setMessage(requestBuilder.build());
    return envelopeBuilder.build();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Map<String, String>> getSecret(String secretStoreName, String secretName, Map<String, String> metadata) {
    try {
      if ((secretStoreName == null) || (secretStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("Secret store name cannot be null or empty.");
      }
      if ((secretName == null) || (secretName.trim().isEmpty())) {
        throw new IllegalArgumentException("Secret name cannot be null or empty.");
      }
    } catch (Exception e) {
      return Mono.error(e);
    }

    DaprProtos.GetSecretEnvelope.Builder envelopeBuilder = DaprProtos.GetSecretEnvelope.newBuilder()
          .setStoreName(secretStoreName)
          .setKey(secretName);

    if (metadata != null) {
      envelopeBuilder.putAllMetadata(metadata);
    }
    return Mono.fromCallable(() -> {
      DaprProtos.GetSecretEnvelope envelope = envelopeBuilder.build();
      ListenableFuture<DaprProtos.GetSecretResponseEnvelope> future = client.getSecret(envelope);
      return future.get();
    }).map(future -> future.getDataMap());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Map<String, String>> getSecret(String secretStoreName, String secretName) {
    return this.getSecret(secretStoreName, secretName, null);
  }

}