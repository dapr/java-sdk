package io.dapr.spring.boot.cloudconfig;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.ConfigurationItem;
import io.dapr.client.domain.GetConfigurationRequest;
import io.dapr.spring.boot.cloudconfig.config.DaprCloudConfigClientManager;
import io.dapr.spring.boot.cloudconfig.config.MultipleConfig;
import io.dapr.spring.boot.cloudconfig.config.SingleConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {CloudConfigTestApplication.class, MultipleConfig.class, SingleConfig.class})
public class CloudConfigTests {

  static {
    try {
      DaprClient daprClient = Mockito.mock(DaprClient.class);
      Mockito.when(daprClient.waitForSidecar(Mockito.anyInt())).thenReturn(Mono.empty());

      Map<String, String> multiValueProperties = new HashMap<>();
      multiValueProperties.put("multivalue-properties", "dapr.spring.democonfigsecret.multivalue.v1=spring\ndapr.spring.democonfigsecret.multivalue.v2=dapr");
      Mockito.when(daprClient.getSecret(Mockito.eq("democonfig"), Mockito.eq("multivalue-properties"))).thenReturn(Mono.just(multiValueProperties));

      Map<String, String> singleValueProperties = new HashMap<>();
      singleValueProperties.put("dapr.spring.democonfigsecret.singlevalue", "testvalue");
      Mockito.when(daprClient.getSecret(Mockito.eq("democonfig"), Mockito.eq("dapr.spring.democonfigsecret.singlevalue"))).thenReturn(Mono.just(singleValueProperties));

      Map<String, Map<String, String>> bulkProperties = new HashMap<>();
      bulkProperties.put("dapr.spring.democonfigsecret.singlevalue", singleValueProperties);
      Mockito.when(daprClient.getBulkSecret(Mockito.eq("democonfig"))).thenReturn(Mono.just(bulkProperties));

      Map<String, ConfigurationItem> singleValueConfigurationItems = new HashMap<>();
      singleValueConfigurationItems.put("dapr.spring.democonfigconfig.singlevalue", new ConfigurationItem("dapr.spring.democonfigconfig.singlevalue", "testvalue", ""));
      Mockito.when(daprClient.getConfiguration(Mockito.refEq(new GetConfigurationRequest("democonfigconf", List.of("dapr.spring.democonfigconfig.singlevalue")), "metadata"))).thenReturn(Mono.just(singleValueConfigurationItems));

      Map<String, ConfigurationItem> multiValueConfigurationItems = new HashMap<>();
      multiValueConfigurationItems.put("multivalue-yaml", new ConfigurationItem("multivalue-yaml", "dapr:\n  spring:\n    democonfigconfig:\n      multivalue:\n        v3: cloud", ""));
      Mockito.when(daprClient.getConfiguration(Mockito.refEq(new GetConfigurationRequest("democonfigconf", List.of("multivalue-yaml")), "metadata"))).thenReturn(Mono.just(multiValueConfigurationItems));

      ReflectionTestUtils.setField(DaprCloudConfigClientManager.class, "daprClient",
          daprClient);

    }
    catch (Exception ignore) {
      ignore.printStackTrace();
    }
  }

  @Autowired
  MultipleConfig multipleConfig;

  @Autowired
  SingleConfig singleConfig;

  @Test
  public void testSecretStoreConfig() {
    assertEquals("testvalue", singleConfig.getSingleValueSecret());

    assertEquals("spring", multipleConfig.getMultipleSecretConfigV1());
    assertEquals("dapr", multipleConfig.getMultipleSecretConfigV2());
  }

  @Test
  public void testConfigurationConfig() {
    assertEquals("testvalue", singleConfig.getSingleValueConfig());

    assertEquals("cloud", multipleConfig.getMultipleConfigurationConfigV3());
  }
}
