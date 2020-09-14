/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import io.dapr.client.domain.DeleteStateRequest;
import io.dapr.client.domain.GetSecretRequest;
import io.dapr.client.domain.GetStateRequest;
import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.InvokeBindingRequest;
import io.dapr.client.domain.InvokeServiceRequest;
import io.dapr.client.domain.PublishEventRequest;
import io.dapr.client.domain.Response;
import io.dapr.client.domain.SaveStateRequest;
import io.dapr.client.domain.State;
import io.dapr.client.domain.StateOptions;
import io.dapr.utils.TypeRef;
import reactor.core.publisher.Mono;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

/**
 * Generic Client Adapter to be used regardless of the GRPC or the HTTP Client implementation required.
 *
 * @see io.dapr.client.DaprClientBuilder for information on how to make instance for this interface.
 */
public interface DaprClient extends Closeable {

  /**
   * Publish an event.
   *
   * @param pubsubName the pubsub name we will publish the event to
   * @param topic the topic where the event will be published.
   * @param data the event's data to be published, use byte[] for skipping serialization.
   * @return a Mono plan of type Void.
   */
  Mono<Void> publishEvent(String pubsubName, String topic, Object data);

  /**
   * Publish an event.
   *
   * @param pubsubName the pubsub name we will publish the event to
   * @param topic    the topic where the event will be published.
   * @param data    the event's data to be published, use byte[] for skipping serialization.
   * @param metadata The metadata for the published event.
   * @return a Mono plan of type Void.
   */
  Mono<Void> publishEvent(String pubsubName, String topic, Object data, Map<String, String> metadata);

  /**
   * Publish an event.
   *
   * @param request the request for the publish event.
   * @return a Mono plan of a Dapr's void response.
   */
  Mono<Response<Void>> publishEvent(PublishEventRequest request);

  /**
   * Invoke a service method, using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param method        The actual Method to be call in the application.
   * @param request       The request to be sent to invoke the service, use byte[] to skip serialization.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on
   *                      HTTP, {@link io.dapr.client.domain.HttpExtension#NONE} otherwise.
   * @param metadata      Metadata (in GRPC) or headers (in HTTP) to be sent in request.
   * @param type          The Type needed as return for the call.
   * @param <T>           The Type of the return, use byte[] to skip serialization.
   * @return A Mono Plan of type T.
   */
  <T> Mono<T> invokeService(String appId, String method, Object request, HttpExtension httpExtension,
                            Map<String, String> metadata, TypeRef<T> type);

  /**
   * Invoke a service method, using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param method        The actual Method to be call in the application.
   * @param request       The request to be sent to invoke the service, use byte[] to skip serialization.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on
   *                      HTTP, {@link HttpExtension#NONE} otherwise.
   * @param metadata      Metadata (in GRPC) or headers (in HTTP) to be sent in request.
   * @param clazz         The type needed as return for the call.
   * @param <T>           The Type of the return, use byte[] to skip serialization.
   * @return A Mono Plan of type T.
   */
  <T> Mono<T> invokeService(String appId, String method, Object request, HttpExtension httpExtension,
                            Map<String, String> metadata, Class<T> clazz);

  /**
   * Invoke a service method, using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param method        The actual Method to be call in the application.
   * @param request       The request to be sent to invoke the service, use byte[] to skip serialization.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on
   *                      HTTP, {@link HttpExtension#NONE} otherwise.
   * @param type          The Type needed as return for the call.
   * @param <T>           The Type of the return, use byte[] to skip serialization.
   * @return A Mono Plan of type T.
   */
  <T> Mono<T> invokeService(String appId, String method, Object request, HttpExtension httpExtension, TypeRef<T> type);

  /**
   * Invoke a service method, using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param method        The actual Method to be call in the application.
   * @param request       The request to be sent to invoke the service, use byte[] to skip serialization.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on
   *                      HTTP, {@link HttpExtension#NONE} otherwise.
   * @param clazz         The type needed as return for the call.
   * @param <T>           The Type of the return, use byte[] to skip serialization.
   * @return A Mono Plan of type T.
   */
  <T> Mono<T> invokeService(String appId, String method, Object request, HttpExtension httpExtension, Class<T> clazz);

