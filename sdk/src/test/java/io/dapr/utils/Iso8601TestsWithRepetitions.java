package io.dapr.utils;

import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;

public class Iso8601TestsWithRepetitions {

  @Test
  public void toStringWithRepetitions() {
    // Arrange
    RepeatedDuration repeatedDuration = new RepeatedDuration(Duration.ofHours(4), 2);

    // Act
    String result = DurationUtils.convertRepeatedDurationToIso8601RepetitionFormat(repeatedDuration);

    // Assert
    Assert.assertEquals("R2/PT4H", result);
  }

  @Test
  public void toStringNoRepetitions() {
    // Arrange
    RepeatedDuration repeatedDuration = new RepeatedDuration(Duration.ofHours(4));

    // Act
    String result = DurationUtils.convertRepeatedDurationToIso8601RepetitionFormat(repeatedDuration);

    // Assert
    Assert.assertEquals("PT4H", result);
  }
}
