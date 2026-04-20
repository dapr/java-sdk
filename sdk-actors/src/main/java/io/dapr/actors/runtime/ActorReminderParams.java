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

import io.dapr.client.domain.FailurePolicy;

import java.time.Duration;

/**
 * Parameters for Actor Reminder.
 */
final class ActorReminderParams {

  /**
   * Minimum duration for period.
   */
  private static final Duration MIN_TIME_PERIOD = Duration.ofMillis(-1);

  /**
   * Data to be passed in as part of the reminder trigger.
   */
  private final byte[] data;

  /**
   * Time the reminder is due for the 1st time.
   */
  private final Duration dueTime;

  /**
   * Interval between triggers.
   */
  private final Duration period;

  /**
   * Failure Policy.
   */
  private FailurePolicy failurePolicy;

  /**
   * Instantiates a new instance for the params of a reminder.
   *
   * @param data    Data to be passed in as part of the reminder trigger.
   * @param dueTime Time the reminder is due for the 1st time.
   * @param period  Interval between triggers.
   */
  ActorReminderParams(byte[] data, Duration dueTime, Duration period) {
    validateDueTime("DueTime", dueTime);
    validatePeriod("Period", period);
    this.data = data;
    this.dueTime = dueTime;
    this.period = period;
  }

  /**
   * Instantiates a new instance for the params of a reminder.
   *
   * @param data    Data to be passed in as part of the reminder trigger.
   * @param dueTime Time the reminder is due for the 1st time.
   * @param period  Interval between triggers.
   */
  ActorReminderParams(byte[] data, Duration dueTime, Duration period, FailurePolicy failurePolicy) {
    this(data, dueTime, period);
    this.failurePolicy = failurePolicy;
  }

  /**
   * Gets the time the reminder is due for the 1st time.
   *
   * @return Time the reminder is due for the 1st time.
   */
  Duration getDueTime() {
    return dueTime;
  }

  /**
   * Gets the interval between triggers.
   *
   * @return Interval between triggers.
   */
  Duration getPeriod() {
    return period;
  }

  /**
   * Gets the data to be passed in as part of the reminder trigger.
   *
   * @return Data to be passed in as part of the reminder trigger.
   */
  byte[] getData() {
    return data;
  }

  /**
   * Validates due time is valid, throws {@link IllegalArgumentException}.
   *
   * @param argName Name of the argument passed in.
   * @param value   Vale being checked.
   */
  private static void validateDueTime(String argName, Duration value) {
    if (value.compareTo(Duration.ZERO) < 0) {
      String message = String.format(
            "argName: %s - Duration toMillis() - specified value must be greater than %s", argName, Duration.ZERO);
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * Validates reminder period is valid, throws {@link IllegalArgumentException}.
   *
   * @param argName Name of the argument passed in.
   * @param value   Vale being checked.
   */
  private static void validatePeriod(String argName, Duration value) throws IllegalArgumentException {
    if (value.compareTo(MIN_TIME_PERIOD) < 0) {
      String message = String.format(
            "argName: %s - Duration toMillis() - specified value must be greater than %s", argName, MIN_TIME_PERIOD);
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * Gets the failure policy.
   *
   * @return the failure policy
   */
  public FailurePolicy getFailurePolicy() {
    return failurePolicy;
  }
}
