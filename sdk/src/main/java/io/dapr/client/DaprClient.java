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

import io.dapr.client.domain.ConfigurationItem;
import io.dapr.client.domain.DaprMetadata;
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
import io.dapr.client.domain.SaveStateRequest;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Generic Client Adapter to be used regardless of the GRPC or the HTTP Client implementation required.
 *
 * @see io.dapr.client.DaprClientBuilder for information on how to make instance for this interface.
 */
public interface DaprClient extends AutoCloseable {

  /**
   * Waits for the sidecar, giving up after timeout.
   * @param timeoutInMilliseconds Timeout in milliseconds to wait for sidecar.
   * @return a Mono plan of type Void.
   */
  Mono<Void> waitForSidecar(int timeoutInMilliseconds);

  /**
   * Publish an event.
   *
   * @param pubsubName the pubsub name we will publish the event to
   * @param topicName the topicName where the event will be published.
   * @param data the event's data to be published, use byte[] for skipping serialization.
   * @return a Mono plan of type Void.
   */
  Mono<Void> publishEvent(String pubsubName, String topicName, Object data);

  /**
   * Publish an event.
   *
   * @param pubsubName the pubsub name we will publish the event to
   * @param topicName    the topicName where the event will be published.
   * @param data    the event's data to be published, use byte[] for skipping serialization.
   * @param metadata The metadata for the published event.
   * @return a Mono plan of type Void.
   */
  Mono<Void> publishEvent(String pubsubName, String topicName, Object data, Map<String, String> metadata);

  /**
   * Publish an event.
   *
   * @param request the request for the publish event.
   * @return a Mono plan of a Dapr's void response.
   */
  Mono<Void> publishEvent(PublishEventRequest request);

  /**
   * Invoke a service method, using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param methodName        The actual Method to be call in the application.
   * @param data       The data to be sent to invoke the service, use byte[] to skip serialization.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on
   *                      HTTP, {@link io.dapr.client.domain.HttpExtension#NONE} otherwise.
   * @param metadata      Metadata (in GRPC) or headers (in HTTP) to be sent in data.
   * @param type          The Type needed as return for the call.
   * @param <T>           The Type of the return, use byte[] to skip serialization.
   * @return A Mono Plan of type T.
   */
  <T> Mono<T> invokeMethod(String appId, String methodName, Object data, HttpExtension httpExtension,
                           Map<String, String> metadata, TypeRef<T> type);

  /**
   * Invoke a service method, using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param methodName        The actual Method to be call in the application.
   * @param request       The request to be sent to invoke the service, use byte[] to skip serialization.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on
   *                      HTTP, {@link HttpExtension#NONE} otherwise.
   * @param metadata      Metadata (in GRPC) or headers (in HTTP) to be sent in request.
   * @param clazz         The type needed as return for the call.
   * @param <T>           The Type of the return, use byte[] to skip serialization.
   * @return A Mono Plan of type T.
   */
  <T> Mono<T> invokeMethod(String appId, String methodName, Object request, HttpExtension httpExtension,
                           Map<String, String> metadata, Class<T> clazz);

  /**
   * Invoke a service method, using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param methodName        The actual Method to be call in the application.
   * @param request       The request to be sent to invoke the service, use byte[] to skip serialization.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on
   *                      HTTP, {@link HttpExtension#NONE} otherwise.
   * @param type          The Type needed as return for the call.
   * @param <T>           The Type of the return, use byte[] to skip serialization.
   * @return A Mono Plan of type T.
   */
  <T> Mono<T> invokeMethod(String appId, String methodName, Object request, HttpExtension httpExtension,
                           TypeRef<T> type);

  /**
   * Invoke a service method, using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param methodName        The actual Method to be call in the application.
   * @param request       The request to be sent to invoke the service, use byte[] to skip serialization.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on
   *                      HTTP, {@link HttpExtension#NONE} otherwise.
   * @param clazz         The type needed as return for the call.
   * @param <T>           The Type of the return, use byte[] to skip serialization.
   * @return A Mono Plan of type T.
   */
  <T> Mono<T> invokeMethod(String appId, String methodName, Object request, HttpExtension httpExtension,
                           Class<T> clazz);

  /**
   * Invoke a service method, using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param methodName        The actual Method to be call in the application.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on
   *                      HTTP, {@link HttpExtension#NONE} otherwise.
   * @param metadata      Metadata (in GRPC) or headers (in HTTP) to be sent in request.
   * @param type          The Type needed as return for the call.
   * @param <T>           The Type of the return, use byte[] to skip serialization.
   * @return A Mono Plan of type T.
   */
  <T> Mono<T> invokeMethod(String appId, String methodName, HttpExtension httpExtension, Map<String, String> metadata,
                           TypeRef<T> type);

