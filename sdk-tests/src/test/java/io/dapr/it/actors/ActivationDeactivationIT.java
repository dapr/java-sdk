/*
 * Copyright 2021 The Dapr Authors
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

package io.dapr.it.actors;

import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorClient;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.it.actors.services.springboot.DemoActor;
import io.dapr.actors.runtime.ActorRuntime;
import io.dapr.it.actors.services.springboot.DaprApplication;
import io.dapr.it.actors.services.springboot.DemoActorImpl;
import io.dapr.it.testcontainers.actors.TestDaprActorsConfiguration;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import io.dapr.testcontainers.internal.DaprContainerFactory;
import io.dapr.testcontainers.internal.DaprSidecarContainer;
import io.dapr.testcontainers.internal.spring.DaprSpringBootTest;
import io.dapr.testcontainers.wait.strategy.DaprWait;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static io.dapr.it.Retry.callWithRetry;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DaprSpringBootTest(classes = {DaprApplication.class, TestDaprActorsConfiguration.class})
public class ActivationDeactivationIT {

  private static final Logger logger = LoggerFactory.getLogger(ActivationDeactivationIT.class);

  @DaprSidecarContainer
  private static final DaprContainer DAPR_CONTAINER = DaprContainerFactory.createForSpringBootTest("activation-deactivation-it")
      .withComponent(new Component("kvstore", "state.in-memory", "v1", Map.of("actorStateStore", "true")))
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withLogConsumer(outputFrame -> logger.info(outputFrame.getUtf8String()));

  @Autowired
  private ActorClient actorClient;

  @Autowired
  private ActorRuntime actorRuntime;

  @BeforeEach
  void setUp() {
    DemoActorImpl.ACTIVE_ACTOR.clear();
    actorRuntime.getConfig().setActorIdleTimeout(Duration.ofSeconds(5));
    actorRuntime.getConfig().setActorScanInterval(Duration.ofSeconds(2));
    actorRuntime.getConfig().setDrainOngoingCallTimeout(Duration.ofSeconds(10));
    actorRuntime.getConfig().setDrainBalancedActors(true);
    actorRuntime.registerActor(DemoActorImpl.class);
    org.testcontainers.Testcontainers.exposeHostPorts(DAPR_CONTAINER.getAppPort());
    DaprWait.forActors().waitUntilReady(DAPR_CONTAINER);
  }

  @Test
  public void activateInvokeDeactivate() throws Exception {
    final AtomicInteger atomicInteger = new AtomicInteger(1);
    logger.debug("Creating proxy builder");
    ActorProxyBuilder<DemoActor> proxyBuilder = new ActorProxyBuilder<>(DemoActor.class, actorClient);
    logger.debug("Creating actorId");
    ActorId actorId1 = new ActorId(Integer.toString(atomicInteger.getAndIncrement()));
    logger.debug("Building proxy");
    DemoActor proxy = proxyBuilder.build(actorId1);

    callWithRetry(() -> {
      logger.debug("Invoking Say from Proxy");
      String sayResponse = proxy.say("message");
      logger.debug("asserting not null response: [" + sayResponse + "]");
      assertNotNull(sayResponse);
    }, 60000);

    logger.debug("Retrieving active Actors");
    List<String> activeActors = proxy.retrieveActiveActors();
    logger.debug("Active actors: [" + activeActors.toString() + "]");
    assertTrue(activeActors.contains(actorId1.toString()),"Expecting actorId:[" + actorId1.toString() + "]");

    ActorId actorId2 = new ActorId(Integer.toString(atomicInteger.getAndIncrement()));
    DemoActor proxy2 = proxyBuilder.build(actorId2);
    callWithRetry(() -> {
      List<String> activeActorsSecondTry = proxy2.retrieveActiveActors();
      logger.debug("Active actors: [" + activeActorsSecondTry.toString() + "]");
      assertFalse(activeActorsSecondTry.contains(actorId1.toString()), "NOT Expecting actorId:[" + actorId1.toString() + "]");
    }, 15000);
  }
}
