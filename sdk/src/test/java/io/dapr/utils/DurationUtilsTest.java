/*
 * Copyright 2021 The Dapr Authors
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

package io.dapr.utils;


import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DurationUtilsTest {

  @Test
  public void convertTimeBothWays() {
    String s = "4h15m50s60ms";
    Duration d1 = DurationUtils.convertDurationFromDaprFormat(s);

    String t = DurationUtils.convertDurationToDaprFormat(d1);
    assertEquals(s, t);
  }

  @Test
  public void testParsePartialDurationMillis(){
    String s = "0h0m0s101ms";
    String partial = "101ms";
    Duration d1 = DurationUtils.convertDurationFromDaprFormat(partial);

    String t = DurationUtils.convertDurationToDaprFormat(d1);
    assertEquals(s, t);
  }

  @Test
  public void testParsePartialDurationSeconds(){
    String s = "0h0m42s0ms";
    String partial = "42s";
    Duration d1 = DurationUtils.convertDurationFromDaprFormat(partial);

    String t = DurationUtils.convertDurationToDaprFormat(d1);
    assertEquals(s, t);
  }

  @Test
  public void testParsePartialDurationMinutes(){
    String s = "0h29m0s0ms";
    String partial = "29m";
    Duration d1 = DurationUtils.convertDurationFromDaprFormat(partial);

    String t = DurationUtils.convertDurationToDaprFormat(d1);
    assertEquals(s, t);
  }

  @Test
  public void testParsePartialDurationHours(){
    String s = "17h0m0s0ms";
    String partial = "17h";
    Duration d1 = DurationUtils.convertDurationFromDaprFormat(partial);

    String t = DurationUtils.convertDurationToDaprFormat(d1);
    assertEquals(s, t);
  }

  @Test
  public void testZeroDurationString(){
    String s = "0h0m0s0ms";
    String partial = "0";
    Duration d1 = DurationUtils.convertDurationFromDaprFormat(partial);

    String t = DurationUtils.convertDurationToDaprFormat(d1);
    assertEquals(s, t);
  }

  @Test
  public void testZeroDuration(){
    String s = "0h0m0s0ms";
    String t = DurationUtils.convertDurationToDaprFormat(Duration.ZERO);
    assertEquals(s, t);
  }

  @Test
  public void testNullString() {
    assertThrows(IllegalArgumentException.class, () ->{
      DurationUtils.convertDurationFromDaprFormat(null);
    });
  }

  @Test
  public void testEmptyString() {
    Duration d = DurationUtils.convertDurationFromDaprFormat("");
    assertEquals(Duration.ZERO, d);
  }

  @Test
  public void largeHours() {
    // hours part is larger than 24
    String s = "31h15m50s60ms";
    Duration d1 = DurationUtils.convertDurationFromDaprFormat(s);

    String t = DurationUtils.convertDurationToDaprFormat(d1);
    assertEquals(s, t);
  }

  @Test
  public void negativeDuration() {
    Duration d = Duration.ofSeconds(-99);
    String t = DurationUtils.convertDurationToDaprFormat(d);
    assertEquals("", t);
  }

  @Test
  public void testGetHoursPart() {
    Duration d1 = Duration.ZERO.plusHours(26);
    assertEquals(2, DurationUtils.getHoursPart(d1));

    Duration d2 = Duration.ZERO.plusHours(23);
    assertEquals(23, DurationUtils.getHoursPart(d2));

    Duration d3 = Duration.ZERO.plusHours(24);
    assertEquals(0, DurationUtils.getHoursPart(d3));
  }

  @Test
  public void testGetMinutesPart() {
    Duration d1 = Duration.ZERO.plusMinutes(61);
    assertEquals(1, DurationUtils.getMinutesPart(d1));

    Duration d2 = Duration.ZERO.plusMinutes(60);
    assertEquals(0, DurationUtils.getMinutesPart(d2));

    Duration d3 = Duration.ZERO.plusMinutes(59);
    assertEquals(59, DurationUtils.getMinutesPart(d3));

    Duration d4 = Duration.ZERO.plusMinutes(3600);
    assertEquals(0, DurationUtils.getMinutesPart(d4));
  }

  @Test
  public void testGetSecondsPart() {
    Duration d1 = Duration.ZERO.plusSeconds(61);
    assertEquals(1, DurationUtils.getSecondsPart(d1));

    Duration d2 = Duration.ZERO.plusSeconds(60);
    assertEquals(0, DurationUtils.getSecondsPart(d2));

    Duration d3 = Duration.ZERO.plusSeconds(59);
    assertEquals(59, DurationUtils.getSecondsPart(d3));

    Duration d4 = Duration.ZERO.plusSeconds(3600);
    assertEquals(0, DurationUtils.getSecondsPart(d4));
  }

  @Test
  public void testGetMillisecondsPart() {
    Duration d1 = Duration.ZERO.plusMillis(61);
    assertEquals(61, DurationUtils.getMilliSecondsPart(d1));

    Duration d2 = Duration.ZERO.plusMillis(60);
    assertEquals(60, DurationUtils.getMilliSecondsPart(d2));

    Duration d3 = Duration.ZERO.plusMillis(59);
    assertEquals(59, DurationUtils.getMilliSecondsPart(d3));

    Duration d4 = Duration.ZERO.plusMillis(999);
    assertEquals(999, DurationUtils.getMilliSecondsPart(d4));

    Duration d5 = Duration.ZERO.plusMillis(1001);
    assertEquals(1, DurationUtils.getMilliSecondsPart(d5));

    Duration d6 = Duration.ZERO.plusMillis(1000);
    assertEquals(0, DurationUtils.getMilliSecondsPart(d6));

    Duration d7 = Duration.ZERO.plusMillis(10000);
    assertEquals(0, DurationUtils.getMilliSecondsPart(d7));
  }
}
