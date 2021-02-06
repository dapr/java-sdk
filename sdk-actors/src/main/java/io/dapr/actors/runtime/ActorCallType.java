/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

/**
 * Represents the call-type associated with the method invoked by actor runtime.
 */
enum ActorCallType {

  /**
   * Specifies that the method invoked is an actor interface method for a given
   * client request.
   */
  ACTOR_INTERFACE_METHOD,
  /**
   * Specifies that the method invoked is a timer callback method.
   */
  TIMER_METHOD,
  /**
   * Specifies that the method is when a reminder fires.
   */
  REMINDER_METHOD
}
