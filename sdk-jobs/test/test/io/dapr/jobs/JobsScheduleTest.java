package io.dapr.jobs.client;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class JobsScheduleTest {

  @Test
  void testFromPeriodValidDuration() {
    Duration duration = Duration.ofHours(1).plusMinutes(30)
        .plusSeconds(15).plusMillis(500);
    JobsSchedule schedule = JobsSchedule.fromPeriod(duration);
    assertEquals("@every 1h30m15s500ms", schedule.getExpression());
  }

  @Test
  void testFromPeriodValidDurationWithoutSecondsAndMillSeconds() {
    Duration duration = Duration.ofHours(1).plusMinutes(30);
    JobsSchedule schedule = JobsSchedule.fromPeriod(duration);
    assertEquals("@every 1h30m0s0ms", schedule.getExpression());
  }

  @Test
  void testFromPeriodNullDuration() {
    Exception exception = assertThrows(IllegalArgumentException.class, () -> JobsSchedule.fromPeriod(null));
    assertEquals("duration cannot be null", exception.getMessage());
  }

  @Test
  void testFromString() {
    String cronExpression = "0 0 * * *";
    JobsSchedule schedule = JobsSchedule.fromString(cronExpression);
    assertEquals(cronExpression, schedule.getExpression());
  }

  @Test
  void testYearly() {
    JobsSchedule schedule = JobsSchedule.yearly();
    assertEquals("0 0 0 1 1 *", schedule.getExpression());
  }

  @Test
  void testMonthly() {
    JobsSchedule schedule = JobsSchedule.monthly();
    assertEquals("0 0 0 1 * *", schedule.getExpression());
  }

  @Test
  void testWeekly() {
    JobsSchedule schedule = JobsSchedule.weekly();
    assertEquals("0 0 0 * * 0", schedule.getExpression());
  }

  @Test
  void testDaily() {
    JobsSchedule schedule = JobsSchedule.daily();
    assertEquals("0 0 0 * * *", schedule.getExpression());
  }

  @Test
  void testHourly() {
    JobsSchedule schedule = JobsSchedule.hourly();
    assertEquals("0 0 * * * *", schedule.getExpression());
  }
}
