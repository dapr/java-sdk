package io.dapr.actors.runtime;

import io.dapr.utils.RepeatedDuration;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;

public class ActorTimerParamsTest {
  private static final ActorObjectSerializer SERIALIZER = new ActorObjectSerializer();

  @Test
  public void ttlShouldBeOptional() throws IOException {
    // Arrange
    ActorTimerParams params =
        new ActorTimerParams("callback", new byte[1], Duration.ZERO, new RepeatedDuration(Duration.ZERO, 1));

    // Act
    byte[] serialized = SERIALIZER.serialize(params);
    ActorTimerParams deserialized = SERIALIZER.deserialize(serialized, ActorTimerParams.class);

    // Assert
    Assert.assertFalse(deserialized.getTtl().isPresent());
  }

  @Test
  public void ttlRepetitionsNotRequired() throws IOException {
    // Arrange
    ActorTimerParams params =
        new ActorTimerParams("callback", new byte[1], Duration.ZERO, new RepeatedDuration(Duration.ZERO));

    // Act
    byte[] serialized = SERIALIZER.serialize(params);
    ActorTimerParams deserialized = SERIALIZER.deserialize(serialized, ActorTimerParams.class);

    // Assert
    Assert.assertTrue(deserialized.getTtl().isPresent());
    Assert.assertFalse(deserialized.getTtl().get().getRepetitions().isPresent());
  }
}
