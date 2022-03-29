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

package io.dapr.actors.runtime;

import reactor.core.publisher.Mono;

import java.util.List;

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
  Mono<byte[]> getState(String actorType, String actorId, String keyName);

  /**
   * Saves state batch to Dapr.
   *
   * @param actorType  Type of actor.
   * @param actorId    Actor Identifier.
   * @param operations State transaction operations.
   * @return Asynchronous void result.
   */
  Mono<Void> saveStateTransactionally(String actorType, String actorId, List<ActorStateOperation> operations);

  /**
   * Register a reminder.
   *
   * @param actorType      Type of actor.
   * @param actorId        Actor Identifier.
   * @param reminderName   Name of reminder to be registered.
   * @param reminderParams Parameters for the reminder.
   * @return Asynchronous void result.
   */
  Mono<Void> registerReminder(
      String actorType,
      String actorId,
      String reminderName,
      ActorReminderParams reminderParams);

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
   * Register a timer.
   *
   * @param actorType   Type of actor.
   * @param actorId     Actor Identifier.
   * @param timerName   Name of reminder to be registered.
   * @param timerParams Parameters for the timer.
   * @return Asynchronous void result.
   */
  Mono<Void> registerTimer(String actorType, String actorId, String timerName, ActorTimerParams timerParams);

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
