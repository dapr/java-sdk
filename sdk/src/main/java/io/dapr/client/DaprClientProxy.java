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

import io.dapr.client.domain.DeleteStateRequest;
import io.dapr.client.domain.ExecuteStateTransactionRequest;
import io.dapr.client.domain.GetBulkSecretRequest;
import io.dapr.client.domain.GetBulkStateRequest;
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
import io.dapr.client.domain.TransactionalStateOperation;
import io.dapr.utils.TypeRef;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Class that delegates to other implementations.
 *
 * @see DaprClient
 * @see DaprClientGrpc
 * @see DaprClientHttp
 */
class DaprClientProxy implements DaprClient {

  /**
   * Client for all API invocations.
   */
  private final DaprClient client;

  /**
   * Client to override Dapr's service invocation APIs.
   */
  private final DaprClient methodInvocationOverrideClient;

  /**
   * Constructor with delegate client.
   *
   * @param client                         Client for all API invocations.
   * @see DaprClientBuilder
   */
  DaprClientProxy(DaprClient client) {
    this(client, client);
  }

  /**
   * Constructor with delegate client and override client for Dapr's method invocation APIs.
   *
   * @param client                         Client for all API invocations, except override below.
   * @param methodInvocationOverrideClient Client to override Dapr's service invocation APIs.
   * @see DaprClientBuilder
   */
  DaprClientProxy(
      DaprClient client,
      DaprClient methodInvocationOverrideClient) {
    this.client = client;
    this.methodInvocationOverrideClient = methodInvocationOverrideClient;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> waitForSidecar(int timeoutInMilliseconds) {
    return client.waitForSidecar(timeoutInMilliseconds);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> publishEvent(String pubsubName, String topicName, Object data) {
    return client.publishEvent(pubsubName, topicName, data);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> publishEvent(String pubsubName, String topicName, Object data, Map<String, String> metadata) {
    return client.publishEvent(pubsubName, topicName, data, metadata);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> publishEvent(PublishEventRequest request) {
    return client.publishEvent(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeMethod(String appId,
      String methodName,
      Object data,
      HttpExtension httpExtension,
      Map<String, String> metadata,
      TypeRef<T> type) {
    return methodInvocationOverrideClient.invokeMethod(appId, methodName, data, httpExtension, metadata, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeMethod(String appId,
      String methodName,
      Object request,
      HttpExtension httpExtension,
      Map<String, String> metadata,
      Class<T> clazz) {
    return methodInvocationOverrideClient.invokeMethod(appId, methodName, request, httpExtension, metadata, clazz);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeMethod(String appId,
      String methodName,
      Object request,
      HttpExtension httpExtension,
      TypeRef<T> type) {
    return methodInvocationOverrideClient.invokeMethod(appId, methodName, request, httpExtension, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeMethod(String appId,
      String methodName,
      Object request,
      HttpExtension httpExtension,
      Class<T> clazz) {
    return methodInvocationOverrideClient.invokeMethod(appId, methodName, request, httpExtension, clazz);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeMethod(String appId,
      String methodName,
      HttpExtension httpExtension,
      Map<String, String> metadata,
      TypeRef<T> type) {
    return methodInvocationOverrideClient.invokeMethod(appId, methodName, httpExtension, metadata, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeMethod(String appId,
      String methodName,
      HttpExtension httpExtension,
      Map<String, String> metadata,
      Class<T> clazz) {
    return methodInvocationOverrideClient.invokeMethod(appId, methodName, httpExtension, metadata, clazz);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeMethod(String appId,
      String methodName,
      Object request,
      HttpExtension httpExtension,
      Map<String, String> metadata) {
    return methodInvocationOverrideClient.invokeMethod(appId, methodName, request, httpExtension, metadata);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeMethod(String appId, String methodName, Object request, HttpExtension httpExtension) {
    return methodInvocationOverrideClient.invokeMethod(appId, methodName, request, httpExtension);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeMethod(String appId,
      String methodName,
      HttpExtension httpExtension,
      Map<String, String> metadata) {
    return methodInvocationOverrideClient.invokeMethod(appId, methodName, httpExtension, metadata);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<byte[]> invokeMethod(String appId,
      String methodName,
      byte[] request,
      HttpExtension httpExtension,
      Map<String, String> metadata) {
    return methodInvocationOverrideClient.invokeMethod(appId, methodName, request, httpExtension, metadata);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeMethod(InvokeMethodRequest invokeMethodRequest, TypeRef<T> type) {
    return methodInvocationOverrideClient.invokeMethod(invokeMethodRequest, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> invokeBinding(String bindingName, String operation, Object data) {
    return client.invokeBinding(bindingName, operation, data);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<byte[]> invokeBinding(String bindingName, String operation, byte[] data, Map<String, String> metadata) {
    return client.invokeBinding(bindingName, operation, data, metadata);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeBinding(String bindingName, String operation, Object data, TypeRef<T> type) {
    return client.invokeBinding(bindingName, operation, data, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeBinding(String bindingName, String operation, Object data, Class<T> clazz) {
    return client.invokeBinding(bindingName, operation, data, clazz);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeBinding(String bindingName,
      String operation,
      Object data,
      Map<String, String> metadata,
      TypeRef<T> type) {
    return client.invokeBinding(bindingName, operation, data, metadata, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeBinding(String bindingName,
      String operation,
      Object data,
      Map<String, String> metadata,
      Class<T> clazz) {
    return client.invokeBinding(bindingName, operation, data, metadata, clazz);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> invokeBinding(InvokeBindingRequest request, TypeRef<T> type) {
    return client.invokeBinding(request, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(String storeName, State<T> state, TypeRef<T> type) {
    return client.getState(storeName, state, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(String storeName, State<T> state, Class<T> clazz) {
    return client.getState(storeName, state, clazz);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(String storeName, String key, TypeRef<T> type) {
    return client.getState(storeName, key, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(String storeName, String key, Class<T> clazz) {
    return client.getState(storeName, key, clazz);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(String storeName, String key, StateOptions options, TypeRef<T> type) {
    return client.getState(storeName, key, options, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(String storeName, String key, StateOptions options, Class<T> clazz) {
    return client.getState(storeName, key, options, clazz);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<State<T>> getState(GetStateRequest request, TypeRef<T> type) {
    return client.getState(request, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<List<State<T>>> getBulkState(String storeName, List<String> keys, TypeRef<T> type) {
    return client.getBulkState(storeName, keys, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<List<State<T>>> getBulkState(String storeName, List<String> keys, Class<T> clazz) {
    return client.getBulkState(storeName, keys, clazz);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<List<State<T>>> getBulkState(GetBulkStateRequest request, TypeRef<T> type) {
    return client.getBulkState(request, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> executeStateTransaction(String storeName, List<TransactionalStateOperation<?>> operations) {
    return client.executeStateTransaction(storeName, operations);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> executeStateTransaction(ExecuteStateTransactionRequest request) {
    return client.executeStateTransaction(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> saveBulkState(String storeName, List<State<?>> states) {
    return client.saveBulkState(storeName, states);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> saveBulkState(SaveStateRequest request) {
    return client.saveBulkState(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> saveState(String storeName, String key, Object value) {
    return client.saveState(storeName, key, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> saveState(String storeName, String key, String etag, Object value, StateOptions options) {
    return client.saveState(storeName, key, etag, value, options);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> deleteState(String storeName, String key) {
    return client.deleteState(storeName, key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> deleteState(String storeName, String key, String etag, StateOptions options) {
    return client.deleteState(storeName, key, etag, options);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> deleteState(DeleteStateRequest request) {
    return client.deleteState(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Map<String, String>> getSecret(String storeName, String secretName, Map<String, String> metadata) {
    return client.getSecret(storeName, secretName, metadata);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Map<String, String>> getSecret(String storeName, String secretName) {
    return client.getSecret(storeName, secretName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Map<String, String>> getSecret(GetSecretRequest request) {
    return client.getSecret(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Map<String, Map<String, String>>> getBulkSecret(String storeName) {
    return client.getBulkSecret(storeName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Map<String, Map<String, String>>> getBulkSecret(String storeName, Map<String, String> metadata) {
    return client.getBulkSecret(storeName, metadata);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Map<String, Map<String, String>>> getBulkSecret(GetBulkSecretRequest request) {
    return client.getBulkSecret(request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws Exception {
    client.close();
    if (client != methodInvocationOverrideClient) {
      methodInvocationOverrideClient.close();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> shutdown() {
    return client.shutdown();
  }
}
