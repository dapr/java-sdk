package io.dapr.jobs.client;

import java.util.ArrayList;
import java.util.List;

public class CronExpressionBuilder {

  private final List<String> seconds;
  private final List<String> minutes;
  private final List<String> hours;
  private final List<String> dayOfMonth;
  private final List<String> dayOfWeek;
  private final List<String> monthOfYear;

  public CronExpressionBuilder() {
    this.seconds = new ArrayList<>();
    this.minutes = new ArrayList<>();
    this.hours = new ArrayList<>();
    this.dayOfMonth = new ArrayList<>();
    this.dayOfWeek = new ArrayList<>();
    this.monthOfYear = new ArrayList<>();
  }

  /**
   * Convert to cron expression depending on period and values.
   *
   * @param cronPeriod {@link CronPeriod}.
   * @param values for the cronPeriod.
   * @return this.
   */
  public CronExpressionBuilder add(CronPeriod cronPeriod, Integer... values) {
    throwIfNull(cronPeriod);
    throwIfNull(values);

    StringBuilder builder = new StringBuilder();
    for (Integer value: values) {
      throwIfNull(value);
      validatePeriod(cronPeriod, value);
      builder.append(value).append(",");
    }

    addToPeriod(cronPeriod, builder.deleteCharAt(builder.length() - 1).toString());

    return this;
  }

  public CronExpressionBuilder add(MonthOfYear... values) {
    throwIfNull(values);

    StringBuilder builder = new StringBuilder();
    for (MonthOfYear value: values) {
      throwIfNull(value);
      builder.append(value).append(",");
    }

    addToPeriod(CronPeriod.MonthOfYear, builder.deleteCharAt(builder.length() - 1).toString());

    return this;
  }

  public CronExpressionBuilder add(DayOfWeek... values) {
    throwIfNull(values);

    StringBuilder builder = new StringBuilder();
    for (DayOfWeek value: values) {
      throwIfNull(value);
      builder.append(value).append(",");
    }

    addToPeriod(CronPeriod.DayOfWeek, builder.deleteCharAt(builder.length() - 1).toString());

    return this;
  }
  
  public CronExpressionBuilder addRange(CronPeriod period, int from, int to) {
    throwIfNull(period);
    validateRange(from, to);
    validatePeriod(period, from);
    validatePeriod(period, to);

    addToPeriod(period, from + "-" + to);

    return this;
  }

  public CronExpressionBuilder addRange(DayOfWeek from, DayOfWeek to) {
    throwIfNull(from);
    throwIfNull(to);
    validateRange(from.getValue(), to.getValue());

    addToPeriod(CronPeriod.DayOfWeek, from + "-" + to);
    return this;
  }

  public CronExpressionBuilder addRange(MonthOfYear from, MonthOfYear to) {
    throwIfNull(from);
    throwIfNull(to);

    addToPeriod(CronPeriod.MonthOfYear, from + "-" + to);
    return this;
  }

  public CronExpressionBuilder addStepRange(CronPeriod period, int from, int to, int denominator) {
    throwIfNull(period);
    validateRange(from, to);
    validatePeriod(period, denominator);

    addToPeriod(period, from + "-" + to + "/" + denominator);
    return this;
  }

  public CronExpressionBuilder addStep(CronPeriod period, int numerator, int denominator) {
    throwIfNull(period);
    validatePeriod(period, numerator);
    validatePeriod(period, denominator);

    addToPeriod(period, numerator + "/" + denominator);
    return this;
  }

  public CronExpressionBuilder addStep(CronPeriod period, int denominator) {
    throwIfNull(period);
    validatePeriod(period, denominator);

    addToPeriod(period, "*/" + denominator);
    return this;
  }

  public String build() {

    if (this.monthOfYear.isEmpty()) {
      this.monthOfYear.add("*");
    }

    if (this.dayOfWeek.isEmpty()) {
      this.dayOfWeek.add("*");
    }

    if (this.seconds.isEmpty()) {
      this.seconds.add("*");
    }

    if (this.minutes.isEmpty()) {
      this.minutes.add("*");
    }

    if (this.hours.isEmpty()) {
      this.hours.add("*");
    }

    if (this.dayOfMonth.isEmpty()) {
      this.dayOfMonth.add("*");
    }

    StringBuilder cronExpression = new StringBuilder();
    cronExpression.append(String.join(",", this.seconds)).append(" ");
    cronExpression.append(String.join(",", this.minutes)).append(" ");
    cronExpression.append(String.join(",", this.hours)).append(" ");
    cronExpression.append(String.join(",", this.dayOfMonth)).append(" ");
    cronExpression.append(String.join(",", this.monthOfYear)).append(" ");
    cronExpression.append(String.join(",", this.dayOfWeek));

    return cronExpression.toString();
  }

  private void validatePeriod(CronPeriod cronPeriod, int value) {
    switch (cronPeriod) {
      case SECONDS:
      case MINUTES:
        if (value < 0 || value > 59) {
          throw new IllegalArgumentException(cronPeriod + " must be between [0, 59] inclusive");
        }
        break;
      case HOURS:
        if (value < 0 || value > 23) {
          throw new IllegalArgumentException(cronPeriod + " must be between [0, 23] inclusive");
        }
        break;
      case DayOfMonth:
        if (value < 1 || value > 31) {
          throw new IllegalArgumentException(cronPeriod + " must be between [1, 31] inclusive");
        }
        break;
      case MonthOfYear:
        if (value < 1 || value > 12) {
          throw new IllegalArgumentException(cronPeriod + " must be between [1, 12] inclusive");
        }
        break;
      case DayOfWeek:
        if (value < 0 || value > 6) {
          throw new IllegalArgumentException(cronPeriod + " must be between [0, 6] inclusive");
        }
        break;
      default: throw new IllegalArgumentException("Invalid CronPeriod: " + cronPeriod);
    }
  }

  private void validateRange(int from, int to) {
    if (from > to || from == to) {
      throw new IllegalArgumentException("from must be before to (from < to)");
    }
  }

  private void addToPeriod(CronPeriod cronPeriod, String value) {
    switch (cronPeriod) {
      case SECONDS:
        this.seconds.add(value);
        break;
      case MINUTES:
        this.minutes.add(value);
        break;
      case HOURS:
        this.hours.add(value);
        break;
      case DayOfMonth:
        this.dayOfMonth.add(value);
        break;
      case MonthOfYear:
        this.monthOfYear.add(value);
        break;
      case DayOfWeek:
        this.dayOfWeek.add(value);
        break;
      default:
        throw new IllegalArgumentException("Invalid CronPeriod: " + cronPeriod);
    }
  }

  private void throwIfNull(Object obj) {
    if (obj == null) {
      throw new IllegalArgumentException("None of the parameters can be null");
    }
  }
}