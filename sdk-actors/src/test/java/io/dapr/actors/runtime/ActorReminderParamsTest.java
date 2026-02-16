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

import io.dapr.client.domain.ConstantFailurePolicy;
import io.dapr.client.domain.DropFailurePolicy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ActorReminderParamsTest {

  private static final ActorObjectSerializer SERIALIZER = new ActorObjectSerializer();

  @Test
  public void outOfRangeDueTime() {
    assertThrows(IllegalArgumentException.class, () -> new ActorReminderParams(null, Duration.ZERO.plusSeconds(-10), Duration.ZERO.plusMinutes(1)));
  }

  @Test
  public void negativePeriod() {
    // this is ok
    ActorReminderParams info = new ActorReminderParams(null, Duration.ZERO.plusMinutes(1), Duration.ZERO.plusMillis(-1));
  }

  @Test
  public void outOfRangePeriod() {
    assertThrows(IllegalArgumentException.class, () ->new ActorReminderParams(null, Duration.ZERO.plusMinutes(1), Duration.ZERO.plusMinutes(-10)));
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
      Assertions.fail();
    }

    Assertions.assertArrayEquals(original.getData(), recreated.getData());
    Assertions.assertEquals(original.getDueTime(), recreated.getDueTime());
    Assertions.assertEquals(original.getPeriod(), recreated.getPeriod());
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
      Assertions.fail();
    }

    Assertions.assertArrayEquals(original.getData(), recreated.getData());
    Assertions.assertEquals(original.getDueTime(), recreated.getDueTime());
    Assertions.assertEquals(original.getPeriod(), recreated.getPeriod());
  }

  @Test
  public void withDropFailurePolicy() {
    ActorReminderParams original = new ActorReminderParams("maru".getBytes(),
        Duration.ZERO.plusMinutes(2),
        Duration.ZERO.plusMinutes(5),
        new DropFailurePolicy());
    ActorReminderParams recreated = null;
    try {
      byte[] serialized = SERIALIZER.serialize(original);
      recreated = SERIALIZER.deserialize(serialized, ActorReminderParams.class);
    } catch (Exception e) {
      System.out.println("The error is: " + e);
      Assertions.fail();
    }

    Assertions.assertArrayEquals(original.getData(), recreated.getData());
    Assertions.assertEquals(original.getDueTime(), recreated.getDueTime());
    Assertions.assertEquals(original.getPeriod(), recreated.getPeriod());
    Assertions.assertEquals(original.getFailurePolicy().getFailurePolicyType(), recreated.getFailurePolicy().getFailurePolicyType());
  }

  @Test
  public void withConstantRetryFailurePolicy() {
    ActorReminderParams original = new ActorReminderParams("maru".getBytes(),
        Duration.ZERO.plusMinutes(2),
        Duration.ZERO.plusMinutes(5),
        new ConstantFailurePolicy(4));
    ActorReminderParams recreated = null;
    try {
      byte[] serialized = SERIALIZER.serialize(original);
      recreated = SERIALIZER.deserialize(serialized, ActorReminderParams.class);
    } catch (Exception e) {
      System.out.println("The error is: " + e);
      Assertions.fail();
    }

    Assertions.assertArrayEquals(original.getData(), recreated.getData());
    Assertions.assertEquals(original.getDueTime(), recreated.getDueTime());
    Assertions.assertEquals(original.getPeriod(), recreated.getPeriod());
    Assertions.assertEquals(original.getFailurePolicy().getFailurePolicyType(), recreated.getFailurePolicy().getFailurePolicyType());
    Assertions.assertEquals(((ConstantFailurePolicy) original.getFailurePolicy()).getMaxRetries(), ((ConstantFailurePolicy) recreated.getFailurePolicy()).getMaxRetries());
  }

  @Test
  public void withConstantIntervalFailurePolicy() {
    ActorReminderParams original = new ActorReminderParams("maru".getBytes(),
        Duration.ZERO.plusMinutes(2),
        Duration.ZERO.plusMinutes(5),
        new ConstantFailurePolicy(Duration.ofSeconds(4)));
    ActorReminderParams recreated = null;
    try {
      byte[] serialized = SERIALIZER.serialize(original);
      recreated = SERIALIZER.deserialize(serialized, ActorReminderParams.class);
    } catch (Exception e) {
      System.out.println("The error is: " + e);
      Assertions.fail();
    }

    Assertions.assertArrayEquals(original.getData(), recreated.getData());
    Assertions.assertEquals(original.getDueTime(), recreated.getDueTime());
    Assertions.assertEquals(original.getPeriod(), recreated.getPeriod());
    Assertions.assertEquals(original.getFailurePolicy().getFailurePolicyType(), recreated.getFailurePolicy().getFailurePolicyType());
    Assertions.assertEquals(((ConstantFailurePolicy) original.getFailurePolicy()).getDurationBetweenRetries(), ((ConstantFailurePolicy) recreated.getFailurePolicy()).getDurationBetweenRetries());
  }
}
