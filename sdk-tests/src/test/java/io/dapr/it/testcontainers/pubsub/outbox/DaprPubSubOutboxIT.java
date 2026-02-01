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

package io.dapr.it.testcontainers.pubsub.outbox;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.ExecuteStateTransactionRequest;
import io.dapr.client.domain.State;
import io.dapr.client.domain.TransactionalStateOperation;
import io.dapr.it.testcontainers.DaprClientFactory;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import io.dapr.testcontainers.wait.strategy.DaprWait;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    classes = {
        TestPubsubOutboxApplication.class
    }
)
@Testcontainers
@Tag("testcontainers")
public class DaprPubSubOutboxIT {

  private static final Logger LOG = LoggerFactory.getLogger(DaprPubSubOutboxIT.class);
  private static final Network DAPR_NETWORK = Network.newNetwork();
  private static final Random RANDOM = new Random();
  private static final int PORT = RANDOM.nextInt(1000) + 8000;

  private static final String PUBSUB_APP_ID = "pubsub-dapr-app";
  private static final String PUBSUB_NAME = "pubsub";

  // topics
  private static final String TOPIC_PRODUCT_CREATED = "product.created";
  private static final String STATE_STORE_NAME = "kvstore";

  @Container
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
      .withAppName(PUBSUB_APP_ID)
      .withNetwork(DAPR_NETWORK)
      .withComponent(new Component(STATE_STORE_NAME, "state.in-memory", "v1", Map.of(
          "outboxPublishPubsub", PUBSUB_NAME,
          "outboxPublishTopic", TOPIC_PRODUCT_CREATED
      )))
      .withComponent(new Component(PUBSUB_NAME, "pubsub.in-memory", "v1", Collections.emptyMap()))
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withLogConsumer(outputFrame -> LOG.info(outputFrame.getUtf8String()))
      .withAppChannelAddress("host.testcontainers.internal")
      .withAppPort(PORT);

  @Autowired
  private ProductWebhookController productWebhookController;

  /**
   * Expose the Dapr ports to the host.
   *
   * @param registry the dynamic property registry
   */
  @DynamicPropertySource
  static void daprProperties(DynamicPropertyRegistry registry) {
    registry.add("dapr.http.endpoint", DAPR_CONTAINER::getHttpEndpoint);
    registry.add("dapr.grpc.endpoint", DAPR_CONTAINER::getGrpcEndpoint);
    registry.add("server.port", () -> PORT);
  }

  @BeforeAll
  public static void beforeAll(){
    org.testcontainers.Testcontainers.exposeHostPorts(PORT);
  }

  @BeforeEach
  public void beforeEach() {
    DaprWait.forSubscription(PUBSUB_NAME, TOPIC_PRODUCT_CREATED).waitUntilReady(DAPR_CONTAINER);
  }

  @Test
  public void shouldPublishUsingOutbox() throws Exception {
    try (DaprClient client = DaprClientFactory.createDaprClientBuilder(DAPR_CONTAINER).build()) {

      ExecuteStateTransactionRequest transactionRequest = new ExecuteStateTransactionRequest(STATE_STORE_NAME);

      Product pencil = new Product("Pencil", 1.50);
      State<Product> state = new State<>(
          pencil.getId(), pencil, null
      );

      TransactionalStateOperation<Product> operation = new TransactionalStateOperation<>(
          TransactionalStateOperation.OperationType.UPSERT, state
      );

      transactionRequest.setOperations(List.of(operation));

      client.executeStateTransaction(transactionRequest).block();

      Awaitility.await().atMost(Duration.ofSeconds(10))
          .ignoreExceptions()
          .untilAsserted(() -> Assertions.assertThat(productWebhookController.getEventList()).isNotEmpty());
    }
  }

}
