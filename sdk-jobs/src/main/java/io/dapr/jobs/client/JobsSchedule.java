package io.dapr.jobs.client;

import java.time.Duration;

public class JobsSchedule {

  private final String expression;

  private JobsSchedule(String expression) {
    this.expression = expression;
  }

  public static JobsSchedule fromPeriod(Duration duration) {
    if (duration == null) {
      throw new IllegalArgumentException("duration cannot be null");
    }

    String formattedDuration = String.format("%dh%dm%ds%dms",
        duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart(), duration.toMillisPart());
    return new JobsSchedule("@every " + formattedDuration);
  }

  public static JobsSchedule fromString(String cronExpression) {
    return new JobsSchedule(cronExpression);
  }

  public static JobsSchedule yearly() {
    return new JobsSchedule(new CronExpressionBuilder()
        .add(CronPeriod.SECONDS, 0)
        .add(CronPeriod.MINUTES, 0)
        .add(CronPeriod.HOURS, 0)
        .add(CronPeriod.DayOfMonth, 1)
        .add(CronPeriod.MonthOfYear, 1)
        .build());
  }

  public static JobsSchedule monthly() {
    return new JobsSchedule(new CronExpressionBuilder()
        .add(CronPeriod.SECONDS, 0)
        .add(CronPeriod.MINUTES, 0)
        .add(CronPeriod.HOURS, 0)
        .add(CronPeriod.DayOfMonth, 1)
        .build());
  }

  public static JobsSchedule weekly() {
    return new JobsSchedule(new CronExpressionBuilder()
        .add(CronPeriod.SECONDS, 0)
        .add(CronPeriod.MINUTES, 0)
        .add(CronPeriod.HOURS, 0)
        .add(CronPeriod.DayOfWeek, 0)
        .build());
  }

  public static JobsSchedule daily() {
    return new JobsSchedule(new CronExpressionBuilder()
        .add(CronPeriod.SECONDS, 0)
        .add(CronPeriod.MINUTES, 0)
        .add(CronPeriod.HOURS, 0)
        .build());
  }

  public static JobsSchedule hourly() {
    return new JobsSchedule(new CronExpressionBuilder()
        .add(CronPeriod.SECONDS, 0)
        .add(CronPeriod.MINUTES, 0)
        .build());
  }

  public String getExpression() {
    return this.expression;
  }
}
