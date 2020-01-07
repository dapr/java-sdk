/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.client;

import io.dapr.client.domain.StateKeyValue;
import io.dapr.client.domain.StateOptions;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Generic Client Adapter to be used regardless of the GRPC or the HTTP Client implementation required.
 *
 * @see io.dapr.client.DaprClientBuilder for information on how to make instance for this interface.
 */
public interface DaprClient {

  /**
   * Publish an event.
   * @param topic the topic where the event will be published
   * @param event the event to be published
   * @param <T>   The type of event to be publishded.
   * @return a Mono plan of type Void
   */
  <T> Mono<Void> publishEvent(String topic, T event);

  /**
   * Invoke a service
   * @param verb    The Verb to be used for HTTP will be the HTTP Verb, for GRPC is just a metadata value.
   * @param appId   The Application ID where the service is
   * @param method  The actual Method to be call in the application.
   * @param request The request to be sent to invoke the service
   * @param clazz   the Type needed as return for the call
   * @param <T>     the Type of the return
   * @param <K>     The Type of the request.
   * @return A Mono Plan of type clazz
   */
  <T, K> Mono<T> invokeService(String verb, String appId, String method, K request, Class<T> clazz);

  /**
   * Invoke a service
   * @param verb    The Verb to be used for HTTP will be the HTTP Verb, for GRPC is just a metadata value.
   * @param appId   The Application ID where the service is
   * @param method  The actual Method to be call in the application.
   * @param request The request to be sent to invoke the service
   * @param <T>     the Type of the requet
   * @return a Mono plan of type Void
   */
  <T> Mono<Void> invokeService(String verb, String appId, String method, T request);

  /**
   * Creating a Binding
   * @param name    The name of the biding to call
   * @param request The request needed for the binding
   * @param <T>     The type of the request.
   * @return a Mono plan of type Void
   */
  <T> Mono<Void> invokeBinding(String name, T request);

  /**
   * Retrieve a State based on their key.
   *
   * @param state   The key of the State to be retrieved
   * @param stateOptions
   * @param clazz the Type of State needed as return.
   * @param <T>   the Type of the return
   * @param <K>   The Type of the key of the State
   * @return A Mono Plan for the requested State
   */
  <T, K> Mono<T> getState(StateKeyValue<K> state, StateOptions stateOptions, Class<T> clazz);

  /**
   * Save/Update a list of states.
   *
   * @param states the States to be saved
   * @param <T>   the Type of the State
   * @return a Mono plan of type Void
   */
  <T> Mono<Void> saveStates(List<StateKeyValue<T>> states);

  /**
   * Save/Update a state
   * @param key    the key of the state
   * @param etag   the etag to be used
   * @param value  the value of the state
   * @param <T>    the Type of the State
   * @return a Mono plan of type Void
   */
  <T> Mono<Void> saveState(String key, String etag, T value);

  /**
   * Delete a state
   *
   * @param state        The key of the State to be removed
   * @param stateOptions The options of the state
   * @param <T>          The Type of the key of the State
   * @return a Mono plan of type Void
   */
  <T> Mono<Void> deleteState(StateKeyValue<T> state, StateOptions stateOptions);

  /**
   * Invokes an Actor method on Dapr.
   *
   * @param actorType   Type of actor.
   * @param actorId     Actor Identifier.
   * @param methodName  Method name to invoke.
   * @param jsonPayload Serialized body.
   * @return Asynchronous result with the Actor's response.
   */
  Mono<String> invokeActorMethod(String actorType, String actorId, String methodName, String jsonPayload);

  /**
   * Gets a state from Dapr's Actor.
   *
   * @param actorType Type of actor.
   * @param actorId   Actor Identifier.
   * @param keyName   State name.
   * @return Asynchronous result with current state value.
   */
  Mono<String> getActorState(String actorType, String actorId, String keyName);

  /**
   * Saves state batch to Dapr.
   *
   * @param actorType Type of actor.
   * @param actorId   Actor Identifier.
   * @param data      State to be saved.
   * @return Asynchronous void result.
   */
  Mono<Void> saveStateTransactionally(String actorType, String actorId, String data);

  /**
   * Register a reminder.
   *
   * @param actorType    Type of actor.
   * @param actorId      Actor Identifier.
   * @param reminderName Name of reminder to be registered.
   * @param data         JSON reminder data as per Dapr's spec.
   * @return Asynchronous void result.
   */
  Mono<Void> registerReminder(String actorType, String actorId, String reminderName, String data);

  /**
   * Unregisters a reminder.
   *
   * @param actorType    Type of actor.
   * @param actorId      Actor Identifier.
   * @param reminderName Name of reminder to be unregistered.
   * @return Asynchronous void result.
   */
  Mono<Void> unregisterReminder(String actorType, String actorId, String reminderName);

  /**
   * Registers a timer.
   *
   * @param actorType Type of actor.
   * @param actorId   Actor Identifier.
   * @param timerName Name of timer to be registered.
   * @param data      JSON reminder data as per Dapr's spec.
   * @return Asynchronous void result.
   */
  Mono<Void> registerTimer(String actorType, String actorId, String timerName, String data);

  /**
   * Unregisters a timer.
   *
   * @param actorType Type of actor.
   * @param actorId   Actor Identifier.
   * @param timerName Name of timer to be unregistered.
   * @return Asynchronous void result.
   */
  Mono<Void> unregisterTimer(String actorType, String actorId, String timerName);
}
