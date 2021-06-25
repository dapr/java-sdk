/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */
package io.dapr.actors.config;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ActorReentrancyConfigTest {

  @Test
  public void TestFullConfig() {
    final ActorReentrancyConfig config = new ActorReentrancyConfig(true, 32);

    assertTrue(config.getEnabled());
    assertEquals(32, (int) config.getMaxStackDepth());
  }

  @Test
  public void TestStackDepthNullable() {
    final ActorReentrancyConfig config = new ActorReentrancyConfig(true, null);

    assertTrue(config.getEnabled());
    assertNull(config.getMaxStackDepth());
  }
}