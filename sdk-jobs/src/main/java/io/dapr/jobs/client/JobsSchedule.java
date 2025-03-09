package io.dapr.jobs.client;

import java.time.Duration;

/**
 * Represents a job schedule using cron expressions or fixed intervals.
 * <p>
 * This class provides various static methods to create schedules based on predefined periods
 * (e.g., daily, weekly, monthly) or using custom cron expressions.
 * <p>
 * Example usage:
 * <pre>
 * JobsSchedule schedule = JobsSchedule.daily();
 * System.out.println(schedule.getExpression()); // Outputs: "0 0 0 * * *"
 * </pre>
 */
public class JobsSchedule {

  private final String expression;

  /**
   * Private constructor to create a job schedule from a cron expression.
   *
   * @param expression the cron expression defining the schedule.
   */
  private JobsSchedule(String expression) {
    this.expression = expression;
  }

  /**
   * Creates a job schedule from a fixed period using a {@link Duration}.
   * <p>
   * The resulting expression follows the format: "@every XhYmZsWms"
   * where X, Y, Z, and W represent hours, minutes, seconds, and milliseconds respectively.
   * <p>
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
  public static JobsSchedule fromPeriod(Duration duration) {
    if (duration == null) {
      throw new IllegalArgumentException("duration cannot be null");
    }

    String formattedDuration = String.format("%dh%dm%ds%dms",
        duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart(), duration.toMillisPart());
    return new JobsSchedule("@every " + formattedDuration);
  }

  /**
   * Creates a job schedule from a custom cron expression.
   *
   * @param cronExpression the cron expression.
   * @return a {@code JobsSchedule} representing the given cron expression.
   */
  public static JobsSchedule fromString(String cronExpression) {
    return new JobsSchedule(cronExpression);
  }

  /**
   * Creates a yearly job schedule, running at midnight on January 1st.
   *
   * @return a {@code JobsSchedule} for yearly execution.
   */
  public static JobsSchedule yearly() {
    return new JobsSchedule(new CronExpressionBuilder()
        .add(CronPeriod.SECONDS, 0)
        .add(CronPeriod.MINUTES, 0)
        .add(CronPeriod.HOURS, 0)
        .add(CronPeriod.DayOfMonth, 1)
        .add(CronPeriod.MonthOfYear, 1)
        .build());
  }

  /**
   * Creates a monthly job schedule, running at midnight on the first day of each month.
   *
   * @return a {@code JobsSchedule} for monthly execution.
   */
  public static JobsSchedule monthly() {
    return new JobsSchedule(new CronExpressionBuilder()
        .add(CronPeriod.SECONDS, 0)
        .add(CronPeriod.MINUTES, 0)
        .add(CronPeriod.HOURS, 0)
        .add(CronPeriod.DayOfMonth, 1)
        .build());
  }

  /**
   * Creates a weekly job schedule, running at midnight on Sunday.
   *
   * @return a {@code JobsSchedule} for weekly execution.
   */
  public static JobsSchedule weekly() {
    return new JobsSchedule(new CronExpressionBuilder()
        .add(CronPeriod.SECONDS, 0)
        .add(CronPeriod.MINUTES, 0)
        .add(CronPeriod.HOURS, 0)
        .add(CronPeriod.DayOfWeek, 0)
        .build());
  }

  /**
   * Creates a daily job schedule, running at midnight every day.
   *
   * @return a {@code JobsSchedule} for daily execution.
   */
  public static JobsSchedule daily() {
    return new JobsSchedule(new CronExpressionBuilder()
        .add(CronPeriod.SECONDS, 0)
        .add(CronPeriod.MINUTES, 0)
        .add(CronPeriod.HOURS, 0)
        .build());
  }

  /**
   * Creates an hourly job schedule, running at the start of every hour.
   *
   * @return a {@code JobsSchedule} for hourly execution.
   */
  public static JobsSchedule hourly() {
    return new JobsSchedule(new CronExpressionBuilder()
        .add(CronPeriod.SECONDS, 0)
        .add(CronPeriod.MINUTES, 0)
        .build());
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
