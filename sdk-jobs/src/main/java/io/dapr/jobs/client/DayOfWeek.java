package io.dapr.jobs.client;

public enum DayOfWeek {

  SUN(0),
  MON(1),
  TUE(2),
  WED(3),
  THU(4),
  FRI(5),
  SAT(6);

  private final int value;

  private DayOfWeek(int value) {
    this.value = value;
  }

  int getValue() {
    return this.value;
  }
}