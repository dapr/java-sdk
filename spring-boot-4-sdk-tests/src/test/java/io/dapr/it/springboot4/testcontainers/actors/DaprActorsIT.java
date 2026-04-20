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

package io.dapr.it.springboot4.testcontainers.actors;

import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorClient;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.actors.runtime.ActorRuntime;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import io.dapr.it.springboot4.testcontainers.DaprContainerFactory;
import io.dapr.it.springboot4.testcontainers.DaprSidecarContainer;
import io.dapr.it.springboot4.testcontainers.spring.DaprSpringBootTest;
import io.dapr.testcontainers.wait.strategy.DaprWait;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DaprSpringBootTest(classes = {TestActorsApplication.class, TestDaprActorsConfiguration.class})
@Tag("testcontainers")
public class DaprActorsIT {

  @DaprSidecarContainer
  private static final DaprContainer DAPR_CONTAINER = DaprContainerFactory.createForSpringBootTest("actor-dapr-app")
      .withComponent(new Component("kvstore", "state.in-memory", "v1",
          Map.of("actorStateStore", "true")))
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()));

  @Autowired
  private ActorClient daprActorClient;

  @Autowired
  private ActorRuntime daprActorRuntime;

  @BeforeEach
  public void setUp() {
    org.testcontainers.Testcontainers.exposeHostPorts(DAPR_CONTAINER.getAppPort());
    daprActorRuntime.registerActor(TestActorImpl.class);
    DaprWait.forActors().waitUntilReady(DAPR_CONTAINER);
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
