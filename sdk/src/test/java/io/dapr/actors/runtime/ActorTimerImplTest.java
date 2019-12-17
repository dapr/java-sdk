package io.dapr.actors.runtime;

import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;

public class ActorTimerImplTest {

  @Test
  public void serialize() {
    Duration dueTime = Duration.ZERO
      .plusMinutes(7)
      .plusSeconds(17);

    Duration period = Duration.ZERO
      .plusHours(1)
      .plusSeconds(3);

    ActorTimerImpl timer = new ActorTimerImpl(
      null,
      "testTimer",
      null,
      null,
      dueTime,
      period);
    String s = timer.serialize();

    String expected = "{\"period\":\"1h0m3s0ms\",\"dueTime\":\"0h7m17s0ms\"}";
    Assert.assertEquals(expected, s);
  }

  @Test
  public void serializeWithOneTimePeriod() {
    Duration dueTime = Duration.ZERO
      .plusMinutes(7)
      .plusSeconds(17);

    // this is intentionally negative
    Duration period = Duration.ZERO
      .minusHours(1)
      .minusMinutes(3);

    ActorTimerImpl timer = new ActorTimerImpl(
      null,
      "testTimer",
      null,
      null,
      dueTime,
      period);
    String s = timer.serialize();

    // A negative period will be serialized to an empty string which is interpreted by Dapr to mean fire once only.
    String expected = "{\"period\":\"\",\"dueTime\":\"0h7m17s0ms\"}";
    Assert.assertEquals(expected, s);
  }
}
