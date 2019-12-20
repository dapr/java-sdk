package io.dapr.actors.runtime;

import org.junit.Assert;
import org.junit.Test;
import java.time.Duration;
import java.util.Arrays;

public class ReminderInfoTest {
  @Test(expected = IllegalArgumentException.class)
  public void outOfRangeDueTime() {
    ReminderInfo info = new ReminderInfo(null, Duration.ZERO.plusSeconds(-10), Duration.ZERO.plusMinutes(1));
  }

  @Test
  public void negativePeriod() {
    // this is ok
    ReminderInfo info = new ReminderInfo(null, Duration.ZERO.plusMinutes(1), Duration.ZERO.plusMillis(-1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void outOfRangePeriod() {
    ReminderInfo info = new ReminderInfo(null, Duration.ZERO.plusMinutes(1), Duration.ZERO.plusMinutes(-10));
  }

  @Test
  public void noState() {
    ReminderInfo original = new ReminderInfo(null, Duration.ZERO.plusMinutes(2), Duration.ZERO.plusMinutes((5)));
    ReminderInfo recreated = null;
    try {
      String serialized = original.serialize();
      recreated = ReminderInfo.deserialize(serialized.getBytes());
    }
    catch(Exception e) {
      System.out.println("The error is: " + e);
      Assert.fail();
    }

    Assert.assertEquals(original.data, recreated.data);
    Assert.assertEquals(original.dueTime, recreated.dueTime);
    Assert.assertEquals(original.period, recreated.period);
  }

  @Test
  public void withState() {
    ReminderInfo original = new ReminderInfo("maru".getBytes(), Duration.ZERO.plusMinutes(2), Duration.ZERO.plusMinutes((5)));
    ReminderInfo recreated = null;
    try {
      String serialized = original.serialize();
      recreated = ReminderInfo.deserialize(serialized.getBytes());
    }
    catch(Exception e) {
      System.out.println("The error is: " + e);
      Assert.fail();
    }

    Assert.assertTrue(Arrays.equals(original.data, recreated.data));
    Assert.assertEquals(original.dueTime, recreated.dueTime);
    Assert.assertEquals(original.period, recreated.period);
  }
}
