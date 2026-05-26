/*
 * Copyright 2025 The Dapr Authors
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
  private static final String MONGO_NETWORK_ALIAS = "mongo";

  private static volatile Network network;
  private static volatile GenericContainer<?> redis;
  private static volatile GenericContainer<?> zipkin;
  private static volatile GenericContainer<?> mongo;

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

  public static synchronized GenericContainer<?> zipkin() {
    if (zipkin == null) {
      zipkin = new GenericContainer<>(DockerImageName.parse("openzipkin/zipkin:latest"))
          .withNetwork(network())
          .withNetworkAliases(ZIPKIN_NETWORK_ALIAS)
          .withExposedPorts(9411)
          .withReuse(true);
      zipkin.start();
    }
    return zipkin;
  }

  public static String zipkinInternalEndpoint() {
    return "http://" + ZIPKIN_NETWORK_ALIAS + ":9411/api/v2/spans";
  }

  public static synchronized GenericContainer<?> mongo() {
    if (mongo == null) {
      mongo = new GenericContainer<>(DockerImageName.parse("mongo:7"))
          .withNetwork(network())
          .withNetworkAliases(MONGO_NETWORK_ALIAS)
          .withExposedPorts(27017)
          .withReuse(true);
      mongo.start();
    }
    return mongo;
  }

  public static String mongoInternalHost() {
    return MONGO_NETWORK_ALIAS + ":27017";
  }
}
