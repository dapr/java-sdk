package io.dapr.it.containers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

/**
 * JVM-singleton holder for backing service containers shared across all
 * migrated integration tests. Containers are started lazily on first access
 * and reused for the lifetime of the JVM. With {@code withReuse(true)}, dev
 * machines that opt in via ~/.testcontainers.properties also reuse across
 * JVM runs.
 */
public final class SharedTestInfra {

  private static final String REDIS_NETWORK_ALIAS = "redis";
  private static final String ZIPKIN_NETWORK_ALIAS = "zipkin";

  private static volatile Network network;
  private static volatile GenericContainer<?> redis;
  private static volatile GenericContainer<?> zipkin;

  private SharedTestInfra() {}

  public static synchronized Network network() {
    if (network == null) {
      network = Network.newNetwork();
    }
    return network;
  }

  public static synchronized GenericContainer<?> redis() {
    if (redis == null) {
      redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
          .withNetwork(network())
          .withNetworkAliases(REDIS_NETWORK_ALIAS)
          .withExposedPorts(6379)
          .withReuse(true);
      redis.start();
    }
    return redis;
  }

  public static String redisInternalHost() {
    return REDIS_NETWORK_ALIAS + ":6379";
  }

  // Zipkin accessor added in Task 8.
}
