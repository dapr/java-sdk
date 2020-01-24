/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import java.time.Duration;
import reactor.core.publisher.Mono;

/**
 * Interface that actors must implement to consume reminders registered using RegisterReminderAsync.
 */
public interface Remindable<T> {

  /**
   * Gets the class for state object.
   *
   * @return Class for state object.
   */
  Class<T> getStateType();

  /**
   * The reminder call back invoked when an actor reminder is triggered.
   * The state of this actor is saved by the actor runtime upon completion of the task returned by this method.
   * If an error occurs while saving the state, then all state cached by this actor's {@link ActorStateManager} will
   * be discarded and reloaded from previously saved state when the next actor method or reminder invocation occurs.
   *
   * @param reminderName The name of reminder provided during registration.
   * @param state        The user state provided during registration.
   * @param dueTime      The invocation due time provided during registration.
   * @param period       The invocation period provided during registration.
   * @return A task that represents the asynchronous operation performed by this callback.
   */
  Mono<Void> receiveReminder(String reminderName, T state, Duration dueTime, Duration period);
}