  /**
   * Invoke a service method, using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param methodName        The actual Method to be call in the application.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on
   *                      HTTP, {@link HttpExtension#NONE} otherwise.
   * @param metadata      Metadata (in GRPC) or headers (in HTTP) to be sent in request.
   * @param clazz         The type needed as return for the call.
   * @param <T>           The Type of the return, use byte[] to skip serialization.
   * @return A Mono Plan of type T.
   */
  <T> Mono<T> invokeMethod(String appId, String methodName, HttpExtension httpExtension, Map<String, String> metadata,
                           Class<T> clazz);

  /**
   * Invoke a service method, using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param methodName        The actual Method to be call in the application.
   * @param request       The request to be sent to invoke the service, use byte[] to skip serialization.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on
   *                      HTTP, {@link HttpExtension#NONE} otherwise.
   * @param metadata      Metadata (in GRPC) or headers (in HTTP) to be sent in request.
   * @return A Mono Plan of type Void.
   */
  Mono<Void> invokeMethod(String appId, String methodName, Object request, HttpExtension httpExtension,
                          Map<String, String> metadata);

  /**
   * Invoke a service method, using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param methodName        The actual Method to be call in the application.
   * @param request       The request to be sent to invoke the service, use byte[] to skip serialization.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on
   *                      HTTP, {@link HttpExtension#NONE} otherwise.
   * @return A Mono Plan of type Void.
   */
  Mono<Void> invokeMethod(String appId, String methodName, Object request, HttpExtension httpExtension);

  /**
   * Invoke a service method, using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param methodName        The actual Method to be call in the application.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on
   *                      HTTP, {@link HttpExtension#NONE} otherwise.
   * @param metadata      Metadata (in GRPC) or headers (in HTTP) to be sent in request.
   * @return A Mono Plan of type Void.
   */
  Mono<Void> invokeMethod(String appId, String methodName, HttpExtension httpExtension, Map<String, String> metadata);

  /**
   * Invoke a service method, without using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param methodName        The actual Method to be call in the application.
   * @param request       The request to be sent to invoke the service, use byte[] to skip serialization.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on
   *                      HTTP, {@link HttpExtension#NONE} otherwise.
   * @param metadata      Metadata (in GRPC) or headers (in HTTP) to be sent in request.
   * @return A Mono Plan of type byte[].
   */
  Mono<byte[]> invokeMethod(String appId, String methodName, byte[] request, HttpExtension httpExtension,
                            Map<String, String> metadata);

  /**
   * Invoke a service method.
   *
   * @param invokeMethodRequest Request object.
   * @param type                 The Type needed as return for the call.
   * @param <T>                  The Type of the return, use byte[] to skip serialization.
   * @return A Mono Plan of type T.
   */
  <T> Mono<T> invokeMethod(InvokeMethodRequest invokeMethodRequest, TypeRef<T> type);

  /**
   * Invokes a Binding operation.
   *
   * @param bindingName      The bindingName of the biding to call.
   * @param operation The operation to be performed by the binding request processor.
   * @param data      The data to be processed, use byte[] to skip serialization.
   * @return an empty Mono.
   */
  Mono<Void> invokeBinding(String bindingName, String operation, Object data);

  /**
   * Invokes a Binding operation, skipping serialization.
   *
   * @param bindingName      The name of the biding to call.
   * @param operation The operation to be performed by the binding request processor.
   * @param data      The data to be processed, skipping serialization.
   * @param metadata  The metadata map.
   * @return a Mono plan of type byte[].
   */
  Mono<byte[]> invokeBinding(String bindingName, String operation, byte[] data, Map<String, String> metadata);

  /**
   * Invokes a Binding operation.
   *
   * @param bindingName      The name of the biding to call.
   * @param operation The operation to be performed by the binding request processor.
   * @param data      The data to be processed, use byte[] to skip serialization.
   * @param type      The type being returned.
   * @param <T>       The type of the return
   * @return a Mono plan of type T.
   */
  <T> Mono<T> invokeBinding(String bindingName, String operation, Object data, TypeRef<T> type);

  /**
   * Invokes a Binding operation.
   *
   * @param bindingName      The name of the biding to call.
   * @param operation The operation to be performed by the binding request processor.
   * @param data      The data to be processed, use byte[] to skip serialization.
   * @param clazz     The type being returned.
   * @param <T>       The type of the return
   * @return a Mono plan of type T.
   */
  <T> Mono<T> invokeBinding(String bindingName, String operation, Object data, Class<T> clazz);

