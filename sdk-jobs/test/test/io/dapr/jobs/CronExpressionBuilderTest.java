package io.dapr.jobs;

import io.dapr.jobs.client.CronExpressionBuilder;
import io.dapr.jobs.client.CronPeriod;
import io.dapr.jobs.client.DayOfWeek;
import io.dapr.jobs.client.MonthOfYear;
import org.junit.Assert;
import org.junit.Test;

public class CronExpressionBuilderTest {

  @Test
  public void builderWithoutParametersShouldReturnDefaultValues() {
    String cronExpression = new CronExpressionBuilder().build();
    Assert.assertEquals("* * * * * *", cronExpression);
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
  public void builderWithInvalidDayOfMonthShouldThrowIllegalArgumentException() {
    IllegalArgumentException exception = Assert.assertThrows(IllegalArgumentException.class,
        () -> new CronExpressionBuilder()
            .add(CronPeriod.DayOfMonth, 32).build());
    Assert.assertTrue(exception.getMessage().contains("DayOfMonth must be between [1, 31]"));
    exception = Assert.assertThrows(IllegalArgumentException.class,
        () -> new CronExpressionBuilder()
            .add(CronPeriod.DayOfMonth, 0).build());
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
  public void builderWithSecondsShouldReturnWithOnlySecondsSet() {
    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.SECONDS, 5)
        .build();
    Assert.assertEquals("5 * * * * *", cronExpression);
  }

  @Test
  public void builderWithMultipleCallsToAddSecondsShouldReturnWithMultipleValues() {
    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.SECONDS, 5)
        .add(CronPeriod.SECONDS, 10)
        .add(CronPeriod.SECONDS, 20)
        .build();
    Assert.assertEquals("5,10,20 * * * * *", cronExpression);
  }

  @Test
  public void builderWithCallToAddRangeForSecondShouldReturnCorrectValues() {
    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.SECONDS, 5)
        .add(CronPeriod.SECONDS, 10)
        .addRange(CronPeriod.SECONDS, 40, 50)
        .build();
    Assert.assertEquals("5,10,40-50 * * * * *", cronExpression);
  }

  @Test
  public void builderWithCallToAddStepForSecondShouldReturnCorrectValues() {
    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.SECONDS, 5)
        .addStep(CronPeriod.SECONDS, 10)
        .addRange(CronPeriod.SECONDS, 40, 50)
        .build();
    Assert.assertEquals("5,*/10,40-50 * * * * *", cronExpression);
  }

  @Test
  public void builderWithMinutesShouldReturnWithOnlySecondsSet() {
    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.MINUTES, 5)
        .build();
    Assert.assertEquals("* 5 * * * *", cronExpression);
  }

  @Test
  public void builderWithMultipleCallsToAddMinutesShouldReturnWithMultipleValues() {
    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.MINUTES, 5)
        .add(CronPeriod.MINUTES, 10)
        .add(CronPeriod.MINUTES, 20)
        .build();
    Assert.assertEquals("* 5,10,20 * * * *", cronExpression);
  }

  @Test
  public void builderWithCallToAddRangeForMinutesShouldReturnCorrectValues() {
    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.MINUTES, 5)
        .add(CronPeriod.MINUTES, 10)
        .addRange(CronPeriod.MINUTES, 40, 50)
        .build();
    Assert.assertEquals("* 5,10,40-50 * * * *", cronExpression);
  }

  @Test
  public void builderWithCallToAddStepForMinutesShouldReturnCorrectValues() {
    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.MINUTES, 5)
        .addStep(CronPeriod.MINUTES, 10)
        .addRange(CronPeriod.MINUTES, 40, 50)
        .build();
    Assert.assertEquals("* 5,*/10,40-50 * * * *", cronExpression);
  }

  @Test
  public void builderWithCallToAddForSecondsAndMinutesShouldReturnCorrectValues() {
    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.SECONDS, 2)
        .add(CronPeriod.MINUTES, 5)
        .addStep(CronPeriod.MINUTES, 10)
        .addRange(CronPeriod.MINUTES, 40, 50)
        .build();
    Assert.assertEquals("2 5,*/10,40-50 * * * *", cronExpression);
  }

  @Test
  public void builderWithHoursShouldReturnWithOnlySecondsSet() {
    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.HOURS, 5)
        .build();
    Assert.assertEquals("* * 5 * * *", cronExpression);
  }

  @Test
  public void builderWithMultipleCallsToAddHoursShouldReturnWithMultipleValues() {
    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.HOURS, 5)
        .add(CronPeriod.HOURS, 10)
        .add(CronPeriod.HOURS, 20)
        .build();
    Assert.assertEquals("* * 5,10,20 * * *", cronExpression);
  }

  @Test
  public void builderWithCallToAddRangeForHoursShouldReturnCorrectValues() {
    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.HOURS, 5)
        .add(CronPeriod.HOURS, 10)
        .addRange(CronPeriod.HOURS,  11, 12)
        .build();
    Assert.assertEquals("* * 5,10,11-12 * * *", cronExpression);
  }

  @Test
  public void builderWithCallToAddStepForHoursShouldReturnCorrectValues() {
    String cronExpression = new CronExpressionBuilder()
        .add(CronPeriod.HOURS, 5)
        .addStep(CronPeriod.HOURS, 10)
        .addRange(CronPeriod.HOURS, 13, 14)
        .build();
    Assert.assertEquals("* * 5,*/10,13-14 * * *", cronExpression);
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
    Assert.assertEquals("2 5,*/10,40-50 20,1-2,*/4,5-6/3 * * *", cronExpression);
  }

  @Test
  public void builderWithCallToAddForMonthOfDayAndDayOfWeekShouldReturnCorrectValues() {
    String cronExpression = new CronExpressionBuilder()
        .add(MonthOfYear.JAN, MonthOfYear.FEB)
        .add(DayOfWeek.MON, DayOfWeek.THU)
        .add(CronPeriod.SECONDS, 1,2,3)
        .add(CronPeriod.MINUTES, 20,30)
        .build();
    Assert.assertEquals("1,2,3 20,30 * * JAN,FEB MON,THU", cronExpression);
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
    Assert.assertEquals("1,2,3 20,30 * * JAN,FEB,MAR-APR MON,THU,SUN-MON", cronExpression);
  }
}
