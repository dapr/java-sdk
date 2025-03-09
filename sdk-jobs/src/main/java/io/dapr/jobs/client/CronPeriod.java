package io.dapr.jobs.client;

/**
 * Represents the different fields of a cron expression that can be modified
 * using the {@link CronExpressionBuilder}.
 * Each enum value corresponds to a specific component of a cron schedule.
 * Example usage:
 * <pre>
 * CronPeriod period = CronPeriod.MINUTES;
 * System.out.println(period); // Outputs: MINUTES
 * </pre>
 */
public enum CronPeriod {

  /**
   * Represents the seconds field in a cron expression (0-59).
   */
  SECONDS,

  /**
   * Represents the minutes field in a cron expression (0-59).
   */
  MINUTES,

  /**
   * Represents the hours field in a cron expression (0-23).
   */
  HOURS,

  /**
   * Represents the day of the month field in a cron expression (1-31).
   */
  DayOfMonth,

  /**
   * Represents the month of the year field in a cron expression (1-12).
   */
  MonthOfYear,

  /**
   * Represents the day of the week field in a cron expression (0-6, where 0 is Sunday).
   */
  DayOfWeek,
}