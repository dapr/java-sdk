package io.dapr.it.spring.cloudconfig;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.redis.testcontainers.RedisContainer;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Map;

import static io.dapr.it.testcontainers.DaprContainerConstants.IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
    "spring.config.import[0]=dapr:config:" + DaprCloudConfigIT.CONFIG_STORE_NAME
        + "/" + DaprCloudConfigIT.CONFIG_MULTI_NAME + "?type=doc&doc-type=yaml",
    "spring.config.import[1]=dapr:config:" + DaprCloudConfigIT.CONFIG_STORE_NAME
        + "/" + DaprCloudConfigIT.CONFIG_SINGLE_NAME + "?type=value",
    "dapr.cloudconfig.wait-sidecar-enabled=true",
    "dapr.cloudconfig.wait-sidecar-retries=5",
})
@ContextConfiguration(classes = TestDaprCloudConfigConfiguration.class)
@ExtendWith(SpringExtension.class)
@Testcontainers
@Tag("testcontainers")
public class DaprCloudConfigIT {
  public static final String CONFIG_STORE_NAME = "democonfigconf";
  public static final String CONFIG_MULTI_NAME = "multivalue-yaml";
  public static final String CONFIG_SINGLE_NAME = "dapr.spring.demo-config-config.singlevalue";

  private static final Map<String, String> STORE_PROPERTY = generateStoreProperty();

  private static final Network DAPR_NETWORK = Network.newNetwork();

  private static final RedisContainer REDIS_CONTAINER = new RedisContainer(
      RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG)) {
    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
      super.containerIsStarted(containerInfo);

      String address = getHost();
      Integer port = getMappedPort(6379);

      Logger logger = LoggerFactory.getLogger(DaprCloudConfigIT.class);
      // Put values using Jedis
      try (Jedis jedis = new Jedis(address, port)) {
        logger.info("Putting Dapr Cloud config to {}:{}", address, port);
        jedis.set(DaprCloudConfigIT.CONFIG_MULTI_NAME, DaprConfigurationStores.YAML_CONFIG);
        jedis.set(DaprCloudConfigIT.CONFIG_SINGLE_NAME, "testvalue");
      }
    }
  }
      .withNetworkAliases("redis")
      .withCommand()
      .withNetwork(DAPR_NETWORK);

  @Container
  @ServiceConnection
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer(IMAGE_TAG)
      .withAppName("configuration-dapr-app")
      .withNetwork(DAPR_NETWORK)
      .withComponent(new Component(CONFIG_STORE_NAME, "configuration.redis", "v1", STORE_PROPERTY))
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
      .dependsOn(REDIS_CONTAINER);

  static {
    DAPR_CONTAINER.setPortBindings(List.of("3500:3500", "50001:50001"));
  }

  private static Map<String, String> generateStoreProperty() {
    return Map.of("redisHost", "redis:6379",
        "redisPassword", "");
  }

  @Value("${dapr.spring.demo-config-config.singlevalue}")
  String valueConfig;

  @Value("${dapr.spring.demo-config-config.multivalue.v3}")
  String yamlConfig;

  @Test
  public void configTest() {
    assertEquals("testvalue", valueConfig);
    assertEquals("cloud", yamlConfig);
  }

}