  /**
   * Invokes a Binding operation.
   *
   * @param bindingName      The name of the biding to call.
   * @param operation The operation to be performed by the binding request processor.
   * @param data      The data to be processed, use byte[] to skip serialization.
   * @param metadata  The metadata map.
   * @param type      The type being returned.
   * @param <T>       The type of the return
   * @return a Mono plan of type T.
   */
  <T> Mono<T> invokeBinding(String bindingName, String operation, Object data, Map<String, String> metadata,
                            TypeRef<T> type);

  /**
   * Invokes a Binding operation.
   *
   * @param bindingName      The name of the biding to call.
   * @param operation The operation to be performed by the binding request processor.
   * @param data      The data to be processed, use byte[] to skip serialization.
   * @param metadata  The metadata map.
   * @param clazz     The type being returned.
   * @param <T>       The type of the return
   * @return a Mono plan of type T.
   */
  <T> Mono<T> invokeBinding(String bindingName, String operation, Object data, Map<String, String> metadata,
                            Class<T> clazz);

  /**
   * Invokes a Binding operation.
   *
   * @param request The binding invocation request.
   * @param type    The type being returned.
   * @param <T>     The type of the return
   * @return a Mono plan of type T.
   */
  <T> Mono<T> invokeBinding(InvokeBindingRequest request, TypeRef<T> type);

  /**
   * Retrieve a State based on their key.
   *
   * @param storeName The name of the state store.
   * @param state          State to be re-retrieved.
   * @param type           The type of State needed as return.
   * @param <T>            The type of the return.
   * @return A Mono Plan for the requested State.
   */
  <T> Mono<State<T>> getState(String storeName, State<T> state, TypeRef<T> type);

  /**
   * Retrieve a State based on their key.
   *
   * @param storeName The name of the state store.
   * @param state          State to be re-retrieved.
   * @param clazz          The type of State needed as return.
   * @param <T>            The type of the return.
   * @return A Mono Plan for the requested State.
   */
  <T> Mono<State<T>> getState(String storeName, State<T> state, Class<T> clazz);

  /**
   * Retrieve a State based on their key.
   *
   * @param storeName The name of the state store.
   * @param key            The key of the State to be retrieved.
   * @param type           The type of State needed as return.
   * @param <T>            The type of the return.
   * @return A Mono Plan for the requested State.
   */
  <T> Mono<State<T>> getState(String storeName, String key, TypeRef<T> type);

  /**
   * Retrieve a State based on their key.
   *
   * @param storeName The name of the state store.
   * @param key            The key of the State to be retrieved.
   * @param clazz          The type of State needed as return.
   * @param <T>            The type of the return.
   * @return A Mono Plan for the requested State.
   */
  <T> Mono<State<T>> getState(String storeName, String key, Class<T> clazz);

  /**
   * Retrieve a State based on their key.
   *
   * @param storeName The name of the state store.
   * @param key            The key of the State to be retrieved.
   * @param options        Optional settings for retrieve operation.
   * @param type           The Type of State needed as return.
   * @param <T>            The Type of the return.
   * @return A Mono Plan for the requested State.
   */
  <T> Mono<State<T>> getState(String storeName, String key, StateOptions options, TypeRef<T> type);

  /**
   * Retrieve a State based on their key.
   *
   * @param storeName The name of the state store.
   * @param key            The key of the State to be retrieved.
   * @param options        Optional settings for retrieve operation.
   * @param clazz          The Type of State needed as return.
   * @param <T>            The Type of the return.
   * @return A Mono Plan for the requested State.
   */
  <T> Mono<State<T>> getState(String storeName, String key, StateOptions options, Class<T> clazz);

  /**
   * Retrieve a State based on their key.
   *
   * @param request The request to get state.
   * @param type    The Type of State needed as return.
   * @param <T>     The Type of the return.
   * @return A Mono Plan for the requested State.
   */
  <T> Mono<State<T>> getState(GetStateRequest request, TypeRef<T> type);

  /**
   * Retrieve bulk States based on their keys.
   *
   * @param storeName The name of the state store.
   * @param keys           The keys of the State to be retrieved.
   * @param type           The type of State needed as return.
   * @param <T>            The type of the return.
   * @return A Mono Plan for the requested State.
   */
  <T> Mono<List<State<T>>> getBulkState(String storeName, List<String> keys, TypeRef<T> type);

  /**
   * Retrieve bulk States based on their keys.
   *
   * @param storeName The name of the state store.
   * @param keys           The keys of the State to be retrieved.
   * @param clazz          The type of State needed as return.
   * @param <T>            The type of the return.
   * @return A Mono Plan for the requested State.
   */
  <T> Mono<List<State<T>>> getBulkState(String storeName, List<String> keys, Class<T> clazz);

  /**
   * Retrieve bulk States based on their keys.
   *
   * @param request The request to get state.
   * @param type    The Type of State needed as return.
   * @param <T>     The Type of the return.
   * @return A Mono Plan for the requested State.
   */
  <T> Mono<List<State<T>>> getBulkState(GetBulkStateRequest request, TypeRef<T> type);

  /** Execute a transaction.
   *
   * @param storeName        The name of the state store.
   * @param operations            The operations to be performed.
   * @return  a Mono plan of type Void
   */
  Mono<Void> executeStateTransaction(String storeName,
                                     List<TransactionalStateOperation<?>> operations);


  /** Execute a transaction.
   *
   * @param request Request to execute transaction.
   * @return  a Mono plan of type Response Void
   */
  Mono<Void> executeStateTransaction(ExecuteStateTransactionRequest request);

  /**
   * Save/Update a list of states.
   *
   * @param storeName The name of the state store.
   * @param states The States to be saved.
   * @return a Mono plan of type Void.
   */
  Mono<Void> saveBulkState(String storeName, List<State<?>> states);

  /**
   * Save/Update a list of states.
   *
   * @param request Request to save states.
   * @return a Mono plan of type Void.
   */
  Mono<Void> saveBulkState(SaveStateRequest request);

  /**
   * Save/Update a state.
   *
   * @param storeName The name of the state store.
   * @param key            The key of the state.
   * @param value          The value of the state.
   * @return a Mono plan of type Void.
   */
  Mono<Void> saveState(String storeName, String key, Object value);

  /**
   * Save/Update a state.
   *
   * @param storeName The name of the state store.
   * @param key            The key of the state.
   * @param etag           The etag to be used.
   * @param value          The value of the state.
   * @param options        The Options to use for each state.
   * @return a Mono plan of type Void.
   */
  Mono<Void> saveState(String storeName, String key, String etag, Object value, StateOptions options);

  /**
   * Delete a state.
   *
   * @param storeName The name of the state store.
   * @param key            The key of the State to be removed.
   * @return a Mono plan of type Void.
   */
  Mono<Void>  deleteState(String storeName, String key);

  /**
   * Delete a state.
   *
   * @param storeName The name of the state store.
   * @param key            The key of the State to be removed.
   * @param etag           Optional etag for conditional delete.
   * @param options        Optional settings for state operation.
   * @return a Mono plan of type Void.
   */
  Mono<Void>  deleteState(String storeName, String key, String etag, StateOptions options);

  /**
   * Delete a state.
   *
   * @param request Request to delete a state.
   * @return a Mono plan of type Void.
   */
  Mono<Void> deleteState(DeleteStateRequest request);

  /**
   * Fetches a secret from the configured vault.
   *
   * @param storeName Name of vault component in Dapr.
   * @param secretName Secret to be fetched.
   * @param metadata Optional metadata.
   * @return Key-value pairs for the secret.
   */
  Mono<Map<String, String>> getSecret(String storeName, String secretName, Map<String, String> metadata);

  /**
   * Fetches a secret from the configured vault.
   *
   * @param storeName Name of vault component in Dapr.
   * @param secretName Secret to be fetched.
   * @return Key-value pairs for the secret.
   */
  Mono<Map<String, String>> getSecret(String storeName, String secretName);

  /**
   * Fetches a secret from the configured vault.
   *
   * @param request Request to fetch secret.
   * @return Key-value pairs for the secret.
   */
  Mono<Map<String, String>> getSecret(GetSecretRequest request);

  /**
   * Fetches all secrets from the configured vault.
   *
   * @param storeName Name of vault component in Dapr.
   * @return Key-value pairs for all the secrets in the state store.
   */
  Mono<Map<String, Map<String, String>>> getBulkSecret(String storeName);

  /**
   * Fetches all secrets from the configured vault.
   *
   * @param storeName Name of vault component in Dapr.
   * @param metadata Optional metadata.
   * @return Key-value pairs for all the secrets in the state store.
   */
  Mono<Map<String, Map<String, String>>> getBulkSecret(String storeName, Map<String, String> metadata);

  /**
   * Fetches all secrets from the configured vault.
   *
   * @param request Request to fetch secret.
   * @return Key-value pairs for the secret.
   */
  Mono<Map<String, Map<String, String>>> getBulkSecret(GetBulkSecretRequest request);

  /**
   * Retrieve a configuration based on a provided key.
   *
   * @param storeName Name of the configuration store
   * @param key       key of the configuration item which is to be retrieved
   * @return Mono of the Configuration Item
   */
  Mono<ConfigurationItem> getConfiguration(String storeName, String key);

  /**
   * Retrieve a configuration based on a provided key.
   *
   * @param storeName Name of the configuration store
   * @param key       key of the configuration item which is to be retrieved
   * @param metadata  optional metadata
   * @return Mono of the Configuration Item
   */
  Mono<ConfigurationItem> getConfiguration(String storeName, String key, Map<String, String> metadata);

  /**
   * Retrieve Map of configurations based on a provided variable number of keys.
   *
   * @param storeName Name of the configuration store
   * @param keys      keys of the configurations which are to be retrieved
   * @return Mono of Map of ConfigurationItems
   */
  Mono<Map<String, ConfigurationItem>> getConfiguration(String storeName, String... keys);

  /**
   * Retrieve Map of configurations based on a provided variable number of keys.
   *
   * @param storeName Name of the configuration store
   * @param keys      keys of the configurations which are to be retrieved
   * @param metadata  optional metadata
   * @return Mono of Map of ConfigurationItems
   */
  Mono<Map<String, ConfigurationItem>> getConfiguration(String storeName, List<String> keys,
                                                        Map<String, String> metadata);

  /**
   * Retrieve Map of configurations based on a provided configuration request object.
   *
   * @param request request for retrieving Configurations for a list keys
   * @return Mono of Map of ConfigurationItems
   */

  Mono<Map<String, ConfigurationItem>> getConfiguration(GetConfigurationRequest request);

  /**
   * Subscribe to the keys for any change.
   *
   * @param storeName Name of the configuration store
   * @param keys      keys of the configurations which are to be subscribed
   * @return Flux of {@link SubscribeConfigurationResponse} instance
   */
  Flux<SubscribeConfigurationResponse> subscribeConfiguration(String storeName, String... keys);

  /**
   * Subscribe to the keys for any change.
   *
   * @param storeName Name of the configuration store
   * @param keys      keys of the configurations which are to be subscribed
   * @param metadata  optional metadata
   * @return Flux of {@link SubscribeConfigurationResponse} instance
   */
  Flux<SubscribeConfigurationResponse> subscribeConfiguration(String storeName, List<String> keys,
                                                              Map<String, String> metadata);

  /**
   * Subscribe to the keys for any change.
   *
   * @param request request for subscribing to any change for the given keys in request
   * @return Flux of {@link SubscribeConfigurationResponse} instance
   */
  Flux<SubscribeConfigurationResponse> subscribeConfiguration(SubscribeConfigurationRequest request);

  /**
   * Unsubscribe from previously subscribed keys.
   *
   * @param id subscription id returned by subscribeConfiguration API.
   * @param storeName Name of the configuration store.
   * @return Mono of {@link UnsubscribeConfigurationResponse} instance.
   */
  Mono<UnsubscribeConfigurationResponse> unsubscribeConfiguration(String id, String storeName);

  /**
   * Unsubscribe from previously subscribed keys.
   *
   * @param request request for unsubscribing to any change for the given subscription id in request
   * @return Mono of {@link UnsubscribeConfigurationResponse} instance.
   */
  Mono<UnsubscribeConfigurationResponse> unsubscribeConfiguration(UnsubscribeConfigurationRequest request);

  /**
   * Returns a newly created gRPC stub with proper interceptors and channel for gRPC proxy invocation.
   * @param appId appId to be included in all gRPC calls for service invocation.
   * @param stubBuilder user-provided callback method to construct a new stub given the channel.
   * @return the gRPC stub with proper interceptors and channel.
   * @param <T> the generic type of the service to be invoked.
   */
  <T extends AbstractStub<T>> T newGrpcStub(String appId, Function<Channel, T> stubBuilder);

  /**
   * Fetches Dapr Metadata from the metadata endpoint.
   *
   * @return DaprMetadata containing Dapr Metadata from the metadata endpoint.
   */
  Mono<DaprMetadata> getMetadata();

  /**
   * Gracefully shutdown the dapr runtime.
   *
   * @return a Mono plan of type Void.
   */
  Mono<Void> shutdown();
}
