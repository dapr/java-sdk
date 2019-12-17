package io.dapr.actors.runtime;

import java.time.Duration;
import java.util.function.Function;

/**
 *  Represents the timer set on an Actor.
 */
public interface ActorTimer {

  /**
   * Gets the time when timer is first due.
   * @return Time as Duration when timer is first due.
   */
  Duration getDueTime();

  /**
   * Gets the periodic time when timer will be invoked.
   * @return Periodic time as <see cref="System.TimeSpan"/> when timer will be invoked.
   */
  Duration getPeriod();

  /**
   * Gets the name of the Timer. The name is unique per actor.
   * @return The name of the timer.
   */
  String getName();

  /**
   *
   * @return Gets a delegate that specifies a method to be called when the timer fires.
   *     It has one parameter: the state object passed to RegisterTimer.
   */
  Function<Object, Void> getAsyncCallback();

  /**
   *
   * @return Gets state containing information to be used by the callback method, or null.
   */
  Object getState();
}
