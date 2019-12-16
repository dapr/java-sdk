/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */
package io.dapr.actors.runtime;

import reactor.core.publisher.Mono;

/**
 * Interface for interacting from the actor app to the Dapr runtime.
 */
interface AppToDaprAsyncClient {

  /**
   * Gets a state from Dapr's Actor.
   *
   * @param actorType Type of actor.
   * @param actorId Actor Identifier.
   * @param keyName State name.
   * @return Asynchronous result with current state value.
   */
  Mono<String> getState(String actorType, String actorId, String keyName);

  /**
   * Saves state batch to Dapr.
   *
   * @param actorType Type of actor.
   * @param actorId Actor Identifier.
   * @param data State to be saved.
   * @return Asynchronous void result.
   */
  Mono<Void> saveStateTransactionally(String actorType, String actorId, String data);

  /**
   * Register a reminder.
   *
   * @param actorType Type of actor.
   * @param actorId Actor Identifier.
   * @param reminderName Name of reminder to be registered.
   * @param data JSON reminder data as per Dapr's spec.
   * @return Asynchronous void result.
   */
  Mono<Void> registerReminder(String actorType, String actorId, String reminderName, String data);

  /**
   * Unregisters a reminder.
   *
   * @param actorType Type of actor.
   * @param actorId Actor Identifier.
   * @param reminderName Name of reminder to be unregistered.
   * @return Asynchronous void result.
   */
  Mono<Void> unregisterReminder(String actorType, String actorId, String reminderName);

  /**
   * Registers a timer.
   *
   * @param actorType Type of actor.
   * @param actorId Actor Identifier.
   * @param timerName Name of timer to be registered.
   * @param data JSON reminder data as per Dapr's spec.
   * @return Asynchronous void result.
   */
  Mono<Void> registerTimer(String actorType, String actorId, String timerName, String data);

  /**
   * Unregisters a timer.
   *
   * @param actorType Type of actor.
   * @param actorId Actor Identifier.
   * @param timerName Name of timer to be unregistered.
   * @return Asynchronous void result.
   */
  Mono<Void> unregisterTimerAsync(String actorType, String actorId, String timerName);
}
