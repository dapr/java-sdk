package io.dapr.utils;

import org.junit.Test;

import java.time.Duration;


public class RepeatedDurationTest {

  @Test(expected = IllegalArgumentException.class)
  public void invalidAmountOfRepetitions() {
    new RepeatedDuration(Duration.ZERO, -1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void repetitionsMustBeGreaterThanZero() {
    new RepeatedDuration(Duration.ZERO, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void durationCanNotBeNull() {
    new RepeatedDuration(null);
  }
}
