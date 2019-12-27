package io.dapr.actors.runtime;

import org.junit.Assert;
import org.junit.Test;
import java.time.Duration;

public class ActorReminderParamsTest {

  private static final ActorStateSerializer SERIALIZER = new ActorStateSerializer();

  @Test(expected = IllegalArgumentException.class)
  public void outOfRangeDueTime() {
    ActorReminderParams info = new ActorReminderParams(null, Duration.ZERO.plusSeconds(-10), Duration.ZERO.plusMinutes(1));
  }

  @Test
  public void negativePeriod() {
    // this is ok
    ActorReminderParams info = new ActorReminderParams(null, Duration.ZERO.plusMinutes(1), Duration.ZERO.plusMillis(-1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void outOfRangePeriod() {
    ActorReminderParams info = new ActorReminderParams(null, Duration.ZERO.plusMinutes(1), Duration.ZERO.plusMinutes(-10));
  }

  @Test
  public void noState() {
    ActorReminderParams original = new ActorReminderParams(null, Duration.ZERO.plusMinutes(2), Duration.ZERO.plusMinutes((5)));
    ActorReminderParams recreated = null;
    try {
      String serialized = SERIALIZER.serialize(original);
      recreated = SERIALIZER.deserialize(serialized, ActorReminderParams.class);
    }
    catch(Exception e) {
      System.out.println("The error is: " + e);
      Assert.fail();
    }

    Assert.assertEquals(original.getData(), recreated.getData());
    Assert.assertEquals(original.getDueTime(), recreated.getDueTime());
    Assert.assertEquals(original.getPeriod(), recreated.getPeriod());
  }

  @Test
  public void withState() {
    ActorReminderParams original = new ActorReminderParams("maru", Duration.ZERO.plusMinutes(2), Duration.ZERO.plusMinutes((5)));
    ActorReminderParams recreated = null;
    try {
      String serialized = SERIALIZER.serialize(original);
      recreated = SERIALIZER.deserialize(serialized, ActorReminderParams.class);
    }
    catch(Exception e) {
      System.out.println("The error is: " + e);
      Assert.fail();
    }

    Assert.assertEquals(original.getData(), recreated.getData());
    Assert.assertEquals(original.getDueTime(), recreated.getDueTime());
    Assert.assertEquals(original.getPeriod(), recreated.getPeriod());
  }
}
