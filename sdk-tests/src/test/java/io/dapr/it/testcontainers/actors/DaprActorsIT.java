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

package io.dapr.it.testcontainers.actors;

import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorClient;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.actors.runtime.ActorRuntime;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static io.dapr.it.testcontainers.ContainerConstants.DAPR_RUNTIME_IMAGE_TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        classes = {
                TestActorsApplication.class,
                TestDaprActorsConfiguration.class
        }
)
@Testcontainers
@Tag("testcontainers")
public class DaprActorsIT {
  private static final Network DAPR_NETWORK = Network.newNetwork();
  private static final Random RANDOM = new Random();
  private static final int PORT = RANDOM.nextInt(1000) + 8000;

  private static final String ACTORS_MESSAGE_PATTERN = ".*Actor runtime started.*";

  @Container
  private static final DaprContainer DAPR_CONTAINER = new DaprContainer(DAPR_RUNTIME_IMAGE_TAG)
          .withAppName("actor-dapr-app")
          .withNetwork(DAPR_NETWORK)
          .withComponent(new Component("kvstore", "state.in-memory", "v1",
                  Map.of("actorStateStore", "true")))
          .withDaprLogLevel(DaprLogLevel.DEBUG)
          .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
          .withAppChannelAddress("host.testcontainers.internal")
          .withAppPort(PORT);

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

  @Autowired
  private ActorClient daprActorClient;

  @Autowired
  private ActorRuntime daprActorRuntime;

  @BeforeEach
  public void setUp(){
    org.testcontainers.Testcontainers.exposeHostPorts(PORT);
    daprActorRuntime.registerActor(TestActorImpl.class);

    // Wait for actor runtime to start.
    Wait.forLogMessage(ACTORS_MESSAGE_PATTERN, 1).waitUntilReady(DAPR_CONTAINER);
  }

  @Test
  public void testActors() {
    ActorProxyBuilder<TestActor> builder = new ActorProxyBuilder<>(TestActor.class, daprActorClient);
    ActorId actorId = ActorId.createRandom();
    TestActor actor = builder.build(actorId);

    String message = UUID.randomUUID().toString();

    String echoedMessage = actor.echo(message);

    assertEquals(echoedMessage, message);
  }
}
