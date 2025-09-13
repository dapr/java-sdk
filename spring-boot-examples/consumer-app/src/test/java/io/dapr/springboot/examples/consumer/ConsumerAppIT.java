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

package io.dapr.springboot.examples.consumer;

import io.dapr.client.DaprClient;
import io.dapr.spring.messaging.DaprMessagingTemplate;
import io.dapr.springboot.DaprAutoConfiguration;
import io.dapr.testcontainers.DaprContainer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.time.Duration;

import static io.restassured.RestAssured.given;
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

  @BeforeAll
  public static void setup() {
    org.testcontainers.Testcontainers.exposeHostPorts(8081);
  }

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "http://localhost:" + 8081;
    Wait.forLogMessage(SUBSCRIPTION_MESSAGE_PATTERN, 1).waitUntilReady(daprContainer);
  }


  @Test
  void testMessageConsumer() throws InterruptedException, IOException {

    messagingTemplate.send("topic", new Order("abc-123", "the mars volta LP", 1));

    given().contentType(ContentType.JSON)
            .when()
            .get("/events")
            .then()
            .statusCode(200);

    await().atMost(Duration.ofSeconds(10))
            .until(subscriberRestController.getAllEvents()::size, equalTo(1));

  }

}
