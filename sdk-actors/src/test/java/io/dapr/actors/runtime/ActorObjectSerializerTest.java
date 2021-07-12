/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */
package io.dapr.actors.runtime;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.Charset;
import java.time.Duration;

public class ActorObjectSerializerTest {

  private static final String FULL_ACTOR_CONFIG = "{\"entities\":[\"actor1\"],\"actorIdleTimeout\":\"0h0m1s0ms\",\"actorScanInterval\":\"0h0m2s0ms\",\"drainOngoingCallTimeout\":\"0h0m3s0ms\",\"drainBalancedActors\":false,\"reentrancy\":{\"enabled\":true,\"maxStackDepth\":32}}";
  private static final String PARTIAL_REENTRANCY_ACTOR_CONFIG = "{\"entities\":[\"actor1\"],\"actorIdleTimeout\":\"0h0m1s0ms\",\"actorScanInterval\":\"0h0m2s0ms\",\"drainOngoingCallTimeout\":\"0h0m3s0ms\",\"drainBalancedActors\":false,\"reentrancy\":{\"enabled\":true}}";
  private static final String NO_REENTRANCY_ACTOR_CONFIG = "{\"entities\":[\"actor1\"],\"actorIdleTimeout\":\"0h0m1s0ms\",\"actorScanInterval\":\"0h0m2s0ms\",\"drainOngoingCallTimeout\":\"0h0m3s0ms\",\"drainBalancedActors\":false}";
  private static final String EMPTY_ACTOR_CONFIG = "{\"entities\":[]}";

  @Test
  public void TestActorRuntimeConfigSerialization() throws Exception {
    final ActorObjectSerializer serializer = new ActorObjectSerializer();
    final ActorRuntimeConfig config = new ActorRuntimeConfig()
        .setActorIdleTimeout(Duration.ofSeconds(1L))
        .setActorScanInterval(Duration.ofSeconds(2L))
        .setDrainOngoingCallTimeout(Duration.ofSeconds(3L))
        .setDrainBalancedActors(false)
        .setActorReentrancyConfig(true, 32);

    config.addRegisteredActorType("actor1");

    final byte[] serializedBytes = serializer.serialize(config);
    final String serializedConfig = new String(serializedBytes, Charset.defaultCharset());
    Assert.assertEquals(FULL_ACTOR_CONFIG, serializedConfig);
  }

  @Test
  public void TestActorRuntimeConfigWithPartialReentrancy() throws Exception {
    final ActorObjectSerializer serializer = new ActorObjectSerializer();
    final ActorRuntimeConfig config = new ActorRuntimeConfig()
        .setActorIdleTimeout(Duration.ofSeconds(1L))
        .setActorScanInterval(Duration.ofSeconds(2L))
        .setDrainOngoingCallTimeout(Duration.ofSeconds(3L))
        .setDrainBalancedActors(false)
        .setActorReentrancyConfig(true, null);

    config.addRegisteredActorType("actor1");

    final byte[] serializedBytes = serializer.serialize(config);
    final String serializedConfig = new String(serializedBytes, Charset.defaultCharset());
    Assert.assertEquals(PARTIAL_REENTRANCY_ACTOR_CONFIG, serializedConfig);
  }

  @Test
  public void TestActorRuntimeConfigWithoutReentrancy() throws Exception {
    final ActorObjectSerializer serializer = new ActorObjectSerializer();
    final ActorRuntimeConfig config = new ActorRuntimeConfig()
        .setActorIdleTimeout(Duration.ofSeconds(1L))
        .setActorScanInterval(Duration.ofSeconds(2L))
        .setDrainOngoingCallTimeout(Duration.ofSeconds(3L))
        .setDrainBalancedActors(false);

    config.addRegisteredActorType("actor1");

    final byte[] serializedBytes = serializer.serialize(config);
    final String serializedConfig = new String(serializedBytes, Charset.defaultCharset());
    Assert.assertEquals(NO_REENTRANCY_ACTOR_CONFIG, serializedConfig);
  }

  @Test
  public void TestActorRuntimeEmptyConfigSerialization() throws Exception {
    final ActorObjectSerializer serializer = new ActorObjectSerializer();
    final ActorRuntimeConfig config = new ActorRuntimeConfig();

    final byte[] serializedBytes = serializer.serialize(config);
    final String serializedConfig = new String(serializedBytes, Charset.defaultCharset());
    Assert.assertEquals(EMPTY_ACTOR_CONFIG, serializedConfig);
  }
}