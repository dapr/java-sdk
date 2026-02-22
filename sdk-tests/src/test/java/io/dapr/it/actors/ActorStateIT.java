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
import io.dapr.actors.client.ActorProxy;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.it.actors.services.springboot.DaprApplication;
import io.dapr.it.actors.services.springboot.StatefulActor;
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

import java.util.Map;

import static io.dapr.it.Retry.callWithRetry;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DaprSpringBootTest(classes = {
    DaprApplication.class,
    TestDaprActorsConfiguration.class,
    StatefulActorRuntimeRegistrationConfiguration.class
})
public class ActorStateIT {

  private static final Logger logger = LoggerFactory.getLogger(ActorStateIT.class);

  @DaprSidecarContainer
  private static final DaprContainer DAPR_CONTAINER = DaprContainerFactory.createForSpringBootTest("actor-state-it")
      .withComponent(new Component("kvstore", "state.in-memory", "v1", Map.of("actorStateStore", "true")))
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withLogConsumer(outputFrame -> logger.info(outputFrame.getUtf8String()));

  @Autowired
  private ActorClient actorClient;

  @BeforeEach
  void setUp() {
    org.testcontainers.Testcontainers.exposeHostPorts(DAPR_CONTAINER.getAppPort());
    DaprWait.forActorType("StatefulActorTest").waitUntilReady(DAPR_CONTAINER);
  }

  @Test
  public void writeReadState() throws Exception {
    String message = "This is a message to be saved and retrieved.";
    String name = "Jon Doe";
    byte[] bytes = new byte[] { 0x1 };
    ActorId actorId = new ActorId(
        String.format("%d", System.currentTimeMillis()));
    String actorType = "StatefulActorTest";
    logger.debug("Building proxy ...");
    ActorProxyBuilder<ActorProxy> proxyBuilder =
        new ActorProxyBuilder(actorType, ActorProxy.class, actorClient);
    ActorProxy proxy = proxyBuilder.build(actorId);

    // Validate conditional read works.
    callWithRetry(() -> {
      logger.debug("Invoking readMessage where data is not present yet ... ");
      String result = proxy.invokeMethod("readMessage", String.class).block();
      assertNull(result);
    }, 10000);

    callWithRetry(() -> {
      logger.debug("Invoking writeMessage ... ");
      proxy.invokeMethod("writeMessage", message).block();
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking readMessage where data is probably still cached ... ");
      String result = proxy.invokeMethod("readMessage", String.class).block();
      assertEquals(message, result);
    }, 5000);

    // writeData uses an object instead of String to test serialization.
    StatefulActor.MyData mydata = new StatefulActor.MyData();
    mydata.value = "My data value.";
    callWithRetry(() -> {
      logger.debug("Invoking writeData with object ... ");
      proxy.invokeMethod("writeData", mydata).block();
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking readData where data is probably still cached ... ");
      StatefulActor.MyData result = proxy.invokeMethod("readData", StatefulActor.MyData.class).block();
      assertEquals(mydata.value, result.value);
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking writeName ... ");
      proxy.invokeMethod("writeName", name).block();
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking readName where data is probably still cached ... ");
      String result = proxy.invokeMethod("readName", String.class).block();
      assertEquals(name, result);
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking writeName with empty content... ");
      proxy.invokeMethod("writeName", "").block();
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking readName where empty content is probably still cached ... ");
      String result = proxy.invokeMethod("readName", String.class).block();
      assertEquals("", result);
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking writeBytes ... ");
      proxy.invokeMethod("writeBytes", bytes).block();
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking readBytes where data is probably still cached ... ");
      byte[] result = proxy.invokeMethod("readBytes", byte[].class).block();
      assertArrayEquals(bytes, result);
    }, 5000);

    // Wait for actor idle timeout + scan interval so cached actor gets deactivated.
    Thread.sleep(10000);

    callWithRetry(() -> {
      logger.debug("Invoking readMessage where data is not cached ... ");
      String result = proxy.invokeMethod("readMessage", String.class).block();
      assertEquals(message, result);
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking readData where data is not cached ... ");
      StatefulActor.MyData result = proxy.invokeMethod("readData", StatefulActor.MyData.class).block();
      assertEquals(mydata.value, result.value);
    }, 5000);
    logger.debug("Finished testing actor string state.");

    callWithRetry(() -> {
      logger.debug("Invoking readName where empty content is not cached ... ");
      String result = proxy.invokeMethod("readName", String.class).block();
      assertEquals("", result);
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking readBytes where content is not cached ... ");
      byte[] result = proxy.invokeMethod("readBytes", byte[].class).block();
      assertArrayEquals(bytes, result);
    }, 5000);
  }
}
