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

package io.dapr.springboot4.examples.consumer;

import io.dapr.client.DaprClient;
import io.dapr.spring.messaging.DaprMessagingTemplate;
import io.dapr.springboot.DaprAutoConfiguration;
import io.dapr.springboot.examples.consumer.Order;
import io.dapr.springboot.examples.consumer.SubscriberRestController;
import io.dapr.testcontainers.DaprContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;


@SpringBootTest(classes = {TestConsumerApplication.class, DaprTestContainersConfig.class,
        ConsumerAppTestConfiguration.class, DaprAutoConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ConsumerAppIT {

  private static final String SUBSCRIPTION_MESSAGE_PATTERN = ".*app is subscribed to the following topics.*";

  @Autowired
  private DaprMessagingTemplate<Order> messagingTemplate;

  @Autowired
  private SubscriberRestController subscriberRestController;

  @Autowired
  private DaprClient daprClient;

  @Autowired
  private DaprContainer daprContainer;

  @LocalServerPort
  private int port;

  private RestTestClient client;

  @BeforeAll
  public static void setup() {
    org.testcontainers.Testcontainers.exposeHostPorts(8081);
  }

  @BeforeEach
  void setUp() {
    client = RestTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .build();
    Wait.forLogMessage(SUBSCRIPTION_MESSAGE_PATTERN, 1).waitUntilReady(daprContainer);
  }


  @Test
  void testMessageConsumer() throws InterruptedException {

    messagingTemplate.send("topic", new Order("abc-123", "the mars volta LP", 1));

    client.get()
        .uri("/events")
        .exchange()
        .expectStatus().isOk();

    await().atMost(Duration.ofSeconds(10))
            .until(subscriberRestController.getAllEvents()::size, equalTo(1));

  }

}
