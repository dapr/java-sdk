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

package io.dapr.actors.runtime;

import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;

public class ActorReminderParamsTest {

  private static final ActorObjectSerializer SERIALIZER = new ActorObjectSerializer();

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
      byte[] serialized = SERIALIZER.serialize(original);
      recreated = SERIALIZER.deserialize(serialized, ActorReminderParams.class);
    }
    catch(Exception e) {
      System.out.println("The error is: " + e);
      Assert.fail();
    }

    Assert.assertArrayEquals(original.getData(), recreated.getData());
    Assert.assertEquals(original.getDueTime(), recreated.getDueTime());
    Assert.assertEquals(original.getPeriod(), recreated.getPeriod());
  }

  @Test
  public void withState() {
    ActorReminderParams original = new ActorReminderParams("maru".getBytes(), Duration.ZERO.plusMinutes(2), Duration.ZERO.plusMinutes((5)));
    ActorReminderParams recreated = null;
    try {
      byte[] serialized = SERIALIZER.serialize(original);
      recreated = SERIALIZER.deserialize(serialized, ActorReminderParams.class);
    }
    catch(Exception e) {
      System.out.println("The error is: " + e);
      Assert.fail();
    }

    Assert.assertArrayEquals(original.getData(), recreated.getData());
    Assert.assertEquals(original.getDueTime(), recreated.getDueTime());
    Assert.assertEquals(original.getPeriod(), recreated.getPeriod());
  }
}
