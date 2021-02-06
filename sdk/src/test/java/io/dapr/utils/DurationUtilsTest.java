/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.utils;

import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;

public class DurationUtilsTest {

  @Test
  public void convertTimeBothWays() {
    String s = "4h15m50s60ms";
    Duration d1 = DurationUtils.convertDurationFromDaprFormat(s);

    String t = DurationUtils.convertDurationToDaprFormat(d1);
    Assert.assertEquals(s, t);
  }

  @Test
  public void largeHours() {
    // hours part is larger than 24
    String s = "31h15m50s60ms";
    Duration d1 = DurationUtils.convertDurationFromDaprFormat(s);

    String t = DurationUtils.convertDurationToDaprFormat(d1);
    Assert.assertEquals(s, t);
  }

  @Test
  public void negativeDuration() {
    Duration d = Duration.ofSeconds(-99);
    String t = DurationUtils.convertDurationToDaprFormat(d);
    Assert.assertEquals("", t);
  }

  @Test
  public void testGetHoursPart() {
    Duration d1 = Duration.ZERO.plusHours(26);
    Assert.assertEquals(2, DurationUtils.getHoursPart(d1));

    Duration d2 = Duration.ZERO.plusHours(23);
    Assert.assertEquals(23, DurationUtils.getHoursPart(d2));

    Duration d3 = Duration.ZERO.plusHours(24);
    Assert.assertEquals(0, DurationUtils.getHoursPart(d3));
  }

  @Test
  public void testGetMinutesPart() {
    Duration d1 = Duration.ZERO.plusMinutes(61);
    Assert.assertEquals(1, DurationUtils.getMinutesPart(d1));

    Duration d2 = Duration.ZERO.plusMinutes(60);
    Assert.assertEquals(0, DurationUtils.getMinutesPart(d2));

    Duration d3 = Duration.ZERO.plusMinutes(59);
    Assert.assertEquals(59, DurationUtils.getMinutesPart(d3));

    Duration d4 = Duration.ZERO.plusMinutes(3600);
    Assert.assertEquals(0, DurationUtils.getMinutesPart(d4));
  }

  @Test
  public void testGetSecondsPart() {
    Duration d1 = Duration.ZERO.plusSeconds(61);
    Assert.assertEquals(1, DurationUtils.getSecondsPart(d1));

    Duration d2 = Duration.ZERO.plusSeconds(60);
    Assert.assertEquals(0, DurationUtils.getSecondsPart(d2));

    Duration d3 = Duration.ZERO.plusSeconds(59);
    Assert.assertEquals(59, DurationUtils.getSecondsPart(d3));

    Duration d4 = Duration.ZERO.plusSeconds(3600);
    Assert.assertEquals(0, DurationUtils.getSecondsPart(d4));
  }

  @Test
  public void testGetMillisecondsPart() {
    Duration d1 = Duration.ZERO.plusMillis(61);
    Assert.assertEquals(61, DurationUtils.getMilliSecondsPart(d1));

    Duration d2 = Duration.ZERO.plusMillis(60);
    Assert.assertEquals(60, DurationUtils.getMilliSecondsPart(d2));

    Duration d3 = Duration.ZERO.plusMillis(59);
    Assert.assertEquals(59, DurationUtils.getMilliSecondsPart(d3));

    Duration d4 = Duration.ZERO.plusMillis(999);
    Assert.assertEquals(999, DurationUtils.getMilliSecondsPart(d4));

    Duration d5 = Duration.ZERO.plusMillis(1001);
    Assert.assertEquals(1, DurationUtils.getMilliSecondsPart(d5));

    Duration d6 = Duration.ZERO.plusMillis(1000);
    Assert.assertEquals(0, DurationUtils.getMilliSecondsPart(d6));

    Duration d7 = Duration.ZERO.plusMillis(10000);
    Assert.assertEquals(0, DurationUtils.getMilliSecondsPart(d7));
  }
}
