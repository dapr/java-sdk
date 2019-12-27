/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.runtime;

import java.time.Duration;

/**
 * Represents the timer set on an Actor, to be called once after due time and then every period.
 * @param <T> State type.
 */
final class ActorTimer<T> {

  /**
   * Actor that owns this timer.
   */
  private final AbstractActor owner;

  /**
   * Name of this timer.
   */
  private String name;

  /**
   * Name of the method to be called for this timer.
   */
  private String methodName;

  /**
   * State to be sent in the timer.
   */
  private T state;

  /**
   * Due time for the timer's first trigger.
   */
  private Duration dueTime;

  /**
   * Period at which the timer will be triggered.
   */
  private Duration period;

  /**
   * Instantiates a new Actor Timer.
   *
   * @param owner      The Actor that owns this timer.  The timer callback will be fired for this Actor.
   * @param timerName  The name of the timer.
   * @param methodName The name of the method to be called for this timer.
   * @param state      information to be used by the callback method
   * @param dueTime    the time when timer is first due.
   * @param period     the periodic time when timer will be invoked.
   */
  ActorTimer(AbstractActor owner,
                    String timerName,
                    String methodName,
                    T state,
                    Duration dueTime,
                    Duration period) {
    this.owner = owner;
    this.name = timerName;
    this.methodName = methodName;
    this.state = state;
    this.dueTime = dueTime;
    this.period = period;
  }

  /**
   * Gets the name of the Timer. The name is unique per actor.
   *
   * @return The name of the timer.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Gets the name of the method for this Timer.
   *
   * @return The name of the method for this timer.
   */
  public String getMethodName() {
    return this.methodName;
  }

  /**
   * Gets the time when timer is first due.
   *
   * @return Time as Duration when timer is first due.
   */
  public Duration getDueTime() {
    return this.dueTime;
  }

  /**
   * Gets the periodic time when timer will be invoked.
   *
   * @return Periodic time as Duration when timer will be invoked.
   */
  public Duration getPeriod() {
    return this.period;
  }

  /**
   * Gets state containing information to be used by the callback method, or null.
   *
   * @return State containing information to be used by the callback method, or null.
   */
  public T getState() {
    return this.state;
  }

}