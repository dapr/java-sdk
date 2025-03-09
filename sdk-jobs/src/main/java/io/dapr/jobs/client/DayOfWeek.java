package io.dapr.jobs.client;

/**
 * Represents the days of the week in a cron expression.
 * <p>
 * This enum maps each day of the week to its corresponding integer value
 * as used in cron expressions (0 for Sunday through 6 for Saturday).
 * <p>
 * Implements {@link OrdinalEnum} to provide an ordinal ranking for each day.
 * <p>
 * Example usage:
 * <pre>
 * DayOfWeek day = DayOfWeek.MON;
 * System.out.println(day.getRank()); // Outputs: 1
 * </pre>
 */
public enum DayOfWeek implements OrdinalEnum {


  SUN(0),
  MON(1),
  TUE(2),
  WED(3),
  THU(4),
  FRI(5),
  SAT(6);

  private final int value;

  /**
   * Constructs a {@code DayOfWeek} enum with the given value.
   *
   * @param value the integer representation of the day (0-6).
   */
  DayOfWeek(int value) {
    this.value = value;
  }

  @Override
  public int getRank() {
    return this.value;
  }
}