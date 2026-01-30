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

import io.dapr.client.DaprClient;
import io.dapr.spring.data.DaprKeyValueTemplate;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.keyvalue.core.query.KeyValueQuery;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.dapr.it.spring.data.DaprSpringDataConstants.STATE_STORE_NAME;
import static io.dapr.it.spring.data.DaprSpringDataConstants.BINDING_NAME;
import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration tests for {@link MySQLDaprKeyValueTemplateIT}.
 */
@SuppressWarnings("AbbreviationAsWordInName")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestDaprSpringDataConfiguration.class)
@Testcontainers
@Tag("testcontainers")
@Disabled("MySQL is broken in 1.17.0-rc.2, waiting for new dapr version")
public class MySQLDaprKeyValueTemplateIT {
  private static final String STATE_STORE_DSN = "mysql:password@tcp(mysql:3306)/";
  private static final String BINDING_DSN = "mysql:password@tcp(mysql:3306)/dapr_db";
  private static final Map<String, String> STATE_STORE_PROPERTIES = createStateStoreProperties();

  private static final Map<String, String> BINDING_PROPERTIES = Map.of("url", BINDING_DSN);

  private static final Network DAPR_NETWORK = Network.newNetwork();

  private static final WaitStrategy MYSQL_WAIT_STRATEGY = Wait
      .forLogMessage(".*port: 3306  MySQL Community Server \\(GPL\\).*", 1)
      .withStartupTimeout(Duration.of(60, ChronoUnit.SECONDS));

  @Container
  private static final MySQLContainer<?> MY_SQL_CONTAINER = new CustomMySQLContainer<>("mysql:5.7.34")
      .withNetworkAliases("mysql")
      .withDatabaseName("dapr_db")
      .withUsername("mysql")
      .withPassword("password")
      .withNetwork(DAPR_NETWORK)
      .waitingFor(MYSQL_WAIT_STRATEGY);

  @Container
  @ServiceConnection
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
      .withAppName("mysql-dapr-app")
      .withNetwork(DAPR_NETWORK)
      .withComponent(new Component(STATE_STORE_NAME, "state.mysql", "v1", STATE_STORE_PROPERTIES))
      .withComponent(new Component(BINDING_NAME, "bindings.mysql", "v1", BINDING_PROPERTIES))
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
      .dependsOn(MY_SQL_CONTAINER);
  

  private static Map<String, String> createStateStoreProperties() {
    Map<String, String> result = new HashMap<>();

    result.put("keyPrefix", "name");
    result.put("schemaName", "dapr_db");
    result.put("actorStateStore", "true");
    result.put("connectionString", STATE_STORE_DSN);

    return result;
  }

  @Autowired
  private DaprClient daprClient;

  @Autowired
  private DaprKeyValueTemplate keyValueTemplate;

  /**
   * Cleans up the state store after each test.
   */
  @AfterEach
  public void tearDown() {
    var meta = Map.of("sql", "delete from state");

    daprClient.invokeBinding(BINDING_NAME, "exec", null, meta).block();
  }

  @Test
  public void testInsertAndQueryDaprKeyValueTemplate() {
    int itemId = 3;
    TestType savedType = keyValueTemplate.insert(new TestType(itemId, "test"));
    assertThat(savedType).isNotNull();

    Optional<TestType> findById = keyValueTemplate.findById(itemId, TestType.class);
    assertThat(findById.isEmpty()).isFalse();
    assertThat(findById.get()).isEqualTo(savedType);

    KeyValueQuery<String> keyValueQuery = new KeyValueQuery<>("content == 'test'");

    Iterable<TestType> myTypes = keyValueTemplate.find(keyValueQuery, TestType.class);
    assertThat(myTypes.iterator().hasNext()).isTrue();

    TestType item = myTypes.iterator().next();
    assertThat(item.getId()).isEqualTo(Integer.valueOf(itemId));
    assertThat(item.getContent()).isEqualTo("test");

    keyValueQuery = new KeyValueQuery<>("content == 'asd'");

    myTypes = keyValueTemplate.find(keyValueQuery, TestType.class);
    assertThat(!myTypes.iterator().hasNext()).isTrue();
  }

  @Test
  public void testInsertMoreThan10AndQueryDaprKeyValueTemplate() {
    int count = 10;
    List<TestType> items = new ArrayList<>();

    for (int i = 0; i < count; i++) {
      items.add(keyValueTemplate.insert(new TestType(i, "test")));
    }

    KeyValueQuery<String> keyValueQuery = new KeyValueQuery<>("content == 'test'");
    keyValueQuery.setRows(100);
    keyValueQuery.setOffset(0);

    Iterable<TestType> foundItems = keyValueTemplate.find(keyValueQuery, TestType.class);
    assertThat(foundItems.iterator().hasNext()).isTrue();

    int index = 0;

    for (TestType foundItem : foundItems) {
      TestType item = items.get(index);

      assertEquals(item.getId(), foundItem.getId());
      assertEquals(item.getContent(), foundItem.getContent());

      index++;
    }

    assertEquals(index, items.size());
  }

  @Test
  public void testUpdateDaprKeyValueTemplate() {
    int itemId = 2;
    TestType insertedType = keyValueTemplate.insert(new TestType(itemId, "test"));
    assertThat(insertedType).isNotNull();

    TestType updatedType = keyValueTemplate.update(new TestType(itemId, "test2"));
    assertThat(updatedType).isNotNull();
  }

  @Test
  public void testDeleteAllOfDaprKeyValueTemplate() {
    int itemId = 1;
    TestType insertedType = keyValueTemplate.insert(new TestType(itemId, "test"));
    assertThat(insertedType).isNotNull();

    keyValueTemplate.delete(TestType.class);

    Optional<TestType> result = keyValueTemplate.findById(itemId, TestType.class);

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetAllOfDaprKeyValueTemplate() {
    int itemId = 1;
    TestType insertedType = keyValueTemplate.insert(new TestType(itemId, "test"));
    assertThat(insertedType).isNotNull();

    Iterable<TestType> result = keyValueTemplate.findAll(TestType.class);

    assertThat(result.iterator().hasNext()).isTrue();
  }

  @Test
  public void testCountDaprKeyValueTemplate() {
    int itemId = 1;
    TestType insertedType = keyValueTemplate.insert(new TestType(itemId, "test"));
    assertThat(insertedType).isNotNull();

    long result = keyValueTemplate.count(TestType.class);

    assertThat(result).isEqualTo(1);
  }

  @Test
  public void testCountWithQueryDaprKeyValueTemplate() {
    int itemId = 1;
    TestType insertedType = keyValueTemplate.insert(new TestType(itemId, "test"));
    assertThat(insertedType).isNotNull();

    KeyValueQuery<String> keyValueQuery = new KeyValueQuery<>("content == 'test'");
    keyValueQuery.setRows(100);
    keyValueQuery.setOffset(0);

    long result = keyValueTemplate.count(keyValueQuery, TestType.class);

    assertThat(result).isEqualTo(1);
  }

}
