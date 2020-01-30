/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.client;

import io.dapr.client.domain.State;
import io.dapr.client.domain.StateOptions;
import io.dapr.client.domain.Verb;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Mono;

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
   * @param event the event to be published.
   * @param <T>   The type of event to be published, use byte[] for skipping serialization.
   * @return a Mono plan of type Void.
   */
  <T> Mono<Void> publishEvent(String topic, T event);

  /**
   * Publish an event.
   *
   * @param topic    the topic where the event will be published.
   * @param event    the event to be published.
   * @param metadata The metadata for the published event.
   * @param <T>      The type of event to be published, use byte[] for skipping serialization.
   * @return a Mono plan of type Void.
   */
  <T> Mono<Void> publishEvent(String topic, T event, Map<String, String> metadata);

  /**
   * Invoke a service with all possible parameters, using serialization.
   *
   * @param verb    The Verb to be used for HTTP will be the HTTP Verb, for GRPC is just a metadata value.
   * @param appId   The Application ID where the service is.
   * @param method  The actual Method to be call in the application.
   * @param request The request to be sent to invoke the service.
   * @param metadata Metadata (in GRPC) or headers (in HTTP) to be send in request.
   * @param clazz   the Type needed as return for the call.
   * @param <T>     the Type of the return, use byte[] to skip serialization.
   * @param <R>     The Type of the request, use byte[] to skip serialization.
   * @return A Mono Plan of type clazz.
   */
  <T, R> Mono<T> invokeService(
      Verb verb, String appId, String method, R request, Map<String, String> metadata, Class<T> clazz);

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
   * @param request The request to be sent to invoke the service.
   * @param metadata Metadata (in GRPC) or headers (in HTTP) to be send in request.
   * @param <R>     The Type of the request, use byte[] to skip serialization.
   * @return A Mono plan for Void.
   */
  <R> Mono<Void> invokeService(Verb verb, String appId, String method, R request, Map<String, String> metadata);

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
   * Creating a Binding.
   *
   * @param name    The name of the biding to call.
   * @param request The request needed for the binding.
   * @param <T>     The type of the request.
   * @return a Mono plan of type Void
   */
  <T> Mono<Void> invokeBinding(String name, T request);

  /**
   * Retrieve a State based on their key.
   *
   * @param state   State to be re-retrieved.
   * @param clazz   The Type of State needed as return.
   * @param <T>     The Type of the return.
   * @return A Mono Plan for the requested State.
   */
  <T> Mono<State<T>> getState(State<T> state, Class<T> clazz);

  /**
   * Retrieve a State based on their key.
   *
   * @param key     The key of the State to be retrieved.
   * @param clazz   The Type of State needed as return.
   * @param <T>     The Type of the return.
   * @return A Mono Plan for the requested State.
   */
  <T> Mono<State<T>> getState(String key, Class<T> clazz);

  /**
   * Retrieve a State based on their key.
   *
   * @param key     The key of the State to be retrieved.
   * @param etag    Optional etag for conditional get
   * @param options Optional settings for retrieve operation.
   * @param clazz   The Type of State needed as return.
   * @param <T>     The Type of the return.
   * @return A Mono Plan for the requested State.
   */
  <T> Mono<State<T>> getState(String key, String etag, StateOptions options, Class<T> clazz);

  /**
   * Save/Update a list of states.
   *
   * @param states  the States to be saved.
   * @return a Mono plan of type Void.
   */
  Mono<Void> saveStates(List<State<?>> states);

  /**
   * Save/Update a state.
   *
   * @param key     the key of the state.
   * @param value   the value of the state.
   * @return a Mono plan of type Void.
   */
  Mono<Void> saveState(String key, Object value);

  /**
   * Save/Update a state.
   *
   * @param key     the key of the state.
   * @param etag    the etag to be used.
   * @param value   the value of the state.
   * @param options the Options to use for each state.
   * @return a Mono plan of type Void.
   */
  Mono<Void> saveState(String key, String etag, Object value, StateOptions options);

  /**
   * Delete a state.
   *
   * @param key     The key of the State to be removed.
   * @return a Mono plan of type Void.
   */
  Mono<Void>  deleteState(String key);

  /**
   * Delete a state.
   *
   * @param key     The key of the State to be removed.
   * @param etag    Optional etag for conditional delete.
   * @param options Optional settings for state operation.
   * @return a Mono plan of type Void.
   */
  Mono<Void>  deleteState(String key, String etag, StateOptions options);
}
