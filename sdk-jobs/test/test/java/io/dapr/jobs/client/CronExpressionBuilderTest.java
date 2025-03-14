package io.dapr.jobs.client;

import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.Assert.assertEquals;

public class CronExpressionBuilderTest {

  @Test
  public void builderWithoutParametersShouldReturnDefaultValues() {
    String cronExpression = new CronExpressionBuilder().build();
    assertEquals("* * * * * *", cronExpression);
  }

  @Test
  public void builderWithInvalidSecondsShouldThrowIllegalArgumentException() {
    IllegalArgumentException exception = Assert.assertThrows(IllegalArgumentException.class,
        () -> new CronExpressionBuilder()
        .add(CronPeriod.SECONDS, 60).build());
    Assert.assertTrue(exception.getMessage().contains("SECONDS must be between [0, 59]"));
    exception = Assert.assertThrows(IllegalArgumentException.class,
        () -> new CronExpressionBuilder()
            .add(CronPeriod.SECONDS, -1).build());
    Assert.assertTrue(exception.getMessage().contains("SECONDS must be between [0, 59]"));
  }

  @Test
  public void builderWithInvalidMinutesShouldThrowIllegalArgumentException() {
    IllegalArgumentException exception = Assert.assertThrows(IllegalArgumentException.class,
        () -> new CronExpressionBuilder()
            .add(CronPeriod.MINUTES, 60).build());
    Assert.assertTrue(exception.getMessage().contains("MINUTES must be between [0, 59]"));
    exception = Assert.assertThrows(IllegalArgumentException.class,
        () -> new CronExpressionBuilder()
            .add(CronPeriod.MINUTES, -1).build());
    Assert.assertTrue(exception.getMessage().contains("MINUTES must be between [0, 59]"));
  }

  @Test
  public void builderWithEmptyParametersShouldThrowIllegalArgumentException() {
    IllegalArgumentException exception = Assert.assertThrows(IllegalArgumentException.class,
        () -> new CronExpressionBuilder()
            .add(CronPeriod.MINUTES).build());
    Assert.assertTrue(exception.getMessage().contains("MINUTES values cannot be empty"));
  }

  @Test
  public void builderWithInvalidHoursShouldThrowIllegalArgumentException() {
    IllegalArgumentException exception = Assert.assertThrows(IllegalArgumentException.class,
        () -> new CronExpressionBuilder()
            .add(CronPeriod.HOURS, -1).build());
    Assert.assertTrue(exception.getMessage().contains("HOURS must be between [0, 23]"));
    exception = Assert.assertThrows(IllegalArgumentException.class,
        () -> new CronExpressionBuilder()
            .add(CronPeriod.HOURS, 24).build());
    Assert.assertTrue(exception.getMessage().contains("HOURS must be between [0, 23]"));
  }

  @Test
  public void builderWithInvalidDayOfMonthShouldThrowIllegalArgumentException1() {
    IllegalArgumentException exception = Assert.assertThrows(IllegalArgumentException.class,
        () -> new CronExpressionBuilder()
            .add(CronPeriod.DayOfMonth, 0).build());
    Assert.assertTrue(exception.getMessage().contains("DayOfMonth must be between [1, 31]"));
  }

  @Test
  public void builderWithInvalidDayOfMonthShouldThrowIllegalArgumentException() {
    IllegalArgumentException exception = Assert.assertThrows(IllegalArgumentException.class,
        () -> new CronExpressionBuilder()
            .add(CronPeriod.DayOfMonth, 32).build());
    Assert.assertTrue(exception.getMessage().contains("DayOfMonth must be between [1, 31]"));
  }

  @Test
  public void builderWithInvalidMonthOfYearShouldThrowIllegalArgumentException() {
    IllegalArgumentException exception = Assert.assertThrows(IllegalArgumentException.class,
        () -> new CronExpressionBuilder()
            .add(CronPeriod.MonthOfYear, 0).build());
    Assert.assertTrue(exception.getMessage().contains("MonthOfYear must be between [1, 12]"));
    exception = Assert.assertThrows(IllegalArgumentException.class,
        () -> new CronExpressionBuilder()
            .add(CronPeriod.MonthOfYear, 13).build());
    Assert.assertTrue(exception.getMessage().contains("MonthOfYear must be between [1, 12]"));
  }

  @Test
  public void builderWithInvalidDayOfWeekShouldThrowIllegalArgumentException() {
    IllegalArgumentException exception = Assert.assertThrows(IllegalArgumentException.class,
        () -> new CronExpressionBuilder()
            .add(CronPeriod.DayOfWeek, -1).build());
    Assert.assertTrue(exception.getMessage().contains("DayOfWeek must be between [0, 6]"));
    exception = Assert.assertThrows(IllegalArgumentException.class,
        () -> new CronExpressionBuilder()
            .add(CronPeriod.DayOfWeek, 7).build());
    Assert.assertTrue(exception.getMessage().contains("DayOfWeek must be between [0, 6]"));
  }

