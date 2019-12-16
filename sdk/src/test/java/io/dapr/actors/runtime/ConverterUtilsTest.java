package io.dapr.actors.runtime;

import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;

public class ConverterUtilsTest {

    @Test
    public void convertTimeBothWays() {
        String s = "4h15m50s60ms";
        Duration d = ConverterUtils.ConvertTimeSpanFromDaprFormat(s);

        String t = ConverterUtils.ConvertDurationToDaprFormat(d);
        Assert.assertEquals(s, t);
    }

    @Test
    public void negativeDuration() {
        Duration d = Duration.ofSeconds(-99);
        String t = ConverterUtils.ConvertDurationToDaprFormat(d);
        Assert.assertEquals("", t);
    }

    @Test
    public void testGetHoursPart() {
        Duration d1 = Duration.ZERO.plusHours(26);
        Assert.assertEquals(2, ConverterUtils.getHoursPart(d1));

        Duration d2 = Duration.ZERO.plusHours(23);
        Assert.assertEquals(23, ConverterUtils.getHoursPart(d2));

        Duration d3 = Duration.ZERO.plusHours(24);
        Assert.assertEquals(0, ConverterUtils.getHoursPart(d3));
    }

    @Test
    public void testGetMinutesPart() {
        Duration d1 = Duration.ZERO.plusMinutes(61);
        Assert.assertEquals(1, ConverterUtils.getMinutesPart(d1));

        Duration d2 = Duration.ZERO.plusMinutes(60);
        Assert.assertEquals(0, ConverterUtils.getMinutesPart(d2));

        Duration d3 = Duration.ZERO.plusMinutes(59);
        Assert.assertEquals(59, ConverterUtils.getMinutesPart(d3));

        Duration d4 = Duration.ZERO.plusMinutes(3600);
        Assert.assertEquals(0, ConverterUtils.getMinutesPart(d4));
    }

    @Test
    public void testGetSecondsPart() {
        Duration d1 = Duration.ZERO.plusSeconds(61);
        Assert.assertEquals(1, ConverterUtils.getSecondsPart(d1));

        Duration d2 = Duration.ZERO.plusSeconds(60);
        Assert.assertEquals(0, ConverterUtils.getSecondsPart(d2));

        Duration d3 = Duration.ZERO.plusSeconds(59);
        Assert.assertEquals(59, ConverterUtils.getSecondsPart(d3));

        Duration d4 = Duration.ZERO.plusSeconds(3600);
        Assert.assertEquals(0, ConverterUtils.getSecondsPart(d4));
    }

    @Test
    public void testGetMillisecondsPart() {
        Duration d1 = Duration.ZERO.plusMillis(61);
        Assert.assertEquals(61, ConverterUtils.getMilliSecondsPart(d1));

        Duration d2 = Duration.ZERO.plusMillis(60);
        Assert.assertEquals(60, ConverterUtils.getMilliSecondsPart(d2));

        Duration d3 = Duration.ZERO.plusMillis(59);
        Assert.assertEquals(59, ConverterUtils.getMilliSecondsPart(d3));

        Duration d4 = Duration.ZERO.plusMillis(999);
        Assert.assertEquals(999, ConverterUtils.getMilliSecondsPart(d4));

        Duration d5 = Duration.ZERO.plusMillis(1001);
        Assert.assertEquals(1, ConverterUtils.getMilliSecondsPart(d5));

        Duration d6 = Duration.ZERO.plusMillis(1000);
        Assert.assertEquals(0, ConverterUtils.getMilliSecondsPart(d6));

        Duration d7 = Duration.ZERO.plusMillis(10000);
        Assert.assertEquals(0, ConverterUtils.getMilliSecondsPart(d7));
    }
}
