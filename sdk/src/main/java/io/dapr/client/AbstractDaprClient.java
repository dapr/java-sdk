/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.dapr.client.domain.DeleteStateRequest;
import io.dapr.client.domain.DeleteStateRequestBuilder;
import io.dapr.client.domain.ExecuteStateTransactionRequest;
import io.dapr.client.domain.ExecuteStateTransactionRequestBuilder;
import io.dapr.client.domain.GetSecretRequest;
import io.dapr.client.domain.GetSecretRequestBuilder;
import io.dapr.client.domain.GetStateRequest;
import io.dapr.client.domain.GetStateRequestBuilder;
import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.InvokeBindingRequest;
import io.dapr.client.domain.InvokeBindingRequestBuilder;
import io.dapr.client.domain.InvokeServiceRequest;
import io.dapr.client.domain.InvokeServiceRequestBuilder;
import io.dapr.client.domain.PublishEventRequest;
import io.dapr.client.domain.PublishEventRequestBuilder;
import io.dapr.client.domain.SaveStateRequest;
import io.dapr.client.domain.SaveStateRequestBuilder;
import io.dapr.client.domain.State;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.TransactionalStateOperation;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.utils.TypeRef;
import io.dapr.v1.CommonProtos;
import io.dapr.v1.DaprProtos;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abstract class with convenient methods common between client implementations.
 *
 * @see io.dapr.client.DaprClient
 * @see io.dapr.client.DaprClientGrpc
 * @see io.dapr.client.DaprClientHttp
 */
abstract class AbstractDaprClient implements DaprClient {

  /**
   * A utility class for serialize and deserialize the transient objects.
   */
  protected DaprObjectSerializer objectSerializer;

  /**
   * A utility class for serialize and deserialize state objects.
   */
  protected DaprObjectSerializer stateSerializer;

  /**
   * Common constructor for implementations of this class.
   *
   * @param objectSerializer Serializer for transient request/response objects.
   * @param stateSerializer  Serializer for state objects.
   * @see DaprClientBuilder
   */
  AbstractDaprClient(
      DaprObjectSerializer objectSerializer,
      DaprObjectSerializer stateSerializer) {
    this.objectSerializer = objectSerializer;
    this.stateSerializer = stateSerializer;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> publishEvent(String pubsubName, String topic, Object data) {
    return this.publishEvent(pubsubName, topic, data, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> publishEvent(String pubsubName, String topic, Object data, Map<String, String> metadata) {
    PublishEventRequest req = new PublishEventRequestBuilder(pubsubName, topic, data).withMetadata(metadata).build();
    return this.publishEvent(req).then();
  }

  /**
   * {@inheritDoc}
   */
  public <T> Mono<T> invokeService(
      String appId,
      String method,
      Object request,
      HttpExtension httpExtension,
      Map<String, String> metadata,
      TypeRef<T> type) {
    InvokeServiceRequestBuilder builder = new InvokeServiceRequestBuilder(appId, method);
    InvokeServiceRequest req = builder
        .withBody(request)
        .withHttpExtension(httpExtension)
        .withMetadata(metadata)
        .build();

    return this.invokeService(req, type).map(r -> r.getObject());
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
    InvokeBindingRequest request = new InvokeBindingRequestBuilder(name, operation)
        .withData(data)
        .withMetadata(metadata)
        .build();
    return this.invokeBinding(request, type).map(r -> r.getObject());
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
    GetStateRequest request = new GetStateRequestBuilder(stateStoreName, key)
        .withEtag(etag)
        .withStateOptions(options)
        .build();
    return this.getState(request, type).map(r -> r.getObject());
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
  public Mono<Void> executeTransaction(String stateStoreName,
                                       List<TransactionalStateOperation<?>> operations) {
    ExecuteStateTransactionRequest request = new ExecuteStateTransactionRequestBuilder(stateStoreName)
        .withTransactionalStates(operations)
        .build();
    return executeTransaction(request).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> saveStates(String stateStoreName, List<State<?>> states) {
    SaveStateRequest request = new SaveStateRequestBuilder(stateStoreName)
        .withStates(states)
        .build();
    return this.saveStates(request).then();
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
    DeleteStateRequest request = new DeleteStateRequestBuilder(stateStoreName, key)
        .withEtag(etag)
        .withStateOptions(options)
        .build();
    return deleteState(request).then();
  }

  /**
   * Builds the object io.dapr.{@link io.dapr.v1.DaprProtos.InvokeServiceRequest} to be send based on the parameters.
   *
   * @param httpExtension Object for HttpExtension
   * @param appId         The application id to be invoked
   * @param method        The application method to be invoked
   * @param request       The body of the request to be send as part of the invocation
   * @param <K>           The Type of the Body
   * @return The object to be sent as part of the invocation.
   * @throws java.io.IOException If there's an issue serializing the request.
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
  public Mono<Map<String, String>> getSecret(String secretStoreName, String key, Map<String, String> metadata) {
    GetSecretRequest request = new GetSecretRequestBuilder(secretStoreName, key)
        .withMetadata(metadata)
        .build();
    return getSecret(request).map(r -> r.getObject() == null ? new HashMap<>() : r.getObject());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Map<String, String>> getSecret(String secretStoreName, String secretName) {
    return this.getSecret(secretStoreName, secretName, null);
  }

}