package io.dapr.jobs.client;

/**
 * Represents the months of the year, each associated with its ordinal position (1-12).
 * This enum implements {@link OrdinalEnum}, allowing retrieval of the numerical rank of each month.
 * Example usage:
 * <pre>
 * MonthOfYear month = MonthOfYear.JAN;
 * int rank = month.getRank(); // Returns 1
 * </pre>
 */

public enum MonthOfYear implements OrdinalEnum {

  JAN(1),
  FEB(2),
  MAR(3),
  APR(4),
  MAY(5),
  JUN(6),
  JUL(7),
  AUG(8),
  SEP(9),
  OCT(10),
  NOV(11),
  DEC(12);

  private final int value;

  MonthOfYear(int value) {
    this.value = value;
  }

  @Override
  public int getRank() {
    return this.value;
  }
}