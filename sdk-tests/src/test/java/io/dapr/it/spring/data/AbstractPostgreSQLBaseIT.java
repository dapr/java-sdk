/*
 * Copyright 2024 The Dapr Authors
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

package io.dapr.it.spring.data;

import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.dapr.it.spring.data.DaprSpringDataConstants.BINDING_NAME;
import static io.dapr.it.spring.data.DaprSpringDataConstants.PUBSUB_NAME;
import static io.dapr.it.spring.data.DaprSpringDataConstants.STATE_STORE_NAME;

@SuppressWarnings("AbbreviationAsWordInName")
@Testcontainers
public abstract class AbstractPostgreSQLBaseIT {
  private static final String CONNECTION_STRING =
      "host=postgres user=postgres password=password port=5432 connect_timeout=10 database=dapr_db";
  private static final Map<String, String> STATE_STORE_PROPERTIES = createStateStoreProperties();

  private static final Map<String, String> BINDING_PROPERTIES = Collections.singletonMap("connectionString", CONNECTION_STRING);

  private static final Network DAPR_NETWORK = Network.newNetwork();

  @Container
  private static final PostgreSQLContainer<?> POSTGRE_SQL_CONTAINER = new PostgreSQLContainer<>("postgres:16-alpine")
      .withNetworkAliases("postgres")
      .withDatabaseName("dapr_db")
      .withUsername("postgres")
      .withPassword("password")
      .withExposedPorts(5432)
      .withNetwork(DAPR_NETWORK);

  @Container
  public static final DaprContainer DAPR_CONTAINER = new DaprContainer("daprio/daprd:1.13.2")
      .withAppName("local-dapr-app")
      .withNetwork(DAPR_NETWORK)
      .withComponent(new Component(STATE_STORE_NAME, "state.postgresql", "v1", STATE_STORE_PROPERTIES))
      .withComponent(new Component(BINDING_NAME, "bindings.postgresql", "v1", BINDING_PROPERTIES))
      .withComponent(new Component(PUBSUB_NAME, "pubsub.in-memory", "v1", Collections.emptyMap()))
      .withAppPort(8080)
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
      .withAppChannelAddress("host.testcontainers.internal")
      .dependsOn(POSTGRE_SQL_CONTAINER);

  @BeforeAll
  static void beforeAll() {
    org.testcontainers.Testcontainers.exposeHostPorts(8080);
  }

  private static Map<String, String> createStateStoreProperties() {
    Map<String, String> result = new HashMap<>();

    result.put("keyPrefix", "name");
    result.put("actorStateStore", String.valueOf(true));
    result.put("connectionString", CONNECTION_STRING);

    return result;
  }

}
