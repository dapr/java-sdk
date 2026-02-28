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

import io.dapr.it.testcontainers.TestContainerNetworks;

import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.dapr.it.spring.data.DaprSpringDataConstants.BINDING_NAME;
import static io.dapr.it.spring.data.DaprSpringDataConstants.STATE_STORE_NAME;
import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link DaprKeyValueRepositoryIT}.
 */

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestDaprSpringDataConfiguration.class)
@Testcontainers
@Tag("testcontainers")
public class DaprKeyValueRepositoryIT {
  private static final String CONNECTION_STRING =
      "host=postgres-repository user=postgres password=password port=5432 connect_timeout=10 database=dapr_db_repository";
  private static final Map<String, String> STATE_STORE_PROPERTIES = createStateStoreProperties();

  private static final Map<String, String> BINDING_PROPERTIES = Map.of("connectionString", CONNECTION_STRING);

  private static final Network DAPR_NETWORK = TestContainerNetworks.DATA_NETWORK;

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
      .withAppName("postgresql-repository-dapr-app")
      .withNetwork(DAPR_NETWORK)
      .withComponent(new Component(STATE_STORE_NAME, "state.postgresql", "v1", STATE_STORE_PROPERTIES))
      .withComponent(new Component(BINDING_NAME, "bindings.postgresql", "v1", BINDING_PROPERTIES))
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
      .dependsOn(POSTGRE_SQL_CONTAINER);

  private static Map<String, String> createStateStoreProperties() {
    Map<String, String> result = new HashMap<>();

    result.put("keyPrefix", "name");
    result.put("actorStateStore", String.valueOf(true));
    result.put("connectionString", CONNECTION_STRING);

    return result;
  }

  @Autowired
  private TestTypeRepository repository;

  @BeforeEach
  public void setUp() {
    repository.deleteAll();
  }

  @Test
  public void testFindById() {
    TestType saved = repository.save(new TestType(3, "test"));
    TestType byId = repository.findById(3).get();

    assertEquals(saved, byId);
  }

  @Test
  public void testExistsById() {
    repository.save(new TestType(3, "test"));

    boolean existsById = repository.existsById(3);
    assertTrue(existsById);

    boolean existsById2 = repository.existsById(4);
    assertFalse(existsById2);
  }

  @Test
  public void testFindAll() {
    repository.save(new TestType(3, "test"));
    repository.save(new TestType(4, "test2"));

    Iterable<TestType> all = repository.findAll();

    assertEquals(2, all.spliterator().getExactSizeIfKnown());
  }

  @Test
  public void testFinUsingQuery() {
    repository.save(new TestType(3, "test"));
    repository.save(new TestType(4, "test2"));

    List<TestType> byContent = repository.findByContent("test2");

    assertEquals(1, byContent.size());
  }

}
