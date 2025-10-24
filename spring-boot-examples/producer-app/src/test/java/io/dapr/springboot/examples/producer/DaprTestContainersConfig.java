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

package io.dapr.springboot.examples.producer;

import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.Subscription;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.dapr.testcontainers.DaprContainerConstants.DAPR_RUNTIME_IMAGE_TAG;

@TestConfiguration(proxyBeanMethods = false)
public class DaprTestContainersConfig {

  static final String CONNECTION_STRING =
          "host=postgres user=postgres password=password port=5432 connect_timeout=10 database=dapr_db_repository";
  static final Map<String, String> STATE_STORE_PROPERTIES = createStateStoreProperties();
  static final Map<String, String> STATE_STORE_OUTBOX_PROPERTIES = createStateStoreOutboxProperties();
  static final Map<String, String> BINDING_PROPERTIES = Collections.singletonMap("connectionString", CONNECTION_STRING);


  @Bean
  public Network getDaprNetwork(Environment env) {
    boolean reuse = env.getProperty("reuse", Boolean.class, false);
    if (reuse) {
      Network defaultDaprNetwork = new Network() {
        @Override
        public String getId() {
          return "dapr-network";
        }

        @Override
        public void close() {

        }

        @Override
        public Statement apply(Statement base, Description description) {
          return null;
        }
      };

      List<com.github.dockerjava.api.model.Network> networks = DockerClientFactory.instance().client().listNetworksCmd()
          .withNameFilter("dapr-network").exec();
      if (networks.isEmpty()) {
        Network.builder().createNetworkCmdModifier(cmd -> cmd.withName("dapr-network")).build().getId();
        return defaultDaprNetwork;
      } else {
        return defaultDaprNetwork;
      }
    } else {
      return Network.newNetwork();
    }
  }


  @Bean
  public RabbitMQContainer rabbitMQContainer(Network daprNetwork, Environment env) {
    boolean reuse = env.getProperty("reuse", Boolean.class, false);
    return new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.7.25-management-alpine"))
            .withExposedPorts(5672)
            .withNetworkAliases("rabbitmq")
            .withReuse(reuse)
            .withNetwork(daprNetwork);

  }

  @Bean
  public PostgreSQLContainer<?> postgreSQLContainer(Network daprNetwork) {
    return new PostgreSQLContainer<>("postgres:16-alpine")
            .withNetworkAliases("postgres")
            .withDatabaseName("dapr_db_repository")
            .withUsername("postgres")
            .withPassword("password")
            .withExposedPorts(5432)
            .withNetwork(daprNetwork);

  }

  @Bean
  @ServiceConnection
  public DaprContainer daprContainer(Network daprNetwork, PostgreSQLContainer<?> postgreSQLContainer, RabbitMQContainer rabbitMQContainer) {

    Map<String, String> rabbitMqProperties = new HashMap<>();
    rabbitMqProperties.put("connectionString", "amqp://guest:guest@rabbitmq:5672");
    rabbitMqProperties.put("user", "guest");
    rabbitMqProperties.put("password", "guest");

    return new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
            .withAppName("producer-app")
            .withNetwork(daprNetwork)
            .withComponent(new Component("kvstore", "state.postgresql", "v1", STATE_STORE_PROPERTIES))
            .withComponent(new Component("kvbinding", "bindings.postgresql", "v1", BINDING_PROPERTIES))
            .withComponent(new Component("pubsub", "pubsub.rabbitmq", "v1", rabbitMqProperties))
            .withComponent(new Component("kvstore-outbox", "state.postgresql", "v1", STATE_STORE_OUTBOX_PROPERTIES))
            .withSubscription(new Subscription("app", "pubsub", "topic", "/subscribe"))
            .withAppPort(8080)
            .withAppHealthCheckPath("/actuator/health")
            .withAppChannelAddress("host.testcontainers.internal")
            .dependsOn(rabbitMQContainer)
            .dependsOn(postgreSQLContainer);
  }


  private static Map<String, String> createStateStoreProperties() {
    Map<String, String> result = new HashMap<>();

    result.put("keyPrefix", "name");
    result.put("actorStateStore", String.valueOf(true));
    result.put("connectionString", CONNECTION_STRING);

    return result;
  }

  private static Map<String, String> createStateStoreOutboxProperties() {
    Map<String, String> result = new HashMap<>();
    result.put("connectionString", CONNECTION_STRING);
    result.put("outboxPublishPubsub", "pubsub");
    result.put("outboxPublishTopic", "outbox-topic");

    return result;
  }


}
