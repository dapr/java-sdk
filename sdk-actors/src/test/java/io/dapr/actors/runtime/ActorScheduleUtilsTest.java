/*
 * Copyright 2026 The Dapr Authors
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

package io.dapr.actors.runtime;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ActorScheduleUtilsTest {

  @Test
  public void validateAcceptsUnsetValues() {
    assertDoesNotThrow(() -> ActorScheduleUtils.validate(null, null, null));
    assertDoesNotThrow(() -> ActorScheduleUtils.validate(Duration.ofSeconds(10), 5, Duration.ZERO));
  }

  @Test
  public void validateRejectsInvalidRepetitions() {
    assertThrows(IllegalArgumentException.class,
        () -> ActorScheduleUtils.validate(Duration.ofSeconds(10), 0, null));
    assertThrows(IllegalArgumentException.class,
        () -> ActorScheduleUtils.validate(Duration.ofSeconds(10), -1, null));
    assertThrows(IllegalArgumentException.class,
        () -> ActorScheduleUtils.validate(null, 5, null));
    assertThrows(IllegalArgumentException.class,
        () -> ActorScheduleUtils.validate(Duration.ZERO, 5, null));
    assertThrows(IllegalArgumentException.class,
        () -> ActorScheduleUtils.validate(Duration.ofSeconds(-10), 5, null));
    assertThrows(IllegalArgumentException.class,
        () -> ActorScheduleUtils.validate(Duration.ofMillis(1500), 5, null));
  }

  @Test
  public void validateRejectsNegativeTtl() {
    assertThrows(IllegalArgumentException.class,
        () -> ActorScheduleUtils.validate(Duration.ofSeconds(10), null, Duration.ofSeconds(-1)));
  }

  @Test
  public void validateRejectsTtlWithoutPeriod() {
    assertThrows(IllegalArgumentException.class,
        () -> ActorScheduleUtils.validate(null, null, Duration.ofMinutes(1)));
    assertThrows(IllegalArgumentException.class,
        () -> ActorScheduleUtils.validate(Duration.ZERO, null, Duration.ofMinutes(1)));
    assertThrows(IllegalArgumentException.class,
        () -> ActorScheduleUtils.validate(Duration.ofMillis(-1), null, Duration.ofMinutes(1)));
  }

  @Test
  public void formatDueTime() {
    // An unset due time must fire right away, so it is sent as zero instead of an empty string.
    assertEquals("0h0m0s0ms", ActorScheduleUtils.formatDueTime(null));
    assertEquals("0h0m1s0ms", ActorScheduleUtils.formatDueTime(Duration.ofSeconds(1)));
  }

  @Test
  public void formatDuration() {
    assertEquals("", ActorScheduleUtils.formatDuration(null));
    assertEquals("0h0m10s0ms", ActorScheduleUtils.formatDuration(Duration.ofSeconds(10)));
  }

  @Test
  public void formatPeriod() {
    assertEquals("", ActorScheduleUtils.formatPeriod(null, null));
    assertEquals("0h0m10s0ms", ActorScheduleUtils.formatPeriod(Duration.ofSeconds(10), null));
    assertEquals("R5/PT10S", ActorScheduleUtils.formatPeriod(Duration.ofSeconds(10), 5));
  }

  @Test
  public void parseRepetitions() {
    assertNull(ActorScheduleUtils.parseRepetitions(null));
    assertNull(ActorScheduleUtils.parseRepetitions(""));
    assertNull(ActorScheduleUtils.parseRepetitions("0h0m10s0ms"));
    assertNull(ActorScheduleUtils.parseRepetitions("PT10S"));
    assertEquals(5, ActorScheduleUtils.parseRepetitions("R5/PT10S"));
    assertEquals(5, ActorScheduleUtils.parseRepetitions("R5"));
    assertNull(ActorScheduleUtils.parseRepetitions("R/PT10S"));
    assertNull(ActorScheduleUtils.parseRepetitions("Rx/PT10S"));
  }
}
