package io.dapr.it.spring.feign;

import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;

import static io.dapr.it.spring.data.DaprSpringDataConstants.STATE_STORE_NAME;
import static io.dapr.it.testcontainers.DaprContainerConstants.IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = {"dapr.feign.enable=true"})
@Testcontainers
@Tag("testcontainers")
public class DaprFeignIT {
  private static final String CONNECTION_STRING =
      "host=postgres-repository user=postgres password=password port=5432 connect_timeout=10 database=dapr_db_repository";

  private static final Map<String, String> BINDING_PROPERTIES = Map.of("connectionString", CONNECTION_STRING);

  private static final Network DAPR_NETWORK = Network.newNetwork();

  public static final String BINDING_NAME = "postgresbinding";

  @Container
  private static final PostgreSQLContainer<?> POSTGRE_SQL_CONTAINER = new PostgreSQLContainer<>("postgres:16-alpine")
      .withNetworkAliases("postgres-repository")
      .withDatabaseName("dapr_db_repository")
      .withUsername("postgres")
      .withPassword("password")
      .withNetwork(DAPR_NETWORK);

  @Container
  @ServiceConnection
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer(IMAGE_TAG)
      .withAppName("dapr-feign-test")
      .withNetwork(DAPR_NETWORK)
      .withComponent(new Component(BINDING_NAME, "bindings.postgresql", "v1", BINDING_PROPERTIES))
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
      .dependsOn(POSTGRE_SQL_CONTAINER);

  @Autowired
  PostgreBindingClient postgreBindingClient;

  @Autowired
  TestMethodClient testMethodClient;

  @Test
  public void invokeBindingTest() {

  }

  @Test
  public void invokeMethodTest() {
    assertEquals("hello", testMethodClient.hello());
    assertEquals("hello", testMethodClient.echo("hello"));
  }

}
