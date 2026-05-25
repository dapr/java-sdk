package io.dapr.it.containers;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedTestInfraTest {

  @Test
  void networkIsSingleton() {
    Network n1 = SharedTestInfra.network();
    Network n2 = SharedTestInfra.network();
    assertSame(n1, n2);
  }

  @Test
  void redisStartsAndIsReachable() {
    GenericContainer<?> redis = SharedTestInfra.redis();
    assertTrue(redis.isRunning());
    assertNotNull(redis.getMappedPort(6379));
    assertEquals("redis", redis.getNetworkAliases().get(0));
  }

  @Test
  void redisInternalHostFormat() {
    SharedTestInfra.redis();  // ensure started
    assertEquals("redis:6379", SharedTestInfra.redisInternalHost());
  }
}
