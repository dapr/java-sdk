package io.dapr.it.spring.cloudconfig;

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
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static io.dapr.it.testcontainers.DaprContainerConstants.IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
    "spring.config.import[0]=dapr:secret:" + DaprSecretStoreIT.SECRET_STORE_NAME
        + "/" + DaprSecretStoreIT.SECRET_MULTI_NAME + "?type=doc",
    "spring.config.import[1]=dapr:secret:" + DaprSecretStoreIT.SECRET_STORE_NAME
        + "/" + DaprSecretStoreIT.SECRET_SINGLE_NAME + "?type=value",
    "spring.config.import[2]=dapr:secret:" + DaprSecretStoreIT.SECRET_STORE_NAME_MULTI
        + "?type=value",
    "dapr.cloudconfig.wait-sidecar-enabled=true",
    "dapr.cloudconfig.wait-sidecar-retries=5",
})
@ContextConfiguration(classes = TestDaprCloudConfigConfiguration.class)
@ExtendWith(SpringExtension.class)
@Testcontainers
@Tag("testcontainers")
public class DaprSecretStoreIT {
  public static final String SECRET_STORE_NAME = "democonfig";
  public static final String SECRET_MULTI_NAME = "multivalue-properties";
  public static final String SECRET_SINGLE_NAME = "dapr.spring.demo-config-secret.singlevalue";

  public static final String SECRET_STORE_NAME_MULTI = "democonfigMultivalued";

  private static final Map<String, String> SINGLE_VALUE_PROPERTY = generateSingleValueProperty();
  private static final Map<String, String> MULTI_VALUE_PROPERTY = generateMultiValueProperty();

  private static final Network DAPR_NETWORK = Network.newNetwork();

  @Container
  @ServiceConnection
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer(IMAGE_TAG)
      .withAppName("secret-store-dapr-app")
      .withComponent(new Component(SECRET_STORE_NAME, "secretstores.local.file", "v1", SINGLE_VALUE_PROPERTY))
      .withComponent(new Component(SECRET_STORE_NAME_MULTI, "secretstores.local.file", "v1", MULTI_VALUE_PROPERTY))
      .withNetwork(DAPR_NETWORK)
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
      .withCopyToContainer(Transferable.of(DaprSecretStores.SINGLE_VALUED_SECRET), "/dapr-secrets/singlevalued.json")
      .withCopyToContainer(Transferable.of(DaprSecretStores.MULTI_VALUED_SECRET), "/dapr-secrets/multivalued.json");

  static {
    DAPR_CONTAINER.setPortBindings(List.of("3500:3500", "50001:50001"));
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

  @Value("${dapr.spring.demo-config-secret.singlevalue}")
  String singleValue;

  @Value("${dapr.spring.demo-config-secret.multivalue.v1}")
  String multiV1;

  @Value("${dapr.spring.demo-config-secret.multivalue.v2}")
  String multiV2;

  @Value("${dapr.spring.demo-config-secret.multivalue.v4}")
  String multiV4;

  @Test
  public void testSecretStore() {
    assertEquals("testvalue", singleValue);
    assertEquals("spring", multiV1);
    assertEquals("dapr", multiV2);
    assertEquals("config", multiV4);
  }

}
