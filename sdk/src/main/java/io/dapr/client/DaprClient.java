/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import io.dapr.client.domain.State;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.Verb;
import io.dapr.utils.TypeRef;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Generic Client Adapter to be used regardless of the GRPC or the HTTP Client implementation required.
 *
 * @see io.dapr.client.DaprClientBuilder for information on how to make instance for this interface.
 */
public interface DaprClient {

  /**
   * Publish an event.
   *
   * @param topic the topic where the event will be published.
   * @param data the event's data to be published, use byte[] for skipping serialization.
   * @return a Mono plan of type Void.
   */
  Mono<Void> publishEvent(String topic, Object data);

  /**
   * Publish an event.
   *
   * @param topic    the topic where the event will be published.
   * @param data    the event's data to be published, use byte[] for skipping serialization.
   * @param metadata The metadata for the published event.
   * @return a Mono plan of type Void.
   */
  Mono<Void> publishEvent(String topic, Object data, Map<String, String> metadata);

  /**
   * Invoke a service with all possible parameters, using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param method        The actual Method to be call in the application.
   * @param request       The request to be sent to invoke the service, use byte[] to skip serialization.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on HTTP.
   * @param metadata      Metadata (in GRPC) or headers (in HTTP) to be sent in request.
   * @param type          The Type needed as return for the call.
   * @param <T>           The Type of the return, use byte[] to skip serialization.
   * @return A Mono Plan of type type.
   */
  <T> Mono<T> invokeService(String appId, String method, Object request, HttpExtension httpExtension,
                            Map<String, String> metadata, TypeRef<T> type);

  /**
   * Invoke a service with all possible parameters, using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param method        The actual Method to be call in the application.
   * @param request       The request to be sent to invoke the service, use byte[] to skip serialization.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on HTTP.
   * @param metadata      Metadata (in GRPC) or headers (in HTTP) to be sent in request.
   * @param clazz         The type needed as return for the call.
   * @param <T>           The Type of the return, use byte[] to skip serialization.
   * @return A Mono Plan of type type.
   */
  <T> Mono<T> invokeService(String appId, String method, Object request, HttpExtension httpExtension,
                            Map<String, String> metadata, Class<T> clazz);

  /**
   * Invoke a service with all possible parameters, using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param method        The actual Method to be call in the application.
   * @param request       The request to be sent to invoke the service, use byte[] to skip serialization.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on HTTP.
   * @param type          The Type needed as return for the call.
   * @param <T>           The Type of the return, use byte[] to skip serialization.
   * @return A Mono Plan of type type.
   */
  <T> Mono<T> invokeService(String appId, String method, Object request, HttpExtension httpExtension, TypeRef<T> type);

  /**
   * Invoke a service with all possible parameters, using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param method        The actual Method to be call in the application.
   * @param request       The request to be sent to invoke the service, use byte[] to skip serialization.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on HTTP.
   * @param clazz         The type needed as return for the call.
   * @param <T>           The Type of the return, use byte[] to skip serialization.
   * @return A Mono Plan of type type.
   */
  <T> Mono<T> invokeService(String appId, String method, Object request, HttpExtension httpExtension, Class<T> clazz);

  /**
   * Invoke a service with all possible parameters, using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param method        The actual Method to be call in the application.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on HTTP.
   * @param metadata      Metadata (in GRPC) or headers (in HTTP) to be sent in request.
   * @param type          The Type needed as return for the call.
   * @param <T>           The Type of the return, use byte[] to skip serialization.
   * @return A Mono Plan of type type.
   */
  <T> Mono<T> invokeService(String appId, String method, HttpExtension httpExtension, Map<String, String> metadata,
                            TypeRef<T> type);

  /**
   * Invoke a service with all possible parameters, using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param method        The actual Method to be call in the application.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on HTTP.
   * @param metadata      Metadata (in GRPC) or headers (in HTTP) to be sent in request.
   * @param clazz         The type needed as return for the call.
   * @param <T>           The Type of the return, use byte[] to skip serialization.
   * @return A Mono Plan of type type.
   */
  <T> Mono<T> invokeService(String appId, String method, HttpExtension httpExtension, Map<String, String> metadata,
                            Class<T> clazz);

  /**
   * Invoke a service with all possible parameters, using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param method        The actual Method to be call in the application.
   * @param request       The request to be sent to invoke the service, use byte[] to skip serialization.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on HTTP.
   * @param metadata      Metadata (in GRPC) or headers (in HTTP) to be sent in request.
   * @return A Mono Plan of type type.
   */
  Mono<Void> invokeService(String appId, String method, Object request, HttpExtension httpExtension,
                            Map<String, String> metadata);

  /**
   * Invoke a service with all possible parameters, using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param method        The actual Method to be call in the application.
   * @param request       The request to be sent to invoke the service, use byte[] to skip serialization.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on HTTP.
   * @return A Mono Plan of type type.
   */
  Mono<Void> invokeService(String appId, String method, Object request, HttpExtension httpExtension);

  /**
   * Invoke a service with all possible parameters, using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param method        The actual Method to be call in the application.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on HTTP.
   * @param metadata      Metadata (in GRPC) or headers (in HTTP) to be sent in request.
   * @return A Mono Plan of type type.
   */
  Mono<Void> invokeService(String appId, String method, HttpExtension httpExtension, Map<String, String> metadata);

  /**
   * Invoke a service with all possible parameters, using serialization.
   *
   * @param appId         The Application ID where the service is.
   * @param method        The actual Method to be call in the application.
   * @param request       The request to be sent to invoke the service, use byte[] to skip serialization.
   * @param httpExtension Additional fields that are needed if the receiving app is listening on HTTP.
   * @param metadata      Metadata (in GRPC) or headers (in HTTP) to be sent in request.
   * @return A Mono Plan of type type.
   */
  Mono<byte[]> invokeService(String appId, String method, byte[] request, HttpExtension httpExtension,
                           Map<String, String> metadata);

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
}
