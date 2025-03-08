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

package io.dapr.springboot.examples.cloudconfig;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.redis.testcontainers.RedisContainer;
import io.dapr.spring.boot.cloudconfig.autoconfigure.DaprCloudConfigAutoConfiguration;
import io.dapr.springboot.DaprAutoConfiguration;
import io.dapr.springboot.examples.cloudconfig.config.MultipleConfig;
import io.dapr.springboot.examples.cloudconfig.config.SingleConfig;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {TestCloudConfigApplication.class, DaprTestContainersConfig.class,
        DaprAutoConfiguration.class, DaprCloudConfigAutoConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Testcontainers
@Tag("testcontainers")
class CloudConfigTests {

  public static final String CONFIG_STORE_NAME = "democonfigconf";
  public static final String CONFIG_MULTI_NAME = "multivalue-yaml";
  public static final String CONFIG_SINGLE_NAME = "dapr.spring.demo-config-config.singlevalue";

  public static final String SECRET_STORE_NAME = "democonfig";
  public static final String SECRET_MULTI_NAME = "multivalue-properties";
  public static final String SECRET_SINGLE_NAME = "dapr.spring.demo-config-secret.singlevalue";

  public static final String SECRET_STORE_NAME_MULTI = "democonfigMultivalued";

  private static final Map<String, String> SINGLE_VALUE_PROPERTY = generateSingleValueProperty();
  private static final Map<String, String> MULTI_VALUE_PROPERTY = generateMultiValueProperty();


  private static final Map<String, String> STORE_PROPERTY = generateStoreProperty();

  private static final Network DAPR_NETWORK = Network.newNetwork();

  @Container
  private static final RedisContainer REDIS_CONTAINER = new RedisContainer(
      RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG)) {
    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
      super.containerIsStarted(containerInfo);

      String address = getHost();
      Integer port = getMappedPort(6379);

      Logger logger = LoggerFactory.getLogger(CloudConfigTests.class);
      // Put values using Jedis
      try (Jedis jedis = new Jedis(address, port)) {
        logger.info("Putting Dapr Cloud config to {}:{}", address, port);
        jedis.set(CloudConfigTests.CONFIG_MULTI_NAME, DaprConfigurationStores.YAML_CONFIG);
        jedis.set(CloudConfigTests.CONFIG_SINGLE_NAME, "testvalue");
      }
    }
  }
      .withNetworkAliases("redis")
      .withCommand()
      .withNetwork(DAPR_NETWORK);

  @Container
  @ServiceConnection
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer("daprio/daprd:1.14.4")
      .withAppName("configuration-dapr-app")
      .withNetwork(DAPR_NETWORK)
      .withComponent(new Component(CONFIG_STORE_NAME, "configuration.redis", "v1", STORE_PROPERTY))
      .withComponent(new Component(SECRET_STORE_NAME, "secretstores.local.file", "v1", SINGLE_VALUE_PROPERTY))
      .withComponent(new Component(SECRET_STORE_NAME_MULTI, "secretstores.local.file", "v1", MULTI_VALUE_PROPERTY))
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
      .dependsOn(REDIS_CONTAINER)
      .withCopyToContainer(Transferable.of(DaprSecretStores.SINGLE_VALUED_SECRET), "/dapr-secrets/singlevalued.json")
      .withCopyToContainer(Transferable.of(DaprSecretStores.MULTI_VALUED_SECRET), "/dapr-secrets/multivalued.json");

  static {
    DAPR_CONTAINER.setPortBindings(List.of("3500:3500", "50001:50001"));
  }

  private static Map<String, String> generateStoreProperty() {
    return Map.of("redisHost", "redis:6379",
        "redisPassword", "");
  }

  private static Map<String, String> generateSingleValueProperty() {
    return Map.of("secretsFile", "/dapr-secrets/singlevalued.json",
        "multiValued", "false");
  }

  private static Map<String, String> generateMultiValueProperty() {
    return Map.of("secretsFile", "/dapr-secrets/multivalued.json",
        "nestedSeparator", ".",
        "multiValued", "true");
  }

  @Autowired
  MultipleConfig multipleConfig;

  @Autowired
  SingleConfig singleConfig;

  @DynamicPropertySource
  static void dynamicProperties(DynamicPropertyRegistry registry) {
    registry.add("dapr.client.http-port", DAPR_CONTAINER::getHttpPort);
    registry.add("dapr.client.grpc-port", DAPR_CONTAINER::getGrpcPort);
  }

  @Test
  void testCloudConfig() {
    assertEquals("testvalue", singleConfig.getSingleValueSecret());
    assertEquals("spring", multipleConfig.getMultipleSecretConfigV1());
    assertEquals("dapr", multipleConfig.getMultipleSecretConfigV2());

    assertEquals("testvalue", singleConfig.getSingleValueConfig());

    assertEquals("cloud", multipleConfig.getMultipleConfigurationConfigV3());
  }

}
