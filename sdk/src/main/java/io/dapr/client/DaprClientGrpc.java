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
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.utils.TypeRef;
import io.dapr.v1.CommonProtos;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprProtos;
import reactor.core.publisher.Mono;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.dapr.client.domain.StateOptions.RetryPolicy;

/**
 * An adapter for the GRPC Client.
 *
 * @see io.dapr.v1.DaprGrpc
 * @see io.dapr.client.DaprClient
 */
public class DaprClientGrpc implements DaprClient {

  /**
   * The GRPC managed channel to be used.
   */
  private Closeable channel;

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
   * @param closeableChannel A closeable for a Managed GRPC channel
   * @param futureClient     GRPC client
   * @param objectSerializer Serializer for transient request/response objects.
   * @param stateSerializer  Serializer for state objects.
   * @see DaprClientBuilder
   */
  DaprClientGrpc(
      Closeable closeableChannel,
      DaprGrpc.DaprFutureStub futureClient,
      DaprObjectSerializer objectSerializer,
      DaprObjectSerializer stateSerializer) {
    this.channel = closeableChannel;
    this.client = futureClient;
    this.objectSerializer = objectSerializer;
    this.stateSerializer = stateSerializer;
  }

  private CommonProtos.StateOptions.StateConsistency getGrpcStateConsistency(StateOptions options) {
    switch (options.getConsistency()) {
      case EVENTUAL:
        return CommonProtos.StateOptions.StateConsistency.CONSISTENCY_EVENTUAL;
      case STRONG:
        return CommonProtos.StateOptions.StateConsistency.CONSISTENCY_STRONG;
      default:
        throw new IllegalArgumentException("Missing Consistency mapping to gRPC Consistency enum");
    }
  }

  private CommonProtos.StateOptions.StateConcurrency getGrpcStateConcurrency(StateOptions options) {
    switch (options.getConcurrency()) {
      case FIRST_WRITE:
        return CommonProtos.StateOptions.StateConcurrency.CONCURRENCY_FIRST_WRITE;
      case LAST_WRITE:
        return CommonProtos.StateOptions.StateConcurrency.CONCURRENCY_LAST_WRITE;
      default:
        throw new IllegalArgumentException("Missing StateConcurrency mapping to gRPC Concurrency enum");
    }
  }

