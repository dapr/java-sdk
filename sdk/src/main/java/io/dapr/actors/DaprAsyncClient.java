/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors;

import reactor.core.publisher.Mono;

/**
 * Interface for interacting with Dapr runtime.
 */
public interface DaprAsyncClient {

    /**
     * Invokes an Actor method on Dapr.
     * @param actorType Type of actor.
     * @param actorId Actor Identifier.
     * @param methodName Method name to invoke.
     * @param jsonPayload Serialized body.
     * @return Asynchronous result with the Actor's response.
     */
    Mono<String> invokeActorMethod(String actorType, String actorId, String methodName, String jsonPayload);

    /**
     * Gets a state from Dapr's Actor.
     * @param actorType Type of actor.
     * @param actorId Actor Identifier.
     * @param keyName State name.
     * @return Asynchronous result with current state value.
     */
    Mono<String> getState(String actorType, String actorId, String keyName);

    /**
     * Removes Actor state in Dapr. This is temporary until the Dapr runtime implements the Batch state update.
     * @param actorType Type of actor.
     * @param actorId Actor Identifier.
     * @param keyName State name.
     * @return Asynchronous void result.
     */
    Mono<Void> removeState(String actorType, String actorId, String keyName);

    /**
     * Saves state batch to Dapr.
     * @param actorType Type of actor.
     * @param actorId Actor Identifier.
     * @param data State to be saved.
     * @return Asynchronous void result.
     */
    Mono<Void> saveStateTransactionally(String actorType, String actorId, String data);

    /**
     * Register a reminder.
     * @param actorType Type of actor.
     * @param actorId Actor Identifier.
     * @param reminderName Name of reminder to be registered.
     * @param data JSON reminder data as per Dapr's spec.
     * @return Asynchronous void result.
     */
    Mono<Void> registerReminder(String actorType, String actorId, String reminderName, String data);

    /**
     * Unregisters a reminder.
     * @param actorType Type of actor.
     * @param actorId Actor Identifier.
     * @param reminderName Name of reminder to be unregistered.
     * @return Asynchronous void result.
     */
    Mono<Void> unregisterReminder(String actorType, String actorId, String reminderName);

    /**
     * Registers a timer.
     * @param actorType Type of actor.
     * @param actorId Actor Identifier.
     * @param timerName Name of timer to be registered.
     * @param data JSON reminder data as per Dapr's spec.
     * @return Asynchronous void result.
     */
    Mono<Void> registerTimer(String actorType, String actorId, String timerName, String data);

    /**
     * Unregisters a timer.
     * @param actorType Type of actor.
     * @param actorId Actor Identifier.
     * @param timerName Name of timer to be unregistered.
     * @return Asynchronous void result.
     */
    Mono<Void> unregisterTimerAsync(String actorType, String actorId, String timerName);
}
