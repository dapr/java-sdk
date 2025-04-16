/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.client.domain;

import java.time.Duration;

/**
 * Represents a job schedule using cron expressions or fixed intervals.
 * This class provides various static methods to create schedules based on predefined periods
 * (e.g., daily, weekly, monthly) or using custom cron expressions.
 * Example usage:
 * <pre>
 * JobsSchedule schedule = JobsSchedule.daily();
 * System.out.println(schedule.getExpression()); // Outputs: "0 0 0 * * *"
 * </pre>
 */
public class JobSchedule {

  private final String expression;

  /**
   * Private constructor to create a job schedule from a cron expression.
   *
   * @param expression the cron expression defining the schedule.
   */
  private JobSchedule(String expression) {
    this.expression = expression;
  }

  /**
   * Creates a job schedule from a fixed period using a {@link Duration}.
   * The resulting expression follows the format: "@every XhYmZsWms"
   * where X, Y, Z, and W represent hours, minutes, seconds, and milliseconds respectively.
   * Example:
   * <pre>
   * JobsSchedule schedule = JobsSchedule.fromPeriod(Duration.ofMinutes(30));
   * System.out.println(schedule.getExpression()); // Outputs: "@every 0h30m0s0ms"
   * </pre>
   *
   * @param duration the duration of the period.
   * @return a {@code JobsSchedule} with the corresponding interval.
   * @throws IllegalArgumentException if the duration is null.
   */
  public static JobSchedule fromPeriod(Duration duration) {
    if (duration == null) {
      throw new IllegalArgumentException("duration cannot be null");
    }

    String formattedDuration = String.format("%dh%dm%ds%dms",
        duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart(), duration.toMillisPart());
    return new JobSchedule("@every " + formattedDuration);
  }

  /**
   * Creates a job schedule from a custom cron expression.
   *
   * @param cronExpression the cron expression.
   * @return a {@code JobsSchedule} representing the given cron expression.
   */
  public static JobSchedule fromString(String cronExpression) {
    if (cronExpression == null) {
      throw new IllegalArgumentException("cronExpression cannot be null");
    }

    return new JobSchedule(cronExpression);
  }

  /**
   * Creates a yearly job schedule, running at midnight on January 1st.
   *
   * @return a {@code JobsSchedule} for yearly execution.
   */
  public static JobSchedule yearly() {
    return new JobSchedule("0 0 0 1 1 *");
  }

  /**
   * Creates a monthly job schedule, running at midnight on the first day of each month.
   *
   * @return a {@code JobsSchedule} for monthly execution.
   */
  public static JobSchedule monthly() {
    return new JobSchedule("0 0 0 1 * *");
  }

  /**
   * Creates a weekly job schedule, running at midnight on Sunday.
   *
   * @return a {@code JobsSchedule} for weekly execution.
   */
  public static JobSchedule weekly() {
    return new JobSchedule("0 0 0 * * 0");
  }

  /**
   * Creates a daily job schedule, running at midnight every day.
   *
   * @return a {@code JobsSchedule} for daily execution.
   */
  public static JobSchedule daily() {
    return new JobSchedule("0 0 0 * * *");
  }

  /**
   * Creates an hourly job schedule, running at the start of every hour.
   *
   * @return a {@code JobsSchedule} for hourly execution.
   */
  public static JobSchedule hourly() {
    return new JobSchedule("0 0 * * * *");
  }

  /**
   * Gets the cron expression representing this job schedule.
   *
   * @return the cron expression as a string.
   */
  public String getExpression() {
    return this.expression;
  }
}
