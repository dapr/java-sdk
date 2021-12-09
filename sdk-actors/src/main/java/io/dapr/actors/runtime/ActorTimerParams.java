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

import java.time.Duration;

/**
 * Represents the timer set on an Actor, to be called once after due time and then every period.
 *
 */
final class ActorTimerParams {

  /**
   * Name of the method to be called for this timer.
   */
  private String callback;

  /**
   * State to be sent in the timer.
   */
  private byte[] data;

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
   * @param callback  The name of the method to be called for this timer.
   * @param data      The state to be used by the callback method
   * @param dueTime   The time when timer is first due.
   * @param period    The periodic time when timer will be invoked.
   */
  ActorTimerParams(String callback,
                   byte[] data,
                   Duration dueTime,
                   Duration period) {
    this.callback = callback;
    this.data = data;
    this.dueTime = dueTime;
    this.period = period;
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
   */
  public Duration getPeriod() {
    return this.period;
  }

  /**
   * Gets state containing information to be used by the callback method, or null.
   *
   * @return State containing information to be used by the callback method, or null.
   */
  public byte[] getData() {
    return this.data;
  }

}
