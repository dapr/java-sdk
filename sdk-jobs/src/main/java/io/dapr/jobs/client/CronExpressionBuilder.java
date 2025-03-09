package io.dapr.jobs.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A builder class for constructing cron expressions. This class provides an easy way to construct cron expressions
 * by adding individual values or ranges for each of the cron fields: seconds, minutes, hours, day of month,
 * day of week, and month of year. It supports adding steps and ranges for fields where appropriate.
 * Example usage:
 * <pre>
 * CronExpressionBuilder builder = new CronExpressionBuilder();
 * builder.add(CronPeriod.MINUTES, 0, 15, 30); // Every 15 minutes starting at 0
 * builder.add(CronPeriod.HOURS, 12); // At noon
 * builder.addRange(CronPeriod.DAYOFMONTH, 1, 31); // On the 1st through the 31st day of the month
 * builder.addStep(CronPeriod.MINUTES, 5); // Every 5 minutes
 * System.out.println(builder.build()); // Outputs the cron expression
 * </pre>
 */
public class CronExpressionBuilder {

  private final List<String> seconds;
  private final List<String> minutes;
  private final List<String> hours;
  private final List<String> dayOfMonth;
  private final List<String> dayOfWeek;
  private final List<String> monthOfYear;

  /**
   * Constructs a new {@link CronExpressionBuilder} instance with empty cron fields.
   */
  public CronExpressionBuilder() {
    this.seconds = new ArrayList<>();
    this.minutes = new ArrayList<>();
    this.hours = new ArrayList<>();
    this.dayOfMonth = new ArrayList<>();
    this.dayOfWeek = new ArrayList<>();
    this.monthOfYear = new ArrayList<>();
  }

  /**
   * Adds values to the specified cron period (e.g., minutes, hours, etc.).
   * example:
   * builder.add(CronPeriod.MINUTES, 0, 15, 30);  // Adds 0, 15, and 30 minutes to the cron expression
   *
   * @param cronPeriod The cron period to modify (e.g., {CronPeriod.MINUTES}).
   * @param values The values to be added to the cron period.
   * @return The {@link CronExpressionBuilder} instance for method chaining.
   * @throws IllegalArgumentException if values are invalid or empty.
   *
   */
  public CronExpressionBuilder add(CronPeriod cronPeriod, int... values) {
    return addInternal(cronPeriod, Arrays.stream(values).boxed().toArray());
  }

  /**
   * Adds values for the {@link MonthOfYear} cron period.
   *
   * @param values The {@link MonthOfYear} values to be added.
   * @return The {@link CronExpressionBuilder} instance for method chaining.
   */
  public CronExpressionBuilder add(MonthOfYear... values) {
    return addInternal(CronPeriod.MonthOfYear, values);
  }

  /**
   * Adds values for the {@link DayOfWeek} cron period.
   *
   * @param values The {@link DayOfWeek} values to be added.
   * @return The {@link CronExpressionBuilder} instance for method chaining.
   */
  public CronExpressionBuilder add(DayOfWeek... values) {
    return addInternal(CronPeriod.DayOfWeek, values);
  }

  /**
   * Adds a range of values to a cron period.
   *
   * @param period The cron period to modify (e.g., {CronPeriod.MONTHOFYEAR}).
   * @param from The starting value of the range (inclusive).
   * @param to The ending value of the range (inclusive).
   * @return The {@link CronExpressionBuilder} instance for method chaining.
   * @throws IllegalArgumentException if the range is invalid.
   */
  public CronExpressionBuilder addRange(CronPeriod period, int from, int to) {
    return addRangeInternal(period, from, to);
  }

  /**
   * Adds a range of {@link DayOfWeek} values to the cron expression.
   *
   * @param from The starting {@link DayOfWeek} value.
   * @param to The ending {@link DayOfWeek} value.
   * @return The {@link CronExpressionBuilder} instance for method chaining.
   */
  public CronExpressionBuilder addRange(DayOfWeek from, DayOfWeek to) {
    return addRangeInternal(CronPeriod.DayOfWeek, from, to);
  }

  /**
   * Adds a range of {@link MonthOfYear} values to the cron expression.
   *
   * @param from The starting {@link MonthOfYear} value.
   * @param to The ending {@link MonthOfYear} value.
   * @return The {@link CronExpressionBuilder} instance for method chaining.
   */
  public CronExpressionBuilder addRange(MonthOfYear from, MonthOfYear to) {
    return addRangeInternal(CronPeriod.MonthOfYear, from, to);
  }

  /**
   * Adds a step range for a cron period.
   *
   * @param period The cron period to modify.
   * @param from The starting value for the step range (inclusive).
   * @param to The ending value for the step range (inclusive).
   * @param interval The interval for the step range.
   * @return The {@link CronExpressionBuilder} instance for method chaining.
   */
  public CronExpressionBuilder addStepRange(CronPeriod period, int from, int to, int interval) {
    return addStepInternal(period, from, to, interval);
  }

