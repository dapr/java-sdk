/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import io.dapr.client.domain.DeleteStateRequest;
import io.dapr.client.domain.DeleteStateRequestBuilder;
import io.dapr.client.domain.ExecuteStateTransactionRequest;
import io.dapr.client.domain.ExecuteStateTransactionRequestBuilder;
import io.dapr.client.domain.GetBulkSecretRequest;
import io.dapr.client.domain.GetBulkSecretRequestBuilder;
import io.dapr.client.domain.GetBulkStateRequestBuilder;
import io.dapr.client.domain.GetSecretRequest;
import io.dapr.client.domain.GetSecretRequestBuilder;
import io.dapr.client.domain.GetStateRequest;
import io.dapr.client.domain.GetStateRequestBuilder;
import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.InvokeBindingRequest;
import io.dapr.client.domain.InvokeBindingRequestBuilder;
import io.dapr.client.domain.InvokeMethodRequest;
import io.dapr.client.domain.InvokeMethodRequestBuilder;
import io.dapr.client.domain.PublishEventRequest;
import io.dapr.client.domain.PublishEventRequestBuilder;
import io.dapr.client.domain.SaveStateRequest;
import io.dapr.client.domain.SaveStateRequestBuilder;
import io.dapr.client.domain.State;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.TransactionalStateOperation;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.utils.TypeRef;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  public Mono<Void> publishEvent(String pubsubName, String topicName, Object data) {
    return this.publishEvent(pubsubName, topicName, data, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> publishEvent(String pubsubName, String topicName, Object data, Map<String, String> metadata) {
    PublishEventRequest req = new PublishEventRequestBuilder(pubsubName, topicName,
            data).withMetadata(metadata).build();
    return this.publishEvent(req).then();
  }

  /**
   * {@inheritDoc}
   */
  public <T> Mono<T> invokeMethod(
      String appId,
      String methodName,
      Object data,
      HttpExtension httpExtension,
      Map<String, String> metadata,
      TypeRef<T> type) {
    InvokeMethodRequestBuilder builder = new InvokeMethodRequestBuilder(appId, methodName);
    InvokeMethodRequest req = builder
        .withBody(data)
        .withHttpExtension(httpExtension)
        .withContentType(objectSerializer.getContentType())
        .build();

    return this.invokeMethod(req, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeMethod(
      String appId,
      String methodName,
      Object request,
      HttpExtension httpExtension,
      Map<String, String> metadata,
      Class<T> clazz) {
    return this.invokeMethod(appId, methodName, request, httpExtension, metadata, TypeRef.get(clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeMethod(
          String appId, String methodName, HttpExtension httpExtension, Map<String, String> metadata, TypeRef<T> type) {
    return this.invokeMethod(appId, methodName, null, httpExtension, metadata, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeMethod(
          String appId, String methodName, HttpExtension httpExtension, Map<String, String> metadata, Class<T> clazz) {
    return this.invokeMethod(appId, methodName, null, httpExtension, metadata, TypeRef.get(clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeMethod(String appId, String methodName, Object request, HttpExtension httpExtension,
                                  TypeRef<T> type) {
    return this.invokeMethod(appId, methodName, request, httpExtension, null, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeMethod(String appId, String methodName, Object request, HttpExtension httpExtension,
                                  Class<T> clazz) {
    return this.invokeMethod(appId, methodName, request, httpExtension, null, TypeRef.get(clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeMethod(String appId, String methodName, Object request, HttpExtension httpExtension) {
    return this.invokeMethod(appId, methodName, request, httpExtension, null, TypeRef.BYTE_ARRAY).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeMethod(
          String appId, String methodName, Object request, HttpExtension httpExtension, Map<String, String> metadata) {
    return this.invokeMethod(appId, methodName, request, httpExtension, metadata, TypeRef.BYTE_ARRAY).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeMethod(
          String appId, String methodName, HttpExtension httpExtension, Map<String, String> metadata) {
    return this.invokeMethod(appId, methodName, null, httpExtension, metadata, TypeRef.BYTE_ARRAY).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<byte[]> invokeMethod(
          String appId, String methodName, byte[] request, HttpExtension httpExtension, Map<String, String> metadata) {
    return this.invokeMethod(appId, methodName, request, httpExtension, metadata, TypeRef.BYTE_ARRAY);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeBinding(String bindingName, String operation, Object data) {
    return this.invokeBinding(bindingName, operation, data, null, TypeRef.BYTE_ARRAY).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<byte[]> invokeBinding(String bindingName, String operation, byte[] data, Map<String, String> metadata) {
    return this.invokeBinding(bindingName, operation, data, metadata, TypeRef.BYTE_ARRAY);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeBinding(String bindingName, String operation, Object data, TypeRef<T> type) {
    return this.invokeBinding(bindingName, operation, data, null, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeBinding(String bindingName, String operation, Object data, Class<T> clazz) {
    return this.invokeBinding(bindingName, operation, data, null, TypeRef.get(clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeBinding(
          String bindingName, String operation, Object data, Map<String, String> metadata, TypeRef<T> type) {
    InvokeBindingRequest request = new InvokeBindingRequestBuilder(bindingName, operation)
        .withData(data)
        .withMetadata(metadata)
        .build();
    return this.invokeBinding(request, type);
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeBinding(
          String bindingName, String operation, Object data, Map<String, String> metadata, Class<T> clazz) {
    return this.invokeBinding(bindingName, operation, data, metadata, TypeRef.get(clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(String storeName, State<T> state, TypeRef<T> type) {
    return this.getState(storeName, state.getKey(), state.getOptions(), type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(String storeName, State<T> state, Class<T> clazz) {
    return this.getState(storeName, state.getKey(), state.getOptions(), TypeRef.get(clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(String storeName, String key, TypeRef<T> type) {
    return this.getState(storeName, key, null, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(String storeName, String key, Class<T> clazz) {
    return this.getState(storeName, key, null, TypeRef.get(clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(
          String storeName, String key, StateOptions options, TypeRef<T> type) {
    GetStateRequest request = new GetStateRequestBuilder(storeName, key)
        .withStateOptions(options)
        .build();
    return this.getState(request, type);
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(
          String storeName, String key, StateOptions options, Class<T> clazz) {
    return this.getState(storeName, key, options, TypeRef.get(clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<List<State<T>>> getBulkState(String storeName, List<String> keys, TypeRef<T> type) {
    return this.getBulkState(new GetBulkStateRequestBuilder(storeName, keys).build(), type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<List<State<T>>> getBulkState(String storeName, List<String> keys, Class<T> clazz) {
    return this.getBulkState(storeName, keys, TypeRef.get(clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> executeStateTransaction(String storeName,
                                            List<TransactionalStateOperation<?>> operations) {
    ExecuteStateTransactionRequest request = new ExecuteStateTransactionRequestBuilder(storeName)
        .withTransactionalStates(operations)
        .build();
    return executeStateTransaction(request).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> saveBulkState(String storeName, List<State<?>> states) {
    SaveStateRequest request = new SaveStateRequestBuilder(storeName)
        .withStates(states)
        .build();
    return this.saveBulkState(request).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> saveState(String storeName, String key, Object value) {
    return this.saveState(storeName, key, null, value, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> saveState(String storeName, String key, String etag, Object value, StateOptions options) {
    State<?> state = new State<>(key, value, etag, options);
    return this.saveBulkState(storeName, Collections.singletonList(state));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> deleteState(String storeName, String key) {
    return this.deleteState(storeName, key, null, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> deleteState(String storeName, String key, String etag, StateOptions options) {
    DeleteStateRequest request = new DeleteStateRequestBuilder(storeName, key)
        .withEtag(etag)
        .withStateOptions(options)
        .build();
    return deleteState(request).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Map<String, String>> getSecret(String storeName, String key, Map<String, String> metadata) {
    GetSecretRequest request = new GetSecretRequestBuilder(storeName, key)
        .withMetadata(metadata)
        .build();
    return getSecret(request).defaultIfEmpty(Collections.emptyMap());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Map<String, String>> getSecret(String storeName, String secretName) {
    return this.getSecret(storeName, secretName, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Map<String, Map<String, String>>> getBulkSecret(String storeName) {
    return this.getBulkSecret(storeName, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Map<String, Map<String, String>>> getBulkSecret(String storeName, Map<String, String> metadata) {
    GetBulkSecretRequest request = new GetBulkSecretRequestBuilder(storeName)
        .withMetadata(metadata)
        .build();
    return this.getBulkSecret(request).defaultIfEmpty(Collections.emptyMap());
  }

}
