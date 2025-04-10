/*
 * Copyright 2025 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.client.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JobsScheduleTest {

  @Test
  void testFromPeriodValidDuration() {
    Duration duration = Duration.ofHours(1).plusMinutes(30)
        .plusSeconds(15).plusMillis(500);
    JobSchedule schedule = JobSchedule.fromPeriod(duration);
    assertEquals("@every 1h30m15s500ms", schedule.getExpression());
  }

  @Test
  void testFromPeriodValidDurationWithoutSecondsAndMillSeconds() {
    Duration duration = Duration.ofHours(1).plusMinutes(30);
    JobSchedule schedule = JobSchedule.fromPeriod(duration);
    assertEquals("@every 1h30m0s0ms", schedule.getExpression());
  }

  @Test
  void testFromPeriodNullDuration() {
    Exception exception = assertThrows(IllegalArgumentException.class, () -> JobSchedule.fromPeriod(null));
    assertEquals("duration cannot be null", exception.getMessage());
  }

  @Test
  void testFromStringThrowsIllegalArgumentWhenExpressionIsNull() {
    Exception exception = assertThrows(IllegalArgumentException.class, () -> JobSchedule.fromString(null));
    assertEquals("cronExpression cannot be null", exception.getMessage());
  }

  @Test
  void testFromString() {
    String cronExpression = "0 0 * * *";
    JobSchedule schedule = JobSchedule.fromString(cronExpression);
    assertEquals(cronExpression, schedule.getExpression());
  }

  @Test
  void testYearly() {
    JobSchedule schedule = JobSchedule.yearly();
    assertEquals("0 0 0 1 1 *", schedule.getExpression());
  }

  @Test
  void testMonthly() {
    JobSchedule schedule = JobSchedule.monthly();
    assertEquals("0 0 0 1 * *", schedule.getExpression());
  }

  @Test
  void testWeekly() {
    JobSchedule schedule = JobSchedule.weekly();
    assertEquals("0 0 0 * * 0", schedule.getExpression());
  }

  @Test
  void testDaily() {
    JobSchedule schedule = JobSchedule.daily();
    assertEquals("0 0 0 * * *", schedule.getExpression());
  }

  @Test
  void testHourly() {
    JobSchedule schedule = JobSchedule.hourly();
    assertEquals("0 0 * * * *", schedule.getExpression());
  }
}
