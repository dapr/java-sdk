/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.spring.boot.autoconfigure.client;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.BulkPublishRequest;
import io.dapr.client.domain.BulkPublishResponse;
import io.dapr.client.domain.ConfigurationItem;
import io.dapr.client.domain.DaprMetadata;
import io.dapr.client.domain.DeleteJobRequest;
import io.dapr.client.domain.DeleteStateRequest;
import io.dapr.client.domain.ExecuteStateTransactionRequest;
import io.dapr.client.domain.GetBulkSecretRequest;
import io.dapr.client.domain.GetBulkStateRequest;
import io.dapr.client.domain.GetConfigurationRequest;
import io.dapr.client.domain.GetJobRequest;
import io.dapr.client.domain.GetJobResponse;
import io.dapr.client.domain.GetSecretRequest;
import io.dapr.client.domain.GetStateRequest;
import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.InvokeBindingRequest;
import io.dapr.client.domain.InvokeMethodRequest;
import io.dapr.client.domain.PublishEventRequest;
import io.dapr.client.domain.SaveStateRequest;
import io.dapr.client.domain.ScheduleJobRequest;
import io.dapr.client.domain.State;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.SubscribeConfigurationRequest;
import io.dapr.client.domain.SubscribeConfigurationResponse;
import io.dapr.client.domain.TransactionalStateOperation;
import io.dapr.client.domain.UnsubscribeConfigurationRequest;
import io.dapr.client.domain.UnsubscribeConfigurationResponse;
import io.dapr.utils.TypeRef;
import io.grpc.Channel;
import io.grpc.stub.AbstractStub;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A {@link DaprClient} decorator that creates Micrometer Observation spans (bridged to OpenTelemetry)
 * for each non-deprecated method call. Consumers continue to use {@link DaprClient} as-is; no code
 * changes are required on their side.
 *
 * <p>Deprecated methods are delegated directly without any observation.
 */
public class ObservationDaprClient implements DaprClient {

  private final DaprClient delegate;
  private final ObservationRegistry observationRegistry;

  /**
   * Creates a new {@code ObservationDaprClient}.
   *
   * @param delegate            the underlying {@link DaprClient} to delegate calls to
   * @param observationRegistry the Micrometer {@link ObservationRegistry} used to create spans
   */
  public ObservationDaprClient(DaprClient delegate, ObservationRegistry observationRegistry) {
    this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    this.observationRegistry = Objects.requireNonNull(observationRegistry,
        "observationRegistry must not be null");
  }

  // -------------------------------------------------------------------------
  // Internal helpers
  // -------------------------------------------------------------------------

  private <T> Mono<T> observe(Observation obs, Supplier<Mono<T>> monoSupplier) {
    return Mono.defer(() -> {
      obs.start();
      return monoSupplier.get()
          .doOnError(obs::error)
          .doFinally(signal -> obs.stop());
    });
  }

  private <T> Flux<T> observeFlux(Observation obs, Supplier<Flux<T>> fluxSupplier) {
    return Flux.defer(() -> {
      obs.start();
      return fluxSupplier.get()
          .doOnError(obs::error)
          .doFinally(signal -> obs.stop());
    });
  }

  private static String safe(String value) {
    return value != null ? value : "";
  }

  private Observation observation(String name) {
    return Observation.createNotStarted(name, observationRegistry);
  }

  // -------------------------------------------------------------------------
  // Lifecycle
  // -------------------------------------------------------------------------

  @Override
  public Mono<Void> waitForSidecar(int timeoutInMilliseconds) {
    return observe(observation("dapr.client.wait_for_sidecar"),
        () -> delegate.waitForSidecar(timeoutInMilliseconds));
  }

  @Override
  public Mono<Void> shutdown() {
    return observe(observation("dapr.client.shutdown"),
        () -> delegate.shutdown());
  }

  @Override
  public void close() throws Exception {
    delegate.close();
  }

  // -------------------------------------------------------------------------
  // Pub/Sub
  // -------------------------------------------------------------------------

