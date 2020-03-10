/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import io.dapr.client.domain.State;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.Verb;
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
   * @param event the event to be published, use byte[] for skipping serialization.
   * @return a Mono plan of type Void.
   */
  Mono<Void> publishEvent(String topic, Object event);

  /**
   * Publish an event.
   *
   * @param topic    the topic where the event will be published.
   * @param event    the event to be published, use byte[] for skipping serialization.
   * @param metadata The metadata for the published event.
   * @return a Mono plan of type Void.
   */
  Mono<Void> publishEvent(String topic, Object event, Map<String, String> metadata);

  /**
   * Invoke a service with all possible parameters, using serialization.
   *
   * @param verb    The Verb to be used for HTTP will be the HTTP Verb, for GRPC is just a metadata value.
   * @param appId   The Application ID where the service is.
   * @param method  The actual Method to be call in the application.
   * @param request The request to be sent to invoke the service, use byte[] to skip serialization.
   * @param metadata Metadata (in GRPC) or headers (in HTTP) to be send in request.
   * @param clazz   the Type needed as return for the call.
   * @param <T>     the Type of the return, use byte[] to skip serialization.
   * @return A Mono Plan of type clazz.
   */
  <T> Mono<T> invokeService(
      Verb verb, String appId, String method, Object request, Map<String, String> metadata, Class<T> clazz);

  /**
   * Invoke a service without metadata, using serialization.
   *
   * @param verb    The Verb to be used for HTTP will be the HTTP Verb, for GRPC is just a metadata value.
   * @param appId   The Application ID where the service is.
   * @param method  The actual Method to be call in the application.
   * @param request The request to be sent to invoke the service, use byte[] to skip serialization.
   * @param clazz   the Type needed as return for the call.
   * @param <T>     the Type of the return, use byte[] to skip serialization.
   * @return A Mono Plan of type clazz.
   */
  <T> Mono<T> invokeService(Verb verb, String appId, String method, Object request, Class<T> clazz);

  /**
   * Invoke a service without input, using serialization for response.
   *
   * @param verb    The Verb to be used for HTTP will be the HTTP Verb, for GRPC is just a metadata value.
   * @param appId   The Application ID where the service is.
   * @param method  The actual Method to be call in the application.
   * @param metadata Metadata (in GRPC) or headers (in HTTP) to be send in request.
   * @param clazz   the Type needed as return for the call.
   * @param <T>     the Type of the return, use byte[] to skip serialization.
   * @return A Mono plan of type clazz.
   */
  <T> Mono<T> invokeService(Verb verb, String appId, String method, Map<String, String> metadata, Class<T> clazz);

  /**
   * Invoke a service with void response, using serialization.
   *
   * @param verb    The Verb to be used for HTTP will be the HTTP Verb, for GRPC is just a metadata value.
   * @param appId   The Application ID where the service is.
   * @param method  The actual Method to be call in the application.
   * @param request The request to be sent to invoke the service, use byte[] to skip serialization.
   * @param metadata Metadata (in GRPC) or headers (in HTTP) to be send in request.
   * @return A Mono plan for Void.
   */
  Mono<Void> invokeService(Verb verb, String appId, String method, Object request, Map<String, String> metadata);

  /**
   * Invoke a service with void response, no metadata and using serialization.
   *
   * @param verb    The Verb to be used for HTTP will be the HTTP Verb, for GRPC is just a metadata value.
   * @param appId   The Application ID where the service is.
   * @param method  The actual Method to be call in the application.
   * @param request The request to be sent to invoke the service, use byte[] to skip serialization.
   * @return A Mono plan for Void.
   */
  Mono<Void> invokeService(Verb verb, String appId, String method, Object request);

  /**
   * Invoke a service without input and void response.
   *
   * @param verb    The Verb to be used for HTTP will be the HTTP Verb, for GRPC is just a metadata value.
   * @param appId   The Application ID where the service is.
   * @param method  The actual Method to be call in the application.
   * @param metadata Metadata (in GRPC) or headers (in HTTP) to be send in request.
   * @return A Mono plan for Void.
   */
  Mono<Void> invokeService(Verb verb, String appId, String method, Map<String, String> metadata);

  /**
   * Invoke a service without serialization.
   *
   * @param verb    The Verb to be used for HTTP will be the HTTP Verb, for GRPC is just a metadata value.
   * @param appId   The Application ID where the service is.
   * @param method  The actual Method to be call in the application.
   * @param request The request to be sent to invoke the service
   * @param metadata Metadata (in GRPC) or headers (in HTTP) to be send in request.
   * @return A Mono plan of byte[].
   */
  Mono<byte[]> invokeService(Verb verb, String appId, String method, byte[] request, Map<String, String> metadata);

  /**
   * Invokes a Binding.
   *
   * @param name    The name of the biding to call.
   * @param request The request needed for the binding, use byte[] to skip serialization.
   * @return a Mono plan of type Void.
   */
  Mono<Void> invokeBinding(String name, Object request);

  /**
   * Invokes a Binding with metadata.
   *
   * @param name     The name of the biding to call.
   * @param request  The request needed for the binding, use byte[] to skip serialization.
   * @param metadata The metadata map.
   * @return a Mono plan of type Void.
   */
  Mono<Void> invokeBinding(String name, Object request, Map<String, String> metadata);

  /**
   * Retrieve a State based on their key.
   *
   * @param stateStoreName The name of the state store.
   * @param state          State to be re-retrieved.
   * @param clazz          The Type of State needed as return.
   * @param <T>            The Type of the return.
   * @return A Mono Plan for the requested State.
   */
  <T> Mono<State<T>> getState(String stateStoreName, State<T> state, Class<T> clazz);

  /**
   * Retrieve a State based on their key.
   *
   * @param stateStoreName The name of the state store.
   * @param key            The key of the State to be retrieved.
   * @param clazz          The Type of State needed as return.
   * @param <T>            The Type of the return.
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
   * @param clazz          The Type of State needed as return.
   * @param <T>            The Type of the return.
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