  private CommonProtos.StateRetryPolicy.RetryPattern getGrpcStateRetryPolicy(RetryPolicy policy) {
    switch (policy.getPattern()) {
      case LINEAR:
        return CommonProtos.StateRetryPolicy.RetryPattern.RETRY_LINEAR;
      case EXPONENTIAL:
        return CommonProtos.StateRetryPolicy.RetryPattern.RETRY_EXPONENTIAL;
      default:
        throw new IllegalArgumentException("Missing RetryPattern mapping to gRPC retry pattern enum");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> publishEvent(String topic, Object data) {
    return this.publishEvent(topic, data, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> publishEvent(String topic, Object data, Map<String, String> metadata) {
    try {
      // TODO: handle metadata.
      DaprProtos.PublishEventRequest envelope = DaprProtos.PublishEventRequest.newBuilder()
          .setTopic(topic).setData(ByteString.copyFrom(objectSerializer.serialize(data))).build();

      return Mono.fromCallable(() -> {
        ListenableFuture<Empty> futureEmpty = client.publishEvent(envelope);
        futureEmpty.get();
        return null;
      });
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  @Override
  public <T> Mono<T> invokeService(String appId, String method, Object request, HttpExtension httpExtension,
                                   Map<String, String> metadata, TypeRef<T> type) {
    try {
      DaprProtos.InvokeServiceRequest envelope = buildInvokeServiceRequest(httpExtension, appId, method, request);
      return Mono.fromCallable(() -> {
        ListenableFuture<CommonProtos.InvokeResponse> futureResponse =
            client.invokeService(envelope);

        return objectSerializer.deserialize(futureResponse.get().getData().getValue().toByteArray(), type);
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
      String appId,
      String method,
      Object request,
      HttpExtension httpExtension,
      Map<String, String> metadata,
      Class<T> clazz) {
    return this.invokeService(appId, method, request, httpExtension, metadata, TypeRef.get(clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeService(
      String appId, String method, HttpExtension httpExtension, Map<String, String> metadata, TypeRef<T> type) {
    return this.invokeService(appId, method, null, httpExtension, metadata, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeService(
      String appId, String method, HttpExtension httpExtension, Map<String, String> metadata, Class<T> clazz) {
    return this.invokeService(appId, method, null, httpExtension, metadata, TypeRef.get(clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeService(String appId, String method, Object request, HttpExtension httpExtension,
                                   TypeRef<T> type) {
    return this.invokeService(appId, method, request, httpExtension, null, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeService(String appId, String method, Object request, HttpExtension httpExtension,
                                   Class<T> clazz) {
    return this.invokeService(appId, method, request, httpExtension, null, TypeRef.get(clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeService(String appId, String method, Object request, HttpExtension httpExtension) {
    return this.invokeService(appId, method, request, httpExtension, null, TypeRef.BYTE_ARRAY).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeService(
      String appId, String method, Object request, HttpExtension httpExtension, Map<String, String> metadata) {
    return this.invokeService(appId, method, request, httpExtension, metadata, TypeRef.BYTE_ARRAY).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeService(
      String appId, String method, HttpExtension httpExtension, Map<String, String> metadata) {
    return this.invokeService(appId, method, null, httpExtension, metadata, TypeRef.BYTE_ARRAY).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<byte[]> invokeService(
      String appId, String method, byte[] request, HttpExtension httpExtension, Map<String, String> metadata) {
    return this.invokeService(appId, method, request, httpExtension, metadata, TypeRef.BYTE_ARRAY);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeBinding(String name, String operation, Object data) {
    return this.invokeBinding(name, operation, data, null, TypeRef.BYTE_ARRAY).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<byte[]> invokeBinding(String name, String operation, byte[] data, Map<String, String> metadata) {
    return this.invokeBinding(name, operation, data, metadata, TypeRef.BYTE_ARRAY);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeBinding(String name, String operation, Object data, TypeRef<T> type) {
    return this.invokeBinding(name, operation, data, null, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeBinding(String name, String operation, Object data, Class<T> clazz) {
    return this.invokeBinding(name, operation, data, null, TypeRef.get(clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeBinding(
      String name, String operation, Object data, Map<String, String> metadata, TypeRef<T> type) {
    try {
      if (name == null || name.trim().isEmpty()) {
        throw new IllegalArgumentException("Binding name cannot be null or empty.");
      }

      if (operation == null || operation.trim().isEmpty()) {
        throw new IllegalArgumentException("Binding operation cannot be null or empty.");
      }

      byte[] byteData = objectSerializer.serialize(data);
      DaprProtos.InvokeBindingRequest.Builder builder = DaprProtos.InvokeBindingRequest.newBuilder()
          .setName(name).setOperation(operation);
      if (byteData != null) {
        builder.setData(ByteString.copyFrom(byteData));
      }
      if (metadata != null) {
        builder.putAllMetadata(metadata);
      }
      DaprProtos.InvokeBindingRequest envelope = builder.build();
      return Mono.fromCallable(() -> {
        ListenableFuture<DaprProtos.InvokeBindingResponse> futureResponse = client.invokeBinding(envelope);
        return objectSerializer.deserialize(futureResponse.get().getData().toByteArray(), type);
      });
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeBinding(
      String name, String operation, Object data, Map<String, String> metadata, Class<T> clazz) {
    return this.invokeBinding(name, operation, data, metadata, TypeRef.get(clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(String stateStoreName, State<T> state, TypeRef<T> type) {
    return this.getState(stateStoreName, state.getKey(), state.getEtag(), state.getOptions(), type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(String stateStoreName, State<T> state, Class<T> clazz) {
    return this.getState(stateStoreName, state.getKey(), state.getEtag(), state.getOptions(), TypeRef.get(clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(String stateStoreName, String key, TypeRef<T> type) {
    return this.getState(stateStoreName, key, null, null, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(String stateStoreName, String key, Class<T> clazz) {
    return this.getState(stateStoreName, key, null, null, TypeRef.get(clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(
      String stateStoreName, String key, String etag, StateOptions options, TypeRef<T> type) {
    try {
      if ((stateStoreName == null) || (stateStoreName.trim().isEmpty())) {
        throw new IllegalArgumentException("State store name cannot be null or empty.");
      }
      if ((key == null) || (key.trim().isEmpty())) {
        throw new IllegalArgumentException("Key cannot be null or empty.");
      }
      DaprProtos.GetStateRequest.Builder builder = DaprProtos.GetStateRequest.newBuilder()
          .setStoreName(stateStoreName)
          .setKey(key);
      if (options != null && options.getConsistency() != null) {
        builder.setConsistency(getGrpcStateConsistency(options));
      }

      DaprProtos.GetStateRequest envelope = builder.build();
      return Mono.fromCallable(() -> {
        ListenableFuture<DaprProtos.GetStateResponse> futureResponse = client.getState(envelope);
        DaprProtos.GetStateResponse response = null;
        try {
          response = futureResponse.get();
        } catch (NullPointerException npe) {
          return null;
        }
        return buildStateKeyValue(response, key, options, type);
      });
    } catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(
      String stateStoreName, String key, String etag, StateOptions options, Class<T> clazz) {
    return this.getState(stateStoreName, key, etag, options, TypeRef.get(clazz));
  }

  private <T> State<T> buildStateKeyValue(
      DaprProtos.GetStateResponse response,
      String requestedKey,
      StateOptions stateOptions,
      TypeRef<T> type) throws IOException {
    ByteString payload = response.getData();
    byte[] data = payload == null ? null : payload.toByteArray();
    T value = stateSerializer.deserialize(data, type);
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
      DaprProtos.SaveStateRequest.Builder builder = DaprProtos.SaveStateRequest.newBuilder();
      builder.setStoreName(stateStoreName);
      for (State state : states) {
        builder.addStates(buildStateRequest(state).build());
      }
      DaprProtos.SaveStateRequest request = builder.build();

      return Mono.fromCallable(() -> client.saveState(request)).flatMap(f -> {
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

  private <T> CommonProtos.StateItem.Builder buildStateRequest(State<T> state) throws IOException {
    byte[] bytes = stateSerializer.serialize(state.getValue());
    ByteString data = ByteString.copyFrom(bytes);
    CommonProtos.StateItem.Builder stateBuilder = CommonProtos.StateItem.newBuilder();
    if (state.getEtag() != null) {
      stateBuilder.setEtag(state.getEtag());
    }
    if (data != null) {
      stateBuilder.setValue(data);
    }
    stateBuilder.setKey(state.getKey());
    CommonProtos.StateOptions.Builder optionBuilder = null;
    if (state.getOptions() != null) {
      StateOptions options = state.getOptions();
      CommonProtos.StateRetryPolicy.Builder retryPolicyBuilder = null;
      if (options.getRetryPolicy() != null) {
        retryPolicyBuilder = CommonProtos.StateRetryPolicy.newBuilder();
        RetryPolicy retryPolicy = options.getRetryPolicy();
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
          retryPolicyBuilder.setPattern(getGrpcStateRetryPolicy(retryPolicy));
        }
      }

      optionBuilder = CommonProtos.StateOptions.newBuilder();
      if (options.getConcurrency() != null) {
        optionBuilder.setConcurrency(getGrpcStateConcurrency(options));
      }
      if (options.getConsistency() != null) {
        optionBuilder.setConsistency(getGrpcStateConsistency(options));
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

      CommonProtos.StateOptions.Builder optionBuilder = null;
      if (options != null) {
        optionBuilder = CommonProtos.StateOptions.newBuilder();
        CommonProtos.StateRetryPolicy.Builder retryPolicyBuilder = null;
        if (options.getRetryPolicy() != null) {
          retryPolicyBuilder = CommonProtos.StateRetryPolicy.newBuilder();
          RetryPolicy retryPolicy = options.getRetryPolicy();
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
            retryPolicyBuilder.setPattern(getGrpcStateRetryPolicy(retryPolicy));
          }
        }

        optionBuilder = CommonProtos.StateOptions.newBuilder();
        if (options.getConcurrency() != null) {
          optionBuilder.setConcurrency(getGrpcStateConcurrency(options));
        }
        if (options.getConsistency() != null) {
          optionBuilder.setConsistency(getGrpcStateConsistency(options));
        }
        if (retryPolicyBuilder != null) {
          optionBuilder.setRetryPolicy(retryPolicyBuilder.build());
        }
      }
      DaprProtos.DeleteStateRequest.Builder builder = DaprProtos.DeleteStateRequest.newBuilder()
          .setStoreName(stateStoreName)
          .setKey(key);
      if (etag != null) {
        builder.setEtag(etag);
      }

      if (optionBuilder != null) {
        builder.setOptions(optionBuilder.build());
      }

      DaprProtos.DeleteStateRequest request = builder.build();
      return Mono.fromCallable(() -> client.deleteState(request)).flatMap(f -> {
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
   * @param httpExtension Object for HttpExtension
   * @param appId         The application id to be invoked
   * @param method        The application method to be invoked
   * @param request       The body of the request to be send as part of the invocation
   * @param <K>           The Type of the Body
   * @return The object to be sent as part of the invocation.
   * @throws IOException If there's an issue serializing the request.
   */
  private <K> DaprProtos.InvokeServiceRequest buildInvokeServiceRequest(
      HttpExtension httpExtension, String appId, String method, K request) throws IOException {
    if (httpExtension == null) {
      throw new IllegalArgumentException("HttpExtension cannot be null. Use HttpExtension.NONE instead.");
    }
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
    httpExtensionBuilder.setVerb(CommonProtos.HTTPExtension.Verb.valueOf(httpExtension.getMethod().toString()))
        .putAllQuerystring(httpExtension.getQueryString());
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

    DaprProtos.GetSecretRequest.Builder requestBuilder = DaprProtos.GetSecretRequest.newBuilder()
          .setStoreName(secretStoreName)
          .setKey(secretName);

    if (metadata != null) {
      requestBuilder.putAllMetadata(metadata);
    }
    return Mono.fromCallable(() -> {
      DaprProtos.GetSecretRequest request = requestBuilder.build();
      ListenableFuture<DaprProtos.GetSecretResponse> future = client.getSecret(request);
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

  /**
   * Closes the ManagedChannel for GRPC.
   * @see io.grpc.ManagedChannel#shutdown()
   * @throws IOException on exception.
   */
  @Override
  public void close() throws IOException {
    if (channel != null) {
      channel.close();
    }
  }
}