  @Test
  public void builderWithInvalidRangeShouldThrowIllegalArgumentException() {
    IllegalArgumentException exception = Assert.assertThrows(IllegalArgumentException.class,
        () -> new CronExpressionBuilder()
            .addRange(CronPeriod.HOURS, 20, 19).build());
    Assert.assertTrue(exception.getMessage().contains("from must be less than to"));
  }

  @Test
  public void builderWithInvalidMinuteRangeSpecifiedShouldThrowIllegalArgumentException() {
    IllegalArgumentException exception = Assert.assertThrows(IllegalArgumentException.class,
        () -> new CronExpressionBuilder()
            .addRange(CronPeriod.MINUTES, 1, 1).build());
    Assert.assertTrue(exception.getMessage().contains("from must be less than to (from < to)"));
  }

  @Test
  public void builderWithInvalidParametersShouldThrowIllegalArgumentException() {
    IllegalArgumentException exception = Assert.assertThrows(IllegalArgumentException.class,
        () -> new CronExpressionBuilder()
            .addRange(null, 1, 2).build());
    Assert.assertTrue(exception.getMessage().contains("None of the input parameters can be null"));
  }

  @ParameterizedTest
  @CsvSource({
      "SECONDS, 0, '0 * * * * *'",
      "MINUTES, 30, '* 30 * * * *'",
      "HOURS, 12, '* * 12 * * *'",
      "DayOfMonth, 15, '* * * 15 * *'",
      "MonthOfYear, 6, '* * * * 6 *'",
      "DayOfWeek, 3, '* * * * * 3'"
  })
  void testAddSingleValue(CronPeriod period, int value, String expected) {
    CronExpressionBuilder builder = new CronExpressionBuilder().add(period, value);
    assertEquals(expected, builder.build());
  }

  @Test
  public void builderWithInvalidParameterInValuesShouldThrowIllegalArgumentException() {
    IllegalArgumentException exception = Assert.assertThrows(IllegalArgumentException.class,
        () -> new CronExpressionBuilder()
            .add(null, MonthOfYear.JAN, null, MonthOfYear.FEB).build());
    Assert.assertTrue(exception.getMessage().contains("None of the input parameters can be null"));
  }

  @Test
  public void builderWithInvalidParameterInDayOfWeekValuesShouldThrowIllegalArgumentException() {
    IllegalArgumentException exception = Assert.assertThrows(IllegalArgumentException.class,
        () -> new CronExpressionBuilder()
            .add(null, DayOfWeek.MON, null, DayOfWeek.THU).build());
    Assert.assertTrue(exception.getMessage().contains("None of the input parameters can be null"));
  }

