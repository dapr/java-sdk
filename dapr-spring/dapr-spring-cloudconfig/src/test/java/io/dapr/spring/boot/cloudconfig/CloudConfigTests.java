package io.dapr.spring.boot.cloudconfig;

import io.dapr.spring.boot.cloudconfig.config.MultipleConfig;
import io.dapr.spring.boot.cloudconfig.config.SingleConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {CloudConfigTestApplication.class, MultipleConfig.class, SingleConfig.class})
public class CloudConfigTests {

  @Autowired
  MultipleConfig multipleConfig;

  @Autowired
  SingleConfig singleConfig;

  @Test
  public void testSecretStoreConfig() {
    assertEquals("testvalue", singleConfig.getSingleValueSecret());
    assertEquals("config", singleConfig.getMultiValuedSecret());

    assertEquals("spring", multipleConfig.getMultipleSecretConfigV1());
    assertEquals("dapr", multipleConfig.getMultipleSecretConfigV2());

  }

  @Test
  public void testConfigurationConfig() {
    assertEquals("testvalue", singleConfig.getSingleValueConfig());

    assertEquals("cloud", multipleConfig.getMultipleConfigurationConfigV3());
  }
}
