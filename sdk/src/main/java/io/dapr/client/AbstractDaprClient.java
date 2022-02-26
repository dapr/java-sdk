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

package io.dapr.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.domain.ConfigurationItem;
import io.dapr.client.domain.DeleteStateRequest;
import io.dapr.client.domain.ExecuteStateTransactionRequest;
import io.dapr.client.domain.GetBulkSecretRequest;
import io.dapr.client.domain.GetBulkStateRequest;
import io.dapr.client.domain.GetConfigurationRequest;
import io.dapr.client.domain.GetSecretRequest;
import io.dapr.client.domain.GetStateRequest;
import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.InvokeBindingRequest;
import io.dapr.client.domain.InvokeMethodRequest;
import io.dapr.client.domain.PublishEventRequest;
import io.dapr.client.domain.QueryStateRequest;
import io.dapr.client.domain.QueryStateResponse;
import io.dapr.client.domain.SaveStateRequest;
import io.dapr.client.domain.State;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.SubscribeConfigurationRequest;
import io.dapr.client.domain.TransactionalStateOperation;
import io.dapr.client.domain.query.Query;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.utils.TypeRef;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
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
abstract class AbstractDaprClient implements DaprClient, DaprPreviewClient {

  /**
   * A mapper to serialize JSON request objects.
   */
  protected static final ObjectMapper JSON_REQUEST_MAPPER = new ObjectMapper();

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
    PublishEventRequest req = new PublishEventRequest(pubsubName, topicName, data)
        .setMetadata(metadata);
    return this.publishEvent(req).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeMethod(
      String appId,
      String methodName,
      Object data,
      HttpExtension httpExtension,
      Map<String, String> metadata,
      TypeRef<T> type) {
    InvokeMethodRequest req = new InvokeMethodRequest(appId, methodName)
        .setBody(data)
        .setHttpExtension(httpExtension)
        .setContentType(objectSerializer.getContentType());

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
    InvokeBindingRequest request = new InvokeBindingRequest(bindingName, operation)
        .setData(data)
        .setMetadata(metadata);
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
    GetStateRequest request = new GetStateRequest(storeName, key)
        .setStateOptions(options);
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
  public <T> Mono<QueryStateResponse<T>> queryState(String storeName, String query, Map<String, String> metadata,
                                                    Class<T> clazz) {
    return this.queryState(new QueryStateRequest(storeName).setQueryString(query).setMetadata(metadata), clazz);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<QueryStateResponse<T>> queryState(String storeName, String query, Map<String, String> metadata,
                                                    TypeRef<T> type) {
    return this.queryState(new QueryStateRequest(storeName).setQueryString(query).setMetadata(metadata), type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<QueryStateResponse<T>> queryState(String storeName, String query, Class<T> clazz) {
    return this.queryState(new QueryStateRequest(storeName).setQueryString(query), clazz);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<QueryStateResponse<T>> queryState(String storeName, String query, TypeRef<T> type) {
    return this.queryState(new QueryStateRequest(storeName).setQueryString(query), type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<QueryStateResponse<T>> queryState(String storeName, Query query, Map<String, String> metadata,
                                                    Class<T> clazz) {
    return this.queryState(new QueryStateRequest(storeName).setQuery(query).setMetadata(metadata), clazz);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<QueryStateResponse<T>> queryState(String storeName, Query query, Map<String, String> metadata,
                                                    TypeRef<T> type) {
    return this.queryState(new QueryStateRequest(storeName).setQuery(query).setMetadata(metadata), type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<QueryStateResponse<T>> queryState(String storeName, Query query, Class<T> clazz) {
    return this.queryState(new QueryStateRequest(storeName).setQuery(query), clazz);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<QueryStateResponse<T>> queryState(String storeName, Query query, TypeRef<T> type) {
    return this.queryState(new QueryStateRequest(storeName).setQuery(query), type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<QueryStateResponse<T>> queryState(QueryStateRequest request, Class<T> clazz) {
    return this.queryState(request, TypeRef.get(clazz));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<List<State<T>>> getBulkState(String storeName, List<String> keys, TypeRef<T> type) {
    return this.getBulkState(new GetBulkStateRequest(storeName, keys), type);
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
    ExecuteStateTransactionRequest request = new ExecuteStateTransactionRequest(storeName)
        .setOperations(operations);
    return executeStateTransaction(request).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> saveBulkState(String storeName, List<State<?>> states) {
    SaveStateRequest request = new SaveStateRequest(storeName)
        .setStates(states);
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
    DeleteStateRequest request = new DeleteStateRequest(storeName, key)
        .setEtag(etag)
        .setStateOptions(options);
    return deleteState(request).then();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Map<String, String>> getSecret(String storeName, String key, Map<String, String> metadata) {
    GetSecretRequest request = new GetSecretRequest(storeName, key)
        .setMetadata(metadata);
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
    GetBulkSecretRequest request = new GetBulkSecretRequest(storeName)
        .setMetadata(metadata);
    return this.getBulkSecret(request).defaultIfEmpty(Collections.emptyMap());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<ConfigurationItem> getConfiguration(String storeName, String key) {
    GetConfigurationRequest request = new GetConfigurationRequest(storeName, filterEmptyKeys(key));
    return this.getConfiguration(request).map(data -> data.get(0));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<ConfigurationItem> getConfiguration(String storeName, String key, Map<String, String> metadata) {
    GetConfigurationRequest request = new GetConfigurationRequest(storeName, filterEmptyKeys(key));
    request.setMetadata(metadata);
    return this.getConfiguration(request).map(data -> data.get(0));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<List<ConfigurationItem>> getConfiguration(String storeName, String... keys) {
    List<String> listOfKeys = filterEmptyKeys(keys);
    GetConfigurationRequest request = new GetConfigurationRequest(storeName, listOfKeys);
    return this.getConfiguration(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<List<ConfigurationItem>> getConfiguration(
      String storeName,
      List<String> keys,
      Map<String, String> metadata) {
    GetConfigurationRequest request = new GetConfigurationRequest(storeName, keys);
    request.setMetadata(metadata);
    return this.getConfiguration(request);
  }

  /**
   * {@inheritDoc}
   */
  public Flux<List<ConfigurationItem>> subscribeToConfiguration(String storeName, String... keys) {
    List<String> listOfKeys = filterEmptyKeys(keys);
    SubscribeConfigurationRequest request = new SubscribeConfigurationRequest(storeName, listOfKeys);
    return this.subscribeToConfiguration(request);
  }

  /**
   * {@inheritDoc}
   */
  public Flux<List<ConfigurationItem>> subscribeToConfiguration(
      String storeName,
      List<String> keys,
      Map<String, String> metadata) {
    SubscribeConfigurationRequest request = new SubscribeConfigurationRequest(storeName, keys);
    request.setMetadata(metadata);
    return this.subscribeToConfiguration(request);
  }

  private List<String> filterEmptyKeys(String... keys) {
    return Arrays.stream(keys)
        .filter(key -> !key.trim().isEmpty())
        .collect(Collectors.toList());
  }
}