  @Test
  public void builderWithSecondsShouldReturnWithOnlySecondsSet() {
    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.SECONDS, 5)
        .build();
    assertEquals("5 * * * * *", cronExpression);
  }

  @Test
  public void builderWithMultipleCallsToAddSecondsShouldReturnWithMultipleValues() {
    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.SECONDS, 5)
        .add(CronPeriod.SECONDS, 10)
        .add(CronPeriod.SECONDS, 20)
        .build();
    assertEquals("5,10,20 * * * * *", cronExpression);
  }

  @Test
  public void builderWithCallToAddRangeForSecondShouldReturnCorrectValues() {
    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.SECONDS, 5)
        .add(CronPeriod.SECONDS, 10)
        .addRange(CronPeriod.SECONDS, 40, 50)
        .build();
    assertEquals("5,10,40-50 * * * * *", cronExpression);
  }

  @Test
  public void builderWithCallToAddStepForSecondShouldReturnCorrectValues() {
    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.SECONDS, 5)
        .addStep(CronPeriod.SECONDS, 10)
        .addRange(CronPeriod.SECONDS, 40, 50)
        .build();
    assertEquals("5,*/10,40-50 * * * * *", cronExpression);
  }

  @Test
  public void builderWithMinutesShouldReturnWithOnlySecondsSet() {
    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.MINUTES, 5)
        .build();
    assertEquals("* 5 * * * *", cronExpression);
  }

  @Test
  public void builderWithMultipleCallsToAddMinutesShouldReturnWithMultipleValues() {
    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.MINUTES, 5)
        .add(CronPeriod.MINUTES, 10)
        .add(CronPeriod.MINUTES, 20)
        .build();
    assertEquals("* 5,10,20 * * * *", cronExpression);
  }

  @Test
  public void builderWithCallToAddRangeForMinutesShouldReturnCorrectValues() {
    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.MINUTES, 5)
        .add(CronPeriod.MINUTES, 10)
        .addRange(CronPeriod.MINUTES, 40, 50)
        .build();
    assertEquals("* 5,10,40-50 * * * *", cronExpression);
  }

  @Test
  public void builderWithCallToAddStepForMinutesShouldReturnCorrectValues() {
    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.MINUTES, 5)
        .addStep(CronPeriod.MINUTES, 10)
        .addRange(CronPeriod.MINUTES, 40, 50)
        .build();
    assertEquals("* 5,*/10,40-50 * * * *", cronExpression);
  }

  @Test
  public void builderWithCallToAddForSecondsAndMinutesShouldReturnCorrectValues() {
    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.SECONDS, 2)
        .add(CronPeriod.MINUTES, 5)
        .addStep(CronPeriod.MINUTES, 10)
        .addRange(CronPeriod.MINUTES, 40, 50)
        .build();
    assertEquals("2 5,*/10,40-50 * * * *", cronExpression);
  }

  @Test
  public void builderWithHoursShouldReturnWithOnlySecondsSet() {
    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.HOURS, 5)
        .build();
    assertEquals("* * 5 * * *", cronExpression);
  }

  @Test
  public void builderWithMultipleCallsToAddHoursShouldReturnWithMultipleValues() {
    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.HOURS, 5)
        .add(CronPeriod.HOURS, 10)
        .add(CronPeriod.HOURS, 20)
        .build();
    assertEquals("* * 5,10,20 * * *", cronExpression);
  }

  @Test
  public void builderWithCallToAddRangeForHoursShouldReturnCorrectValues() {
    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.HOURS, 5)
        .add(CronPeriod.HOURS, 10)
        .addRange(CronPeriod.HOURS,  11, 12)
        .build();
    assertEquals("* * 5,10,11-12 * * *", cronExpression);
  }

  @Test
  public void builderWithCallToAddStepForHoursShouldReturnCorrectValues() {
    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.HOURS, 5)
        .addStep(CronPeriod.HOURS, 10)
        .addRange(CronPeriod.HOURS, 13, 14)
        .build();
    assertEquals("* * 5,*/10,13-14 * * *", cronExpression);
  }

  @Test
  public void builderWithCallToAddForSecondsMinutesHoursShouldReturnCorrectValues() {
    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.SECONDS, 2)
        .add(CronPeriod.MINUTES, 5)
        .addStep(CronPeriod.MINUTES, 10)
        .addRange(CronPeriod.MINUTES, 40, 50)
        .add(CronPeriod.HOURS, 20)
        .addRange(CronPeriod.HOURS, 1, 2)
        .addStep(CronPeriod.HOURS, 4)
        .addStepRange(CronPeriod.HOURS, 5, 6, 3)
        .build();
    assertEquals("2 5,*/10,40-50 20,1-2,*/4,5-6/3 * * *", cronExpression);
  }

  @Test
  public void builderWithCallToAddForMonthOfDayAndDayOfWeekShouldReturnCorrectValues() {
    String cronExpression = new CronExpressionBuilder()
        .add(MonthOfYear.JAN, MonthOfYear.FEB)
        .add(DayOfWeek.MON, DayOfWeek.THU)
        .add(CronPeriod.SECONDS, 1,2,3)
        .add(CronPeriod.MINUTES, 20,30)
        .build();
    assertEquals("1,2,3 20,30 * * JAN,FEB MON,THU", cronExpression);
  }

  @Test
  public void builderWithCallToAddForRangeMonthOfDayAndDayOfWeekShouldReturnCorrectValues() {
    String cronExpression = new CronExpressionBuilder()
        .add(MonthOfYear.JAN, MonthOfYear.FEB)
        .add(DayOfWeek.MON, DayOfWeek.THU)
        .add(CronPeriod.SECONDS, 1,2,3)
        .add(CronPeriod.MINUTES, 20,30)
        .addRange(MonthOfYear.MAR, MonthOfYear.APR)
        .addRange(DayOfWeek.SUN, DayOfWeek.MON)
        .build();
    assertEquals("1,2,3 20,30 * * JAN,FEB,MAR-APR MON,THU,SUN-MON", cronExpression);
  }

  @Test
  public void builderWithCallToAddForStepShouldReturnCorrectValues() {
    String cronExpression = new CronExpressionBuilder()
        .add(MonthOfYear.JAN, MonthOfYear.FEB)
        .add(DayOfWeek.MON, DayOfWeek.THU)
        .addStep(CronPeriod.HOURS, 20, 2)
        .build();
    assertEquals("* * 20/2 * JAN,FEB MON,THU", cronExpression);
  }

  @Test
  public void builderWithCallToAddAllFieldsShouldReturnCorrectValues() {
    String cronExpression = new CronExpressionBuilder()
        .add(MonthOfYear.JAN, MonthOfYear.FEB)
        .add(DayOfWeek.MON, DayOfWeek.THU)
        .addStep(CronPeriod.HOURS, 20, 2)
        .add(CronPeriod.SECONDS, 1)
        .add(CronPeriod.MINUTES, 1)
        .add(CronPeriod.HOURS, 1)
        .add(CronPeriod.DayOfMonth, 1)
        .build();
    assertEquals("1 1 20/2,1 1 JAN,FEB MON,THU", cronExpression);
  }
}
