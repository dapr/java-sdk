// ------------------------------------------------------------
// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
// ------------------------------------------------------------

package io.dapr.actors.runtime;

import java.time.Duration;

class ReminderInfo {

  private static final Duration MIN_TIME_PERIOD = Duration.ofMillis(-1);

  private Duration dueTime;

  private Duration period;

  private byte[] data;

  public Duration getDueTime() {
    return dueTime;
  }

  public void setDueTime(Duration dueTime) {
    this.dueTime = dueTime;
  }

  public Duration getPeriod() {
    return period;
  }

  public void setPeriod(Duration period) {
    this.period = period;
  }

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  public ReminderInfo(byte[] state, Duration dueTime, Duration period) {
    ValidateDueTime("DueTime", dueTime);
    ValidatePeriod("Period", period);
    this.data = state;
    this.dueTime = dueTime;
    this.period = period;
  }

  private static void ValidateDueTime(String argName, Duration value) {
    if (value.compareTo(Duration.ZERO) < 0) {
      String message = String.format("argName: %s - Duration toMillis() - specified value must be greater than %s", argName, Duration.ZERO);
      throw new IllegalArgumentException(message);
    }
  }

  private static void ValidatePeriod(String argName, Duration value) throws IllegalArgumentException {
    if (value.compareTo(MIN_TIME_PERIOD) < 0) {
      String message = String.format("argName: %s - Duration toMillis() - specified value must be greater than %s", argName, Duration.ZERO);
      throw new IllegalArgumentException(message);
    }
  }
}