  /**
   * Invoke a service method, using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param method        The actual Method to be call in the application.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on
   *                      HTTP, {@link HttpExtension#NONE} otherwise.
   * @param metadata      Metadata (in GRPC) or headers (in HTTP) to be sent in request.
   * @param type          The Type needed as return for the call.
   * @param <T>           The Type of the return, use byte[] to skip serialization.
   * @return A Mono Plan of type T.
   */
  <T> Mono<T> invokeService(String appId, String method, HttpExtension httpExtension, Map<String, String> metadata,
                            TypeRef<T> type);

  /**
   * Invoke a service method, using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param method        The actual Method to be call in the application.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on
   *                      HTTP, {@link HttpExtension#NONE} otherwise.
   * @param metadata      Metadata (in GRPC) or headers (in HTTP) to be sent in request.
   * @param clazz         The type needed as return for the call.
   * @param <T>           The Type of the return, use byte[] to skip serialization.
   * @return A Mono Plan of type T.
   */
  <T> Mono<T> invokeService(String appId, String method, HttpExtension httpExtension, Map<String, String> metadata,
                            Class<T> clazz);

  /**
   * Invoke a service method, using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param method        The actual Method to be call in the application.
   * @param request       The request to be sent to invoke the service, use byte[] to skip serialization.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on
   *                      HTTP, {@link HttpExtension#NONE} otherwise.
   * @param metadata      Metadata (in GRPC) or headers (in HTTP) to be sent in request.
   * @return A Mono Plan of type Void.
   */
  Mono<Void> invokeService(String appId, String method, Object request, HttpExtension httpExtension,
                            Map<String, String> metadata);

  /**
   * Invoke a service method, using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param method        The actual Method to be call in the application.
   * @param request       The request to be sent to invoke the service, use byte[] to skip serialization.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on
   *                      HTTP, {@link HttpExtension#NONE} otherwise.
   * @return A Mono Plan of type Void.
   */
  Mono<Void> invokeService(String appId, String method, Object request, HttpExtension httpExtension);

  /**
   * Invoke a service method, using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param method        The actual Method to be call in the application.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on
   *                      HTTP, {@link HttpExtension#NONE} otherwise.
   * @param metadata      Metadata (in GRPC) or headers (in HTTP) to be sent in request.
   * @return A Mono Plan of type Void.
   */
  Mono<Void> invokeService(String appId, String method, HttpExtension httpExtension, Map<String, String> metadata);

  /**
   * Invoke a service method, without using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param method        The actual Method to be call in the application.
   * @param request       The request to be sent to invoke the service, use byte[] to skip serialization.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on
   *                      HTTP, {@link HttpExtension#NONE} otherwise.
   * @param metadata      Metadata (in GRPC) or headers (in HTTP) to be sent in request.
   * @return A Mono Plan of type byte[].
   */
  Mono<byte[]> invokeService(String appId, String method, byte[] request, HttpExtension httpExtension,
                           Map<String, String> metadata);

  /**
   * Invoke a service method.
   *
   * @param invokeServiceRequest Request object.
   * @param type                 The Type needed as return for the call.
   * @param <T>                  The Type of the return, use byte[] to skip serialization.
   * @return A Mono Plan of type T.
   */
  <T> Mono<Response<T>> invokeService(InvokeServiceRequest invokeServiceRequest, TypeRef<T> type);

  /**
   * Invokes a Binding operation.
   *
   * @param name      The name of the biding to call.
   * @param operation The operation to be performed by the binding request processor.
   * @param data      The data to be processed, use byte[] to skip serialization.
   * @return an empty Mono.
   */
  Mono<Void> invokeBinding(String name, String operation, Object data);

