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

package io.dapr.it.spring.messaging;

import io.dapr.client.domain.CloudEvent;
import io.dapr.spring.boot.autoconfigure.client.DaprClientAutoConfiguration;
import io.dapr.spring.messaging.DaprMessagingTemplate;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import io.dapr.testcontainers.wait.strategy.DaprWait;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.List;

import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    webEnvironment = WebEnvironment.DEFINED_PORT,
    classes = {
        DaprClientAutoConfiguration.class,
        TestApplication.class
    },
    properties = {"dapr.pubsub.name=pubsub"}
)
@Testcontainers
@Tag("testcontainers")
public class DaprSpringMessagingIT {

  private static final Logger logger = LoggerFactory.getLogger(DaprSpringMessagingIT.class);

  private static final String PUBSUB_NAME = "pubsub";
  private static final String TOPIC = "mockTopic";
  private static final Network DAPR_NETWORK = io.dapr.it.testcontainers.TestContainerNetworks.SHARED_NETWORK;
  private static final int APP_PORT = 8080;

  @Container
  @ServiceConnection
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
      .withAppName("messaging-dapr-app")
      .withNetwork(DAPR_NETWORK)
      .withComponent(new Component("pubsub", "pubsub.in-memory", "v1", Collections.emptyMap()))
      .withAppPort(APP_PORT)
      .withAppHealthCheckPath("/ready")
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
      .withAppChannelAddress("host.testcontainers.internal");

  @Autowired
  private DaprMessagingTemplate<String> messagingTemplate;

  @Autowired
  private TestRestController testRestController;

  @BeforeAll
  public static void beforeAll(){
    org.testcontainers.Testcontainers.exposeHostPorts(APP_PORT);
  }

  @BeforeEach
  public void beforeEach() {
    DaprWait.forSubscription(PUBSUB_NAME, TOPIC).waitUntilReady(DAPR_CONTAINER);
  }

  @Test
  public void testDaprMessagingTemplate() throws InterruptedException {
    for (int i = 0; i < 10; i++) {
      var msg = "ProduceAndReadWithPrimitiveMessageType:" + i;

      messagingTemplate.send(TOPIC, msg);

      logger.info("++++++PRODUCE {}------", msg);
    }

    // Wait for the messages to arrive
    Thread.sleep(1000);

    List<CloudEvent<String>> events = testRestController.getEvents();

    assertThat(events.size()).isEqualTo(10);
  }
}
