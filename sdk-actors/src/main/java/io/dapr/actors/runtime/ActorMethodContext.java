/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

/**
 * Contains information about the method that is invoked by actor runtime.
 */
public class ActorMethodContext {

  /**
   * Method name to be invoked.
   */
  private final String methodName;

  /**
   * Call type to be used.
   */
  private final ActorCallType callType;

  /**
   * Constructs a new instance of {@link ActorMethodContext}, representing a call for an Actor.
   *
   * @param methodName Method name to be invoked.
   * @param callType   Call type to be used.
   */
  private ActorMethodContext(String methodName, ActorCallType callType) {
    this.methodName = methodName;
    this.callType = callType;
  }

  /**
   * Gets the name of the method invoked by actor runtime.
   *
   * @return The method name.
   */
  public String getMethodName() {
    return this.methodName;
  }

  /**
   * Gets the call type to be used.
   *
   * @return Call type.
   */
  public ActorCallType getCallType() {
    return this.callType;
  }

  /**
   * Creates a context to invoke an Actor's method.
   *
   * @param methodName THe method to be invoked.
   * @return Context of the method call as {@link ActorMethodContext}
   */
  static ActorMethodContext createForActor(String methodName) {
    return new ActorMethodContext(methodName, ActorCallType.ACTOR_INTERFACE_METHOD);
  }

  /**
   * Creates a context to invoke an Actor's timer.
   *
   * @param methodName THe method to be invoked.
   * @return Context of the method call as {@link ActorMethodContext}
   */
  static ActorMethodContext createForTimer(String methodName) {
    return new ActorMethodContext(methodName, ActorCallType.TIMER_METHOD);
  }

  /**
   * Creates a context to invoke an Actor's reminder.
   *
   * @param methodName THe method to be invoked.
   * @return Context of the method call as {@link ActorMethodContext}
   */
  static ActorMethodContext createForReminder(String methodName) {
    return new ActorMethodContext(methodName, ActorCallType.REMINDER_METHOD);
  }
}