  /**
   * Invokes a Binding operation, skipping serialization.
   *
   * @param name      The name of the biding to call.
   * @param operation The operation to be performed by the binding request processor.
   * @param data      The data to be processed, skipping serialization.
   * @param metadata  The metadata map.
   * @return a Mono plan of type byte[].
   */
  Mono<byte[]> invokeBinding(String name, String operation, byte[] data, Map<String, String> metadata);

  /**
   * Invokes a Binding operation.
   *
   * @param name      The name of the biding to call.
   * @param operation The operation to be performed by the binding request processor.
   * @param data      The data to be processed, use byte[] to skip serialization.
   * @param type      The type being returned.
   * @param <T>       The type of the return
   * @return a Mono plan of type T.
   */
  <T> Mono<T> invokeBinding(String name, String operation, Object data, TypeRef<T> type);

  /**
   * Invokes a Binding operation.
   *
   * @param name      The name of the biding to call.
   * @param operation The operation to be performed by the binding request processor.
   * @param data      The data to be processed, use byte[] to skip serialization.
   * @param clazz     The type being returned.
   * @param <T>       The type of the return
   * @return a Mono plan of type T.
   */
  <T> Mono<T> invokeBinding(String name, String operation, Object data, Class<T> clazz);

  /**
   * Invokes a Binding operation.
   *
   * @param name      The name of the biding to call.
   * @param operation The operation to be performed by the binding request processor.
   * @param data      The data to be processed, use byte[] to skip serialization.
   * @param metadata  The metadata map.
   * @param type      The type being returned.
   * @param <T>       The type of the return
   * @return a Mono plan of type T.
   */
  <T> Mono<T> invokeBinding(String name, String operation, Object data, Map<String, String> metadata, TypeRef<T> type);

  /**
   * Invokes a Binding operation.
   *
   * @param name      The name of the biding to call.
   * @param operation The operation to be performed by the binding request processor.
   * @param data      The data to be processed, use byte[] to skip serialization.
   * @param metadata  The metadata map.
   * @param clazz     The type being returned.
   * @param <T>       The type of the return
   * @return a Mono plan of type T.
   */
  <T> Mono<T> invokeBinding(String name, String operation, Object data, Map<String, String> metadata, Class<T> clazz);

  /**
   * Invokes a Binding operation.
   *
   * @param request The binding invocation request.
   * @param type    The type being returned.
   * @param <T>     The type of the return
   * @return a Mono plan of type T.
   */
  <T> Mono<Response<T>> invokeBinding(InvokeBindingRequest request, TypeRef<T> type);

  /**
   * Retrieve a State based on their key.
   *
   * @param stateStoreName The name of the state store.
   * @param state          State to be re-retrieved.
   * @param type           The type of State needed as return.
   * @param <T>            The type of the return.
   * @return A Mono Plan for the requested State.
   */
  <T> Mono<State<T>> getState(String stateStoreName, State<T> state, TypeRef<T> type);

  /**
   * Retrieve a State based on their key.
   *
   * @param stateStoreName The name of the state store.
   * @param state          State to be re-retrieved.
   * @param clazz          The type of State needed as return.
   * @param <T>            The type of the return.
   * @return A Mono Plan for the requested State.
   */
  <T> Mono<State<T>> getState(String stateStoreName, State<T> state, Class<T> clazz);

  /**
   * Retrieve a State based on their key.
   *
   * @param stateStoreName The name of the state store.
   * @param key            The key of the State to be retrieved.
   * @param type           The type of State needed as return.
   * @param <T>            The type of the return.
   * @return A Mono Plan for the requested State.
   */
  <T> Mono<State<T>> getState(String stateStoreName, String key, TypeRef<T> type);

  /**
   * Retrieve a State based on their key.
   *
   * @param stateStoreName The name of the state store.
   * @param key            The key of the State to be retrieved.
   * @param clazz          The type of State needed as return.
   * @param <T>            The type of the return.
   * @return A Mono Plan for the requested State.
   */
  <T> Mono<State<T>> getState(String stateStoreName, String key, Class<T> clazz);

