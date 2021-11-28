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

import io.dapr.utils.RepeatedDuration;

import java.time.Duration;
import java.util.Optional;

/**
 * Represents the timer set on an Actor, to be called once after due time and then every period.
 */
final class ActorTimerParams {

  /**
   * Name of the method to be called for this timer.
   */
  private final String callback;

  /**
   * State to be sent in the timer.
   */
  private final byte[] data;

  /**
   * Due time for the timer's first trigger.
   */
  private final Duration dueTime;

  /**
   * Period at which the timer will be triggered.
   * The number of repetition can also be configured in order to limit the total number of callback invocations.
   */
  private final RepeatedDuration period;

  /**
   * Time at which or time interval after which the timer will be expired and deleted.
   * If ttl is omitted, no restrictions are applied.
   */
  private final RepeatedDuration ttl;

  /**
   * Instantiates a new Actor Timer.
   *
   * @param callback The name of the method to be called for this timer.
   * @param data     The state to be used by the callback method
   * @param dueTime  The time when timer is first due.
   * @param period   The periodic time when timer will be invoked.
   */
  ActorTimerParams(String callback,
                   byte[] data,
                   Duration dueTime,
                   Duration period) {
    this(callback, data, dueTime, new RepeatedDuration(period), null);
  }

  /**
   * Instantiates a new Actor Timer.
   *
   * @param callback The name of the method to be called for this timer.
   * @param data     Data to be passed in as part of the reminder trigger.
   * @param dueTime  Time the reminder is due for the 1st time.
   * @param period   Interval between triggers.
   */
  ActorTimerParams(String callback,
                   byte[] data,
                   Duration dueTime,
                   RepeatedDuration period) {
    this(callback, data, dueTime, period, null);
  }

  /**
   * Instantiates a new Actor Timer.
   *
   * @param callback The name of the method to be called for this timer.
   * @param data     Data to be passed in as part of the reminder trigger.
   * @param dueTime  Time the reminder is due for the 1st time.
   * @param period   Interval between triggers.
   * @param ttl      Time at which or time interval after which the reminder will be expired and deleted.
   */
  ActorTimerParams(String callback,
                   byte[] data,
                   Duration dueTime,
                   RepeatedDuration period,
                   RepeatedDuration ttl) {
    this.callback = callback;
    this.data = data;
    this.dueTime = dueTime;
    this.period = period;
    this.ttl = ttl;
  }

  /**
   * Gets the name of the method for this Timer.
   *
   * @return The name of the method for this timer.
   */
  public String getCallback() {
    return this.callback;
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
   * @deprecated As of release 1.4, replaced by {@link #getRepeatedPeriod()}
   */
  @Deprecated
  public Duration getPeriod() {
    return this.period.getDuration();
  }

  /**
   * Gets the periodic time when timer will be invoked.
   * Possibly contains repetitions to limit the  the total number of callback invocations.
   *
   * @return Periodic time as {@link RepeatedDuration} when timer will be invoked.
   */
  public RepeatedDuration getRepeatedPeriod() {
    return this.period;
  }

  /**
   * Gets state containing information to be used by the callback method, or <code>null</code>.
   *
   * @return State containing information to be used by the callback method, or <code>null</code>.
   */
  public byte[] getData() {
    return this.data;
  }

  /**
   * Gets the time at which or time interval after which the timer will be expired and deleted.
   * This is an optional field and may return <code>null</code>.
   *
   * @return Time at which or time interval after which the timer will be expired and deleted, or <code>null</code>.
   */
  public Optional<RepeatedDuration> getTtl() {
    return Optional.ofNullable(this.ttl);
  }
}
