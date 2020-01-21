/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.runtime;

import reactor.core.publisher.Mono;

/**
 * Generic Client Adapter to be used regardless of the GRPC or the HTTP Client implementation required.
 */
interface DaprClient {

  /**
   * Gets a state from Dapr's Actor.
   *
   * @param actorType Type of actor.
   * @param actorId   Actor Identifier.
   * @param keyName   State name.
   * @return Asynchronous result with current state value.
   */
  Mono<byte[]> getActorState(String actorType, String actorId, String keyName);

  /**
   * Saves state batch to Dapr.
   *
   * @param actorType Type of actor.
   * @param actorId   Actor Identifier.
   * @param data      State to be saved.
   * @return Asynchronous void result.
   */
  Mono<Void> saveActorStateTransactionally(String actorType, String actorId, byte[] data);

  /**
   * Register a reminder.
   *
   * @param actorType    Type of actor.
   * @param actorId      Actor Identifier.
   * @param reminderName Name of reminder to be registered.
   * @param data         JSON reminder data as per Dapr's spec.
   * @return Asynchronous void result.
   */
  Mono<Void> registerActorReminder(String actorType, String actorId, String reminderName, byte[] data);

  /**
   * Unregisters a reminder.
   *
   * @param actorType    Type of actor.
   * @param actorId      Actor Identifier.
   * @param reminderName Name of reminder to be unregistered.
   * @return Asynchronous void result.
   */
  Mono<Void> unregisterActorReminder(String actorType, String actorId, String reminderName);

  /**
   * Registers a timer.
   *
   * @param actorType Type of actor.
   * @param actorId   Actor Identifier.
   * @param timerName Name of timer to be registered.
   * @param data      JSON reminder data as per Dapr's spec.
   * @return Asynchronous void result.
   */
  Mono<Void> registerActorTimer(String actorType, String actorId, String timerName, byte[] data);

  /**
   * Unregisters a timer.
   *
   * @param actorType Type of actor.
   * @param actorId   Actor Identifier.
   * @param timerName Name of timer to be unregistered.
   * @return Asynchronous void result.
   */
  Mono<Void> unregisterActorTimer(String actorType, String actorId, String timerName);
}