  @Override
  public Mono<Void> publishEvent(String pubsubName, String topicName, Object data) {
    return observe(
        observation("dapr.client.publish_event")
            .highCardinalityKeyValue("dapr.pubsub.name", safe(pubsubName))
            .highCardinalityKeyValue("dapr.topic.name", safe(topicName)),
        () -> delegate.publishEvent(pubsubName, topicName, data));
  }

  @Override
  public Mono<Void> publishEvent(String pubsubName, String topicName, Object data,
                                 Map<String, String> metadata) {
    return observe(
        observation("dapr.client.publish_event")
            .highCardinalityKeyValue("dapr.pubsub.name", safe(pubsubName))
            .highCardinalityKeyValue("dapr.topic.name", safe(topicName)),
        () -> delegate.publishEvent(pubsubName, topicName, data, metadata));
  }

  @Override
  public Mono<Void> publishEvent(PublishEventRequest request) {
    return observe(
        observation("dapr.client.publish_event")
            .highCardinalityKeyValue("dapr.pubsub.name", safe(request.getPubsubName()))
            .highCardinalityKeyValue("dapr.topic.name", safe(request.getTopic())),
        () -> delegate.publishEvent(request));
  }

  @Override
  public <T> Mono<BulkPublishResponse<T>> publishEvents(BulkPublishRequest<T> request) {
    return observe(
        observation("dapr.client.publish_events")
            .highCardinalityKeyValue("dapr.pubsub.name", safe(request.getPubsubName()))
            .highCardinalityKeyValue("dapr.topic.name", safe(request.getTopic())),
        () -> delegate.publishEvents(request));
  }

