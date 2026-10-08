/*
 * Copyright 2026 The Dapr Authors
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

import io.dapr.utils.DurationUtils;

import java.time.Duration;

/**
 * Helpers to validate and format the schedule (due time, period, repetitions and TTL) of timers and reminders.
 */
final class ActorScheduleUtils {

  private ActorScheduleUtils() {
  }

  /**
   * Validates the repetitions and TTL of a timer or reminder, throws {@link IllegalArgumentException}.
   *
   * @param period      Interval between triggers.
   * @param repetitions Number of times to trigger, or null for unlimited.
   * @param ttl         Time after which the timer or reminder expires, or null for no expiration.
   */
  static void validate(Duration period, Integer repetitions, Duration ttl) {
    if (repetitions != null) {
      if (repetitions <= 0) {
        throw new IllegalArgumentException("repetitions must be greater than zero");
      }
      if (period == null || period.isZero() || period.isNegative()) {
        throw new IllegalArgumentException("repetitions requires a positive period");
      }
      if (period.getNano() != 0) {
        throw new IllegalArgumentException("period must be in whole seconds when repetitions are set");
      }
    }
    if (ttl != null) {
      if (ttl.isNegative()) {
        throw new IllegalArgumentException("ttl must not be negative");
      }
      // The Dapr scheduler rejects a TTL for jobs without a schedule.
      if (period == null || period.isZero() || period.isNegative()) {
        throw new IllegalArgumentException("ttl requires a positive period");
      }
    }
  }

  /**
   * Formats a due time for the Dapr runtime. An unset due time is sent as zero so the timer or reminder fires
   * right away; an empty value would make the Dapr scheduler wait one period before the first trigger.
   *
   * @param dueTime Duration or null.
   * @return Formatted value.
   */
  static String formatDueTime(Duration dueTime) {
    return DurationUtils.convertDurationToDaprFormat(dueTime == null ? Duration.ZERO : dueTime);
  }

  /**
   * Formats a TTL or period for the Dapr runtime, an empty string means not set.
   *
   * @param value Duration or null.
   * @return Formatted value.
   */
  static String formatDuration(Duration value) {
    return value == null ? "" : DurationUtils.convertDurationToDaprFormat(value);
  }

  /**
   * Formats a period for the Dapr runtime, using the ISO 8601 format (e.g. R5/PT10S) when repetitions are set.
   *
   * @param period      Interval between triggers or null.
   * @param repetitions Number of times to trigger, or null for unlimited.
   * @return Formatted value.
   */
  static String formatPeriod(Duration period, Integer repetitions) {
    if (repetitions != null) {
      return "R" + repetitions + "/" + DurationUtils.convertDurationToIso8601Format(period);
    }
    return formatDuration(period);
  }

  /**
   * Extracts the repetitions from a period in the ISO 8601 format (e.g. R5/PT10S).
   *
   * @param period Period as sent by the Dapr runtime.
   * @return Repetitions or null if not set.
   */
  static Integer parseRepetitions(String period) {
    if (period == null || !period.startsWith("R")) {
      return null;
    }
    int separator = period.indexOf('/');
    String value = separator == -1 ? period.substring(1) : period.substring(1, separator);
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