  /**
   * Adds a step for a cron period.
   *
   * @param period The cron period to modify.
   * @param interval The interval for the step.
   * @return The {@link CronExpressionBuilder} instance for method chaining.
   */
  public CronExpressionBuilder addStep(CronPeriod period, int interval) {
    return addStepInternal(period, null, null, interval);
  }

  /**
   * Adds a specific value with a step interval for the cron period.
   *
   * @param period The cron period to modify.
   * @param value The starting value for the step.
   * @param interval The interval for the step.
   * @return The {@link CronExpressionBuilder} instance for method chaining.
   */
  public CronExpressionBuilder addStep(CronPeriod period, int value, int interval) {
    throwIfNull(period);
    validatePeriod(period, value);
    validatePeriod(period, interval);

    addToPeriod(period, value + "/" + interval);
    return this;
  }

  /**
   * Builds the cron expression by combining all the specified cron periods and their values.
   *
   * @return A string representation of the cron expression.
   */
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

    return String.join(",", this.seconds) + " "
        + String.join(",", this.minutes) + " "
        + String.join(",", this.hours) + " "
        + String.join(",", this.dayOfMonth) + " "
        + String.join(",", this.monthOfYear) + " "
        + String.join(",", this.dayOfWeek);
  }

  /**
   * Internal method to add values to a cron period.
   *
   * @param period The cron period to modify.
   * @param values The values to add.
   * @return The {@link CronExpressionBuilder} instance for method chaining.
   */
  private <T> CronExpressionBuilder addInternal(CronPeriod period, T[] values) {
    throwIfNull(period);
    throwIfNull(values);

    if (values.length == 0) {
      throw new IllegalArgumentException(period + " values cannot be empty");
    }

    List<String> valueStrings = new ArrayList<>(values.length);
    for (T value : values) {
      throwIfNull(value);
      if (value instanceof OrdinalEnum) {
        validatePeriod(period, ((OrdinalEnum) value).getRank());
        valueStrings.add(value.toString());
      } else {
        validatePeriod(period, (int)value);
        valueStrings.add(String.valueOf(value));
      }
    }

    addToPeriod(period, String.join(",", valueStrings));

    return this;
  }

  /**
   * Internal method to add a range of values to a cron period.
   *
   * @param period The cron period to modify.
   * @param from The starting value for the range (inclusive).
   * @param to The ending value for the range (inclusive).
   * @return The {@link CronExpressionBuilder} instance for method chaining.
   */
  private <T> CronExpressionBuilder addRangeInternal(CronPeriod period, T from, T to) {
    throwIfNull(period);
    throwIfNull(from);
    throwIfNull(to);

    if (from instanceof OrdinalEnum && to instanceof OrdinalEnum) {
      int fromInterval = ((OrdinalEnum) from).getRank();
      int toInterval = ((OrdinalEnum) to).getRank();
      validateRange(fromInterval, toInterval);
      validatePeriod(period, fromInterval);
      validatePeriod(period, toInterval);
    } else {
      validateRange((int)from, (int)to);
      validatePeriod(period, (int)from);
      validatePeriod(period, (int)to);
    }

    addToPeriod(period, from + "-" + to);
    return this;
  }

  /**
   * Internal method to add a step range to a cron period.
   *
   * @param period The cron period to modify.
   * @param from The starting value for the step range (inclusive).
   * @param to The ending value for the step range (inclusive).
   * @param interval The interval for the step.
   * @return The {@link CronExpressionBuilder} instance for method chaining.
   */
  private CronExpressionBuilder addStepInternal(CronPeriod period, Integer from, Integer to, Integer interval) {
    throwIfNull(period);
    throwIfNull(interval);

    if (from != null || to != null) {
      throwIfNull(from);
      throwIfNull(to);
      validatePeriod(period, from);
      validatePeriod(period, to);
      validateRange(from, to);

      addToPeriod(period, from + "-" + to + "/" + interval);
      return this;
    }

    addToPeriod(period, "*/" + interval);
    return this;
  }

  /**
   * Validates the value for a specific cron period.
   *
   * @param cronPeriod The cron period to validate (e.g., {@link CronPeriod.HOURS}).
   * @param value The value to validate.
   * @throws IllegalArgumentException if the value is invalid for the cron period.
   */
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
      throw new IllegalArgumentException("from must be less than to (from < to)");
    }
  }

  /**
   * Helper method to add values to the period.
   *
   * @param period The cron period to modify.
   * @param value The value to add to the period.
   */
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
      throw new IllegalArgumentException("None of the input parameters can be null");
    }
  }
}