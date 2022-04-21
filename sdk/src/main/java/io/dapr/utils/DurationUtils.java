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

package io.dapr.utils;

import java.time.Duration;

public class DurationUtils {

  /**
   * Converts time from the String format used by Dapr into a Duration.
   *
   * @param valueString A String representing time in the Dapr runtime's format (e.g. 4h15m50s60ms).
   * @return A Duration
   */
  public static Duration convertDurationFromDaprFormat(String valueString) {
    // Convert the format returned by the Dapr runtime into Duration
    // An example of the format is: 4h15m50s60ms. It does not include days.
    if (valueString == null) {
      throw new IllegalArgumentException("duration string cannot be null");
    }
    Duration parsedDuration = Duration.ZERO;
    int hourIndex = valueString.indexOf('h');
    // Get first occurrence of "m"
    int minuteIndex = valueString.indexOf('m');
    // Check if that is not "ms"
    if (minuteIndex != -1 && valueString.length() > minuteIndex + 1 && valueString.charAt(minuteIndex + 1) == 's') {
      // condition satisfied when "m" is part of the string "ms"
      minuteIndex = -1;
    }
    // Get first occurrence of "s"
    int secondIndex = valueString.indexOf('s');
    if (secondIndex != -1 && valueString.charAt(secondIndex - 1) == 'm') {
      // condition satisfied when "s" is part of the string "ms"
      secondIndex = -1;
    }
    // Declaring it final to skip checkstyle issues
    final int milliIndex = valueString.indexOf("ms");

    if (hourIndex != -1) {
      String hoursSpan = valueString.substring(0, hourIndex);

      int hours = Integer.parseInt(hoursSpan);
      int days = hours / 24;
      hours = hours % 24;
      parsedDuration = parsedDuration.plusDays(days)
          .plusHours(hours);
    }

    if (minuteIndex != -1) {
      String minutesSpan = valueString.substring(hourIndex + 1, minuteIndex);
      int minutes = Integer.parseInt(minutesSpan);
      parsedDuration = parsedDuration.plusMinutes(minutes);
    }

    if (secondIndex != -1) {
      String secondsSpan = valueString.substring(minuteIndex + 1, secondIndex);
      int seconds = Integer.parseInt(secondsSpan);
      parsedDuration = parsedDuration.plusSeconds(seconds);
    }

    if (milliIndex != -1) {
      String millisecondsSpan = valueString.substring(secondIndex + 1, milliIndex);
      int milliseconds = Integer.parseInt(millisecondsSpan);
      parsedDuration = parsedDuration.plusMillis(milliseconds);
    }

    return parsedDuration;
  }

  /**
   * Converts a Duration to the format used by the Dapr runtime.
   *
   * @param value Duration
   * @return The Duration formatted as a String in the format the Dapr runtime uses (e.g. 4h15m50s60ms)
   */
  public static String convertDurationToDaprFormat(Duration value) {
    String stringValue = "";

    // return empty string for anything negative, it'll only happen for reminder "periods", not dueTimes.  A
    // negative "period" means fire once only.
    if (value == Duration.ZERO || (value.compareTo(Duration.ZERO) > 0)) {
      long hours = getDaysPart(value) * 24 + getHoursPart(value);

      StringBuilder sb = new StringBuilder();

      sb.append(hours);
      sb.append("h");

      sb.append(getMinutesPart((value)));
      sb.append("m");

      sb.append(getSecondsPart((value)));
      sb.append("s");

      sb.append(getMilliSecondsPart((value)));
      sb.append("ms");

      return sb.toString();
    }

    return stringValue;
  }

  /**
   * Helper to get the "days" part of the Duration.  For example if the duration is 26 hours, this returns 1.
   *
   * @param d Duration
   * @return Number of days.
   */
  static long getDaysPart(Duration d) {
    long t = d.getSeconds() / 60 / 60 / 24;
    return t;
  }

  /**
   * Helper to get the "hours" part of the Duration.
   * For example if the duration is 26 hours, this is 1 day, 2 hours, so this returns 2.
   *
   * @param d The duration to parse
   * @return the hour part of the duration
   */
  static long getHoursPart(Duration d) {
    long u = (d.getSeconds() / 60 / 60) % 24;

    return u;
  }

  /**
   * Helper to get the "minutes" part of the Duration.
   *
   * @param d The duration to parse
   * @return the minutes part of the duration
   */
  static long getMinutesPart(Duration d) {
    long u = (d.getSeconds() / 60) % 60;

    return u;
  }

  /**
   * Helper to get the "seconds" part of the Duration.
   *
   * @param d The duration to parse
   * @return the seconds part of the duration
   */
  static long getSecondsPart(Duration d) {
    long u = d.getSeconds() % 60;

    return u;
  }

  /**
   * Helper to get the "millis" part of the Duration.
   *
   * @param d The duration to parse
   * @return the milliseconds part of the duration
   */
  static long getMilliSecondsPart(Duration d) {
    long u = d.toMillis() % 1000;

    return u;
  }
}