  @Override
  public <T> Mono<BulkPublishResponse<T>> publishEvents(String pubsubName, String topicName,
                                                         String contentType, List<T> events) {
    return observe(
        observation("dapr.client.publish_events")
            .highCardinalityKeyValue("dapr.pubsub.name", safe(pubsubName))
            .highCardinalityKeyValue("dapr.topic.name", safe(topicName)),
        () -> delegate.publishEvents(pubsubName, topicName, contentType, events));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Mono<BulkPublishResponse<T>> publishEvents(String pubsubName, String topicName,
                                                         String contentType, T... events) {
    return observe(
        observation("dapr.client.publish_events")
            .highCardinalityKeyValue("dapr.pubsub.name", safe(pubsubName))
            .highCardinalityKeyValue("dapr.topic.name", safe(topicName)),
        () -> delegate.publishEvents(pubsubName, topicName, contentType, events));
  }

  @Override
  public <T> Mono<BulkPublishResponse<T>> publishEvents(String pubsubName, String topicName,
                                                         String contentType,
                                                         Map<String, String> requestMetadata,
                                                         List<T> events) {
    return observe(
        observation("dapr.client.publish_events")
            .highCardinalityKeyValue("dapr.pubsub.name", safe(pubsubName))
            .highCardinalityKeyValue("dapr.topic.name", safe(topicName)),
        () -> delegate.publishEvents(pubsubName, topicName, contentType, requestMetadata, events));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Mono<BulkPublishResponse<T>> publishEvents(String pubsubName, String topicName,
                                                         String contentType,
                                                         Map<String, String> requestMetadata,
                                                         T... events) {
    return observe(
        observation("dapr.client.publish_events")
            .highCardinalityKeyValue("dapr.pubsub.name", safe(pubsubName))
            .highCardinalityKeyValue("dapr.topic.name", safe(topicName)),
        () -> delegate.publishEvents(pubsubName, topicName, contentType, requestMetadata, events));
  }

  // -------------------------------------------------------------------------
  // Service Invocation — all deprecated, delegate directly
  // -------------------------------------------------------------------------

  @Override
  @Deprecated
  public <T> Mono<T> invokeMethod(String appId, String methodName, Object data,
                                   HttpExtension httpExtension, Map<String, String> metadata,
                                   TypeRef<T> type) {
    return delegate.invokeMethod(appId, methodName, data, httpExtension, metadata, type);
  }

  @Override
  @Deprecated
  public <T> Mono<T> invokeMethod(String appId, String methodName, Object request,
                                   HttpExtension httpExtension, Map<String, String> metadata,
                                   Class<T> clazz) {
    return delegate.invokeMethod(appId, methodName, request, httpExtension, metadata, clazz);
  }

  @Override
  @Deprecated
  public <T> Mono<T> invokeMethod(String appId, String methodName, Object request,
                                   HttpExtension httpExtension, TypeRef<T> type) {
    return delegate.invokeMethod(appId, methodName, request, httpExtension, type);
  }

  @Override
  @Deprecated
  public <T> Mono<T> invokeMethod(String appId, String methodName, Object request,
                                   HttpExtension httpExtension, Class<T> clazz) {
    return delegate.invokeMethod(appId, methodName, request, httpExtension, clazz);
  }

  @Override
  @Deprecated
  public <T> Mono<T> invokeMethod(String appId, String methodName, HttpExtension httpExtension,
                                   Map<String, String> metadata, TypeRef<T> type) {
    return delegate.invokeMethod(appId, methodName, httpExtension, metadata, type);
  }

  @Override
  @Deprecated
  public <T> Mono<T> invokeMethod(String appId, String methodName, HttpExtension httpExtension,
                                   Map<String, String> metadata, Class<T> clazz) {
    return delegate.invokeMethod(appId, methodName, httpExtension, metadata, clazz);
  }

  @Override
  @Deprecated
  public Mono<Void> invokeMethod(String appId, String methodName, Object request,
                                  HttpExtension httpExtension, Map<String, String> metadata) {
    return delegate.invokeMethod(appId, methodName, request, httpExtension, metadata);
  }

  @Override
  @Deprecated
  public Mono<Void> invokeMethod(String appId, String methodName, Object request,
                                  HttpExtension httpExtension) {
    return delegate.invokeMethod(appId, methodName, request, httpExtension);
  }

  @Override
  @Deprecated
  public Mono<Void> invokeMethod(String appId, String methodName, HttpExtension httpExtension,
                                  Map<String, String> metadata) {
    return delegate.invokeMethod(appId, methodName, httpExtension, metadata);
  }

  @Override
  @Deprecated
  public Mono<byte[]> invokeMethod(String appId, String methodName, byte[] request,
                                    HttpExtension httpExtension, Map<String, String> metadata) {
    return delegate.invokeMethod(appId, methodName, request, httpExtension, metadata);
  }

  @Override
  @Deprecated
  public <T> Mono<T> invokeMethod(InvokeMethodRequest invokeMethodRequest, TypeRef<T> type) {
    return delegate.invokeMethod(invokeMethodRequest, type);
  }

  // -------------------------------------------------------------------------
  // Bindings
  // -------------------------------------------------------------------------

  @Override
  public Mono<Void> invokeBinding(String bindingName, String operation, Object data) {
    return observe(
        observation("dapr.client.invoke_binding")
            .highCardinalityKeyValue("dapr.binding.name", safe(bindingName))
            .highCardinalityKeyValue("dapr.binding.operation", safe(operation)),
        () -> delegate.invokeBinding(bindingName, operation, data));
  }

  @Override
  public Mono<byte[]> invokeBinding(String bindingName, String operation, byte[] data,
                                     Map<String, String> metadata) {
    return observe(
        observation("dapr.client.invoke_binding")
            .highCardinalityKeyValue("dapr.binding.name", safe(bindingName))
            .highCardinalityKeyValue("dapr.binding.operation", safe(operation)),
        () -> delegate.invokeBinding(bindingName, operation, data, metadata));
  }

  @Override
  public <T> Mono<T> invokeBinding(String bindingName, String operation, Object data,
                                    TypeRef<T> type) {
    return observe(
        observation("dapr.client.invoke_binding")
            .highCardinalityKeyValue("dapr.binding.name", safe(bindingName))
            .highCardinalityKeyValue("dapr.binding.operation", safe(operation)),
        () -> delegate.invokeBinding(bindingName, operation, data, type));
  }

  @Override
  public <T> Mono<T> invokeBinding(String bindingName, String operation, Object data,
                                    Class<T> clazz) {
    return observe(
        observation("dapr.client.invoke_binding")
            .highCardinalityKeyValue("dapr.binding.name", safe(bindingName))
            .highCardinalityKeyValue("dapr.binding.operation", safe(operation)),
        () -> delegate.invokeBinding(bindingName, operation, data, clazz));
  }

  @Override
  public <T> Mono<T> invokeBinding(String bindingName, String operation, Object data,
                                    Map<String, String> metadata, TypeRef<T> type) {
    return observe(
        observation("dapr.client.invoke_binding")
            .highCardinalityKeyValue("dapr.binding.name", safe(bindingName))
            .highCardinalityKeyValue("dapr.binding.operation", safe(operation)),
        () -> delegate.invokeBinding(bindingName, operation, data, metadata, type));
  }

  @Override
  public <T> Mono<T> invokeBinding(String bindingName, String operation, Object data,
                                    Map<String, String> metadata, Class<T> clazz) {
    return observe(
        observation("dapr.client.invoke_binding")
            .highCardinalityKeyValue("dapr.binding.name", safe(bindingName))
            .highCardinalityKeyValue("dapr.binding.operation", safe(operation)),
        () -> delegate.invokeBinding(bindingName, operation, data, metadata, clazz));
  }

  @Override
  public Mono<Void> invokeBinding(InvokeBindingRequest request) {
    return observe(
        observation("dapr.client.invoke_binding")
            .highCardinalityKeyValue("dapr.binding.name", safe(request.getName()))
            .highCardinalityKeyValue("dapr.binding.operation", safe(request.getOperation())),
        () -> delegate.invokeBinding(request));
  }

  @Override
  public <T> Mono<T> invokeBinding(InvokeBindingRequest request, TypeRef<T> type) {
    return observe(
        observation("dapr.client.invoke_binding")
            .highCardinalityKeyValue("dapr.binding.name", safe(request.getName()))
            .highCardinalityKeyValue("dapr.binding.operation", safe(request.getOperation())),
        () -> delegate.invokeBinding(request, type));
  }

  // -------------------------------------------------------------------------
  // State Management
  // -------------------------------------------------------------------------

  @Override
  public <T> Mono<State<T>> getState(String storeName, State<T> state, TypeRef<T> type) {
    return observe(
        observation("dapr.client.get_state")
            .highCardinalityKeyValue("dapr.store.name", safe(storeName))
            .highCardinalityKeyValue("dapr.state.key", safe(state.getKey())),
        () -> delegate.getState(storeName, state, type));
  }

  @Override
  public <T> Mono<State<T>> getState(String storeName, State<T> state, Class<T> clazz) {
    return observe(
        observation("dapr.client.get_state")
            .highCardinalityKeyValue("dapr.store.name", safe(storeName))
            .highCardinalityKeyValue("dapr.state.key", safe(state.getKey())),
        () -> delegate.getState(storeName, state, clazz));
  }

  @Override
  public <T> Mono<State<T>> getState(String storeName, String key, TypeRef<T> type) {
    return observe(
        observation("dapr.client.get_state")
            .highCardinalityKeyValue("dapr.store.name", safe(storeName))
            .highCardinalityKeyValue("dapr.state.key", safe(key)),
        () -> delegate.getState(storeName, key, type));
  }

  @Override
  public <T> Mono<State<T>> getState(String storeName, String key, Class<T> clazz) {
    return observe(
        observation("dapr.client.get_state")
            .highCardinalityKeyValue("dapr.store.name", safe(storeName))
            .highCardinalityKeyValue("dapr.state.key", safe(key)),
        () -> delegate.getState(storeName, key, clazz));
  }

  @Override
  public <T> Mono<State<T>> getState(String storeName, String key, StateOptions options,
                                      TypeRef<T> type) {
    return observe(
        observation("dapr.client.get_state")
            .highCardinalityKeyValue("dapr.store.name", safe(storeName))
            .highCardinalityKeyValue("dapr.state.key", safe(key)),
        () -> delegate.getState(storeName, key, options, type));
  }

  @Override
  public <T> Mono<State<T>> getState(String storeName, String key, StateOptions options,
                                      Class<T> clazz) {
    return observe(
        observation("dapr.client.get_state")
            .highCardinalityKeyValue("dapr.store.name", safe(storeName))
            .highCardinalityKeyValue("dapr.state.key", safe(key)),
        () -> delegate.getState(storeName, key, options, clazz));
  }

  @Override
  public <T> Mono<State<T>> getState(GetStateRequest request, TypeRef<T> type) {
    return observe(
        observation("dapr.client.get_state")
            .highCardinalityKeyValue("dapr.store.name", safe(request.getStoreName()))
            .highCardinalityKeyValue("dapr.state.key", safe(request.getKey())),
        () -> delegate.getState(request, type));
  }

  @Override
  public <T> Mono<List<State<T>>> getBulkState(String storeName, List<String> keys,
                                                TypeRef<T> type) {
    return observe(
        observation("dapr.client.get_bulk_state")
            .highCardinalityKeyValue("dapr.store.name", safe(storeName)),
        () -> delegate.getBulkState(storeName, keys, type));
  }

  @Override
  public <T> Mono<List<State<T>>> getBulkState(String storeName, List<String> keys,
                                                Class<T> clazz) {
    return observe(
        observation("dapr.client.get_bulk_state")
            .highCardinalityKeyValue("dapr.store.name", safe(storeName)),
        () -> delegate.getBulkState(storeName, keys, clazz));
  }

  @Override
  public <T> Mono<List<State<T>>> getBulkState(GetBulkStateRequest request, TypeRef<T> type) {
    return observe(
        observation("dapr.client.get_bulk_state")
            .highCardinalityKeyValue("dapr.store.name", safe(request.getStoreName())),
        () -> delegate.getBulkState(request, type));
  }

  @Override
  public Mono<Void> executeStateTransaction(String storeName,
                                             List<TransactionalStateOperation<?>> operations) {
    return observe(
        observation("dapr.client.execute_state_transaction")
            .highCardinalityKeyValue("dapr.store.name", safe(storeName)),
        () -> delegate.executeStateTransaction(storeName, operations));
  }

  @Override
  public Mono<Void> executeStateTransaction(ExecuteStateTransactionRequest request) {
    return observe(
        observation("dapr.client.execute_state_transaction")
            .highCardinalityKeyValue("dapr.store.name", safe(request.getStateStoreName())),
        () -> delegate.executeStateTransaction(request));
  }

  @Override
  public Mono<Void> saveBulkState(String storeName, List<State<?>> states) {
    return observe(
        observation("dapr.client.save_bulk_state")
            .highCardinalityKeyValue("dapr.store.name", safe(storeName)),
        () -> delegate.saveBulkState(storeName, states));
  }

  @Override
  public Mono<Void> saveBulkState(SaveStateRequest request) {
    return observe(
        observation("dapr.client.save_bulk_state")
            .highCardinalityKeyValue("dapr.store.name", safe(request.getStoreName())),
        () -> delegate.saveBulkState(request));
  }

  @Override
  public Mono<Void> saveState(String storeName, String key, Object value) {
    return observe(
        observation("dapr.client.save_state")
            .highCardinalityKeyValue("dapr.store.name", safe(storeName))
            .highCardinalityKeyValue("dapr.state.key", safe(key)),
        () -> delegate.saveState(storeName, key, value));
  }

  @Override
  public Mono<Void> saveState(String storeName, String key, String etag, Object value,
                               StateOptions options) {
    return observe(
        observation("dapr.client.save_state")
            .highCardinalityKeyValue("dapr.store.name", safe(storeName))
            .highCardinalityKeyValue("dapr.state.key", safe(key)),
        () -> delegate.saveState(storeName, key, etag, value, options));
  }

  @Override
  public Mono<Void> saveState(String storeName, String key, String etag, Object value,
                               Map<String, String> meta, StateOptions options) {
    return observe(
        observation("dapr.client.save_state")
            .highCardinalityKeyValue("dapr.store.name", safe(storeName))
            .highCardinalityKeyValue("dapr.state.key", safe(key)),
        () -> delegate.saveState(storeName, key, etag, value, meta, options));
  }

  @Override
  public Mono<Void> deleteState(String storeName, String key) {
    return observe(
        observation("dapr.client.delete_state")
            .highCardinalityKeyValue("dapr.store.name", safe(storeName))
            .highCardinalityKeyValue("dapr.state.key", safe(key)),
        () -> delegate.deleteState(storeName, key));
  }

  @Override
  public Mono<Void> deleteState(String storeName, String key, String etag,
                                 StateOptions options) {
    return observe(
        observation("dapr.client.delete_state")
            .highCardinalityKeyValue("dapr.store.name", safe(storeName))
            .highCardinalityKeyValue("dapr.state.key", safe(key)),
        () -> delegate.deleteState(storeName, key, etag, options));
  }

  @Override
  public Mono<Void> deleteState(DeleteStateRequest request) {
    return observe(
        observation("dapr.client.delete_state")
            .highCardinalityKeyValue("dapr.store.name", safe(request.getStateStoreName()))
            .highCardinalityKeyValue("dapr.state.key", safe(request.getKey())),
        () -> delegate.deleteState(request));
  }

  // -------------------------------------------------------------------------
  // Secrets
  // -------------------------------------------------------------------------

  @Override
  public Mono<Map<String, String>> getSecret(String storeName, String secretName,
                                              Map<String, String> metadata) {
    return observe(
        observation("dapr.client.get_secret")
            .highCardinalityKeyValue("dapr.secret.store", safe(storeName))
            .highCardinalityKeyValue("dapr.secret.name", safe(secretName)),
        () -> delegate.getSecret(storeName, secretName, metadata));
  }

  @Override
  public Mono<Map<String, String>> getSecret(String storeName, String secretName) {
    return observe(
        observation("dapr.client.get_secret")
            .highCardinalityKeyValue("dapr.secret.store", safe(storeName))
            .highCardinalityKeyValue("dapr.secret.name", safe(secretName)),
        () -> delegate.getSecret(storeName, secretName));
  }

  @Override
  public Mono<Map<String, String>> getSecret(GetSecretRequest request) {
    return observe(
        observation("dapr.client.get_secret")
            .highCardinalityKeyValue("dapr.secret.store", safe(request.getStoreName()))
            .highCardinalityKeyValue("dapr.secret.name", safe(request.getKey())),
        () -> delegate.getSecret(request));
  }

  @Override
  public Mono<Map<String, Map<String, String>>> getBulkSecret(String storeName) {
    return observe(
        observation("dapr.client.get_bulk_secret")
            .highCardinalityKeyValue("dapr.secret.store", safe(storeName)),
        () -> delegate.getBulkSecret(storeName));
  }

  @Override
  public Mono<Map<String, Map<String, String>>> getBulkSecret(String storeName,
                                                               Map<String, String> metadata) {
    return observe(
        observation("dapr.client.get_bulk_secret")
            .highCardinalityKeyValue("dapr.secret.store", safe(storeName)),
        () -> delegate.getBulkSecret(storeName, metadata));
  }

  @Override
  public Mono<Map<String, Map<String, String>>> getBulkSecret(GetBulkSecretRequest request) {
    return observe(
        observation("dapr.client.get_bulk_secret")
            .highCardinalityKeyValue("dapr.secret.store", safe(request.getStoreName())),
        () -> delegate.getBulkSecret(request));
  }

  // -------------------------------------------------------------------------
  // Configuration
  // -------------------------------------------------------------------------

  @Override
  public Mono<ConfigurationItem> getConfiguration(String storeName, String key) {
    return observe(
        observation("dapr.client.get_configuration")
            .highCardinalityKeyValue("dapr.configuration.store", safe(storeName))
            .highCardinalityKeyValue("dapr.configuration.key", safe(key)),
        () -> delegate.getConfiguration(storeName, key));
  }

  @Override
  public Mono<ConfigurationItem> getConfiguration(String storeName, String key,
                                                   Map<String, String> metadata) {
    return observe(
        observation("dapr.client.get_configuration")
            .highCardinalityKeyValue("dapr.configuration.store", safe(storeName))
            .highCardinalityKeyValue("dapr.configuration.key", safe(key)),
        () -> delegate.getConfiguration(storeName, key, metadata));
  }

  @Override
  public Mono<Map<String, ConfigurationItem>> getConfiguration(String storeName, String... keys) {
    return observe(
        observation("dapr.client.get_configuration")
            .highCardinalityKeyValue("dapr.configuration.store", safe(storeName)),
        () -> delegate.getConfiguration(storeName, keys));
  }

  @Override
  public Mono<Map<String, ConfigurationItem>> getConfiguration(String storeName, List<String> keys,
                                                                Map<String, String> metadata) {
    return observe(
        observation("dapr.client.get_configuration")
            .highCardinalityKeyValue("dapr.configuration.store", safe(storeName)),
        () -> delegate.getConfiguration(storeName, keys, metadata));
  }

  @Override
  public Mono<Map<String, ConfigurationItem>> getConfiguration(GetConfigurationRequest request) {
    return observe(
        observation("dapr.client.get_configuration")
            .highCardinalityKeyValue("dapr.configuration.store", safe(request.getStoreName())),
        () -> delegate.getConfiguration(request));
  }

  @Override
  public Flux<SubscribeConfigurationResponse> subscribeConfiguration(String storeName,
                                                                      String... keys) {
    return observeFlux(
        observation("dapr.client.subscribe_configuration")
            .highCardinalityKeyValue("dapr.configuration.store", safe(storeName)),
        () -> delegate.subscribeConfiguration(storeName, keys));
  }

  @Override
  public Flux<SubscribeConfigurationResponse> subscribeConfiguration(String storeName,
                                                                      List<String> keys,
                                                                      Map<String, String> metadata) {
    return observeFlux(
        observation("dapr.client.subscribe_configuration")
            .highCardinalityKeyValue("dapr.configuration.store", safe(storeName)),
        () -> delegate.subscribeConfiguration(storeName, keys, metadata));
  }

  @Override
  public Flux<SubscribeConfigurationResponse> subscribeConfiguration(
      SubscribeConfigurationRequest request) {
    return observeFlux(
        observation("dapr.client.subscribe_configuration")
            .highCardinalityKeyValue("dapr.configuration.store", safe(request.getStoreName())),
        () -> delegate.subscribeConfiguration(request));
  }

  @Override
  public Mono<UnsubscribeConfigurationResponse> unsubscribeConfiguration(String id,
                                                                          String storeName) {
    return observe(
        observation("dapr.client.unsubscribe_configuration")
            .highCardinalityKeyValue("dapr.configuration.store", safe(storeName)),
        () -> delegate.unsubscribeConfiguration(id, storeName));
  }

  @Override
  public Mono<UnsubscribeConfigurationResponse> unsubscribeConfiguration(
      UnsubscribeConfigurationRequest request) {
    return observe(
        observation("dapr.client.unsubscribe_configuration")
            .highCardinalityKeyValue("dapr.configuration.store", safe(request.getStoreName())),
        () -> delegate.unsubscribeConfiguration(request));
  }

  // -------------------------------------------------------------------------
  // gRPC Stub — no remote call at creation time, no observation needed
  // -------------------------------------------------------------------------

  @Override
  public <T extends AbstractStub<T>> T newGrpcStub(String appId, Function<Channel, T> stubBuilder) {
    return delegate.newGrpcStub(appId, stubBuilder);
  }

  // -------------------------------------------------------------------------
  // Metadata
  // -------------------------------------------------------------------------

  @Override
  public Mono<DaprMetadata> getMetadata() {
    return observe(observation("dapr.client.get_metadata"), () -> delegate.getMetadata());
  }

  // -------------------------------------------------------------------------
  // Jobs
  // -------------------------------------------------------------------------

  @Override
  public Mono<Void> scheduleJob(ScheduleJobRequest scheduleJobRequest) {
    return observe(
        observation("dapr.client.schedule_job")
            .highCardinalityKeyValue("dapr.job.name", safe(scheduleJobRequest.getName())),
        () -> delegate.scheduleJob(scheduleJobRequest));
  }

  @Override
  public Mono<GetJobResponse> getJob(GetJobRequest getJobRequest) {
    return observe(
        observation("dapr.client.get_job")
            .highCardinalityKeyValue("dapr.job.name", safe(getJobRequest.getName())),
        () -> delegate.getJob(getJobRequest));
  }

  @Override
  public Mono<Void> deleteJob(DeleteJobRequest deleteJobRequest) {
    return observe(
        observation("dapr.client.delete_job")
            .highCardinalityKeyValue("dapr.job.name", safe(deleteJobRequest.getName())),
        () -> delegate.deleteJob(deleteJobRequest));
  }
}