  /**
   * Retrieve a State based on their key.
   *
   * @param stateStoreName The name of the state store.
   * @param key            The key of the State to be retrieved.
   * @param etag           Optional etag for conditional get
   * @param options        Optional settings for retrieve operation.
   * @param type           The Type of State needed as return.
   * @param <T>            The Type of the return.
   * @return A Mono Plan for the requested State.
   */
  <T> Mono<State<T>> getState(String stateStoreName, String key, String etag, StateOptions options, TypeRef<T> type);

  /**
   * Retrieve a State based on their key.
   *
   * @param request The request to get state.
   * @param type    The Type of State needed as return.
   * @param <T>     The Type of the return.
   * @return A Mono Plan for the requested State.
   */
  <T> Mono<Response<State<T>>> getState(GetStateRequest request, TypeRef<T> type);

  /**
   * Retrieve a State based on their key.
   *
   * @param stateStoreName The name of the state store.
   * @param key            The key of the State to be retrieved.
   * @param etag           Optional etag for conditional get
   * @param options        Optional settings for retrieve operation.
   * @param clazz          The type of State needed as return.
   * @param <T>            The type of the return.
   * @return A Mono Plan for the requested State.
   */
  <T> Mono<State<T>> getState(String stateStoreName, String key, String etag, StateOptions options, Class<T> clazz);

  /**
   * Save/Update a list of states.
   *
   * @param stateStoreName The name of the state store.
   * @param states The States to be saved.
   * @return a Mono plan of type Void.
   */
  Mono<Void> saveStates(String stateStoreName, List<State<?>> states);

  /**
   * Save/Update a list of states.
   *
   * @param request Request to save states.
   * @return a Mono plan of type Void.
   */
  Mono<Response<Void>> saveStates(SaveStateRequest request);

  /**
   * Save/Update a state.
   *
   * @param stateStoreName The name of the state store.
   * @param key            The key of the state.
   * @param value          The value of the state.
   * @return a Mono plan of type Void.
   */
  Mono<Void> saveState(String stateStoreName, String key, Object value);

  /**
   * Save/Update a state.
   *
   * @param stateStoreName The name of the state store.
   * @param key            The key of the state.
   * @param etag           The etag to be used.
   * @param value          The value of the state.
   * @param options        The Options to use for each state.
   * @return a Mono plan of type Void.
   */
  Mono<Void> saveState(String stateStoreName, String key, String etag, Object value, StateOptions options);

  /**
   * Delete a state.
   *
   * @param stateStoreName The name of the state store.
   * @param key            The key of the State to be removed.
   * @return a Mono plan of type Void.
   */
  Mono<Void>  deleteState(String stateStoreName, String key);

  /**
   * Delete a state.
   *
   * @param stateStoreName The name of the state store.
   * @param key            The key of the State to be removed.
   * @param etag           Optional etag for conditional delete.
   * @param options        Optional settings for state operation.
   * @return a Mono plan of type Void.
   */
  Mono<Void>  deleteState(String stateStoreName, String key, String etag, StateOptions options);

  /**
   * Delete a state.
   *
   * @param request Request to delete a state.
   * @return a Mono plan of type Void.
   */
  Mono<Response<Void>> deleteState(DeleteStateRequest request);

  /**
   * Fetches a secret from the configured vault.
   *
   * @param secretStoreName Name of vault component in Dapr.
   * @param secretName Secret to be fetched.
   * @param metadata Optional metadata.
   * @return Key-value pairs for the secret.
   */
  Mono<Map<String, String>> getSecret(String secretStoreName, String secretName, Map<String, String> metadata);

  /**
   * Fetches a secret from the configured vault.
   *
   * @param secretStoreName Name of vault component in Dapr.
   * @param secretName Secret to be fetched.
   * @return Key-value pairs for the secret.
   */
  Mono<Map<String, String>> getSecret(String secretStoreName, String secretName);

  /**
   * Fetches a secret from the configured vault.
   *
   * @param request Request to fetch secret.
   * @return Key-value pairs for the secret.
   */
  Mono<Response<Map<String, String>>> getSecret(GetSecretRequest request);
}