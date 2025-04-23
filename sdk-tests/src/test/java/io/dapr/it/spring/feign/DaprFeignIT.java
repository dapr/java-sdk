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

package io.dapr.it.spring.feign;

import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static io.dapr.testcontainers.DaprContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    classes = {
        DaprFeignTestApplication.class
    },
    properties = {
        "dapr.feign.retries=1",
        "server.port=" + DaprFeignIT.APP_PORT
    }
)
@Testcontainers
@Tag("testcontainers")
public class DaprFeignIT {
  public static final String BINDING_NAME = "postgresbinding";
  private static final String CONNECTION_STRING =
      "host=postgres-repository user=postgres password=password port=5432 connect_timeout=10 database=dapr_db_repository";
  private static final Map<String, String> BINDING_PROPERTIES = Map.of("connectionString", CONNECTION_STRING);
  private static final Network DAPR_NETWORK = Network.newNetwork();
  protected static final int APP_PORT = 8081;
  private static final String SUBSCRIPTION_MESSAGE_PATTERN = ".*App entered healthy status.*";

  @Container
  private static final PostgreSQLContainer<?> POSTGRE_SQL_CONTAINER = new PostgreSQLContainer<>("postgres:16-alpine")
      .withNetworkAliases("postgres-repository")
      .withDatabaseName("dapr_db_repository")
      .withUsername("postgres")
      .withPassword("password")
      .withNetwork(DAPR_NETWORK);

  @Container
  @ServiceConnection
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
      .withAppName("dapr-feign-test")
      .withNetwork(DAPR_NETWORK)
      .withComponent(new Component(BINDING_NAME, "bindings.postgresql", "v1", BINDING_PROPERTIES))
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withAppPort(APP_PORT)
      .withAppHealthCheckPath("/ready")
      .withAppChannelAddress("host.testcontainers.internal")
      .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
      .dependsOn(POSTGRE_SQL_CONTAINER);
  @Autowired
  PostgreBindingClient postgreBindingClient;
  @Autowired
  TestMethodClient testMethodClient;

  @BeforeAll
  public static void beforeAll() {
    org.testcontainers.Testcontainers.exposeHostPorts(APP_PORT);
  }

  @BeforeEach
  public void beforeEach() {
    // Ensure the subscriptions are registered
    Wait.forLogMessage(SUBSCRIPTION_MESSAGE_PATTERN, 1).waitUntilReady(DAPR_CONTAINER);
  }

  @Test
  public void invokeBindingTest() {
    postgreBindingClient.exec("CREATE TABLE \"demodata\" (\n" +
        "\t\"id\" serial NOT NULL UNIQUE,\n" +
        "\t\"name\" varchar(255) NOT NULL,\n" +
        "\tPRIMARY KEY(\"id\")\n" +
        ");", List.of());

    postgreBindingClient.exec("INSERT INTO demodata (id, name) VALUES ($1, $2)", "[1, \"hello\"]");

    assertEquals("[[1,\"hello\"]]", postgreBindingClient.query("SELECT * FROM demodata", List.of()));
  }

  @Test
  public void invokeSimpleGetMethodTest() {
    assertEquals("hello", testMethodClient.hello());
  }

  @Test
  public void invokeSimplePostMethodTest() {
    assertEquals("hello", testMethodClient.echo("hello"));
  }

  @Test
  public void invokeJsonMethodTest() {
    assertEquals("hello", testMethodClient.echoJson("hello").getMessage());
  }

}
