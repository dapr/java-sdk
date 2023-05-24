package io.dapr.utils;

import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;

public class DurationUtilsIso8601Test {

  @Test
  public void convertRepeatedDurationToIso8601StringWithRepetitions() {
    // Arrange
    RepeatedDuration repeatedDuration = new RepeatedDuration(Duration.ofHours(4), 2);

    // Act
    String result = DurationUtils.convertRepeatedDurationToIso8601RepetitionFormat(repeatedDuration);

    // Assert
    Assert.assertEquals("R2/PT4H", result);
  }

  @Test
  public void convertRepeatedDurationToIso8601StringWithoutRepetitions() {
    // Arrange
    RepeatedDuration repeatedDuration = new RepeatedDuration(Duration.ofHours(4));

    // Act
    String result = DurationUtils.convertRepeatedDurationToIso8601RepetitionFormat(repeatedDuration);

    // Assert
    Assert.assertEquals("PT4H", result);
  }

  @Test
  public void convertIso8601StringToRepeatedDurationWithRepetitions() {
    // Arrange
    RepeatedDuration expected = new RepeatedDuration(Duration.ofDays(1).plusMinutes(4), 5);
    String value = "R5/P1DT4M";

    // Act
    RepeatedDuration result = DurationUtils.convertIso8601StringToRepeatedDuration(value);

    // Assert
    Assert.assertEquals(expected.getDuration(), result.getDuration());
    Assert.assertEquals(expected.getRepetitions().get(), result.getRepetitions().get());
  }

  @Test
  public void convertIso8601StringToRepeatedDurationWithoutRepetitions() {
    // Arrange
    RepeatedDuration expected = new RepeatedDuration(Duration.ofDays(1).plusMinutes(4));
    String value = "P1DT4M";

    // Act
    RepeatedDuration result = DurationUtils.convertIso8601StringToRepeatedDuration(value);

    // Assert
    Assert.assertEquals(expected.getDuration(), result.getDuration());
    Assert.assertFalse(result.getRepetitions().isPresent());
  }

  @Test
  public void convertDaprFormatStringToRepeatedDuration() {
    // Arrange
    String daprFormatDuration = "4h15m50s60ms";
    Duration expectedDuration = Duration.ofHours(4).plusMinutes(15).plusSeconds(50).plusMillis(60);

    // Act
    RepeatedDuration result = DurationUtils.convertIso8601StringToRepeatedDuration(daprFormatDuration);

    // Assert
    Assert.assertEquals(expectedDuration, result.getDuration());
  }

  @Test(expected = IllegalArgumentException.class)
  public void convertWithInvalidRepetitionSyntax() {
    // Arrange
    String input = "Z4/PT4S";

    // Act
    DurationUtils.convertIso8601StringToRepeatedDuration(input);
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullInputResultsInIllegalArgumentException() {
    DurationUtils.convertIso8601StringToRepeatedDuration("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyInputResultsInIllegalArgumentException() {
    DurationUtils.convertIso8601StringToRepeatedDuration(null);
  }

  @Test
  public void negativeDurationShouldReturnEmptyString() {
    // Arrange
    RepeatedDuration input = new RepeatedDuration(Duration.ZERO.minusMinutes(1));

    // Act
    String result = DurationUtils.convertRepeatedDurationToIso8601RepetitionFormat(input);

    // Assert
    Assert.assertEquals("", result);
  }
}
