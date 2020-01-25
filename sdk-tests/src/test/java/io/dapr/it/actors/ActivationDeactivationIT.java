/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.actors;

import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorProxy;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.it.BaseIT;
import io.dapr.it.actors.services.EmptyService;
import io.dapr.it.actors.services.springboot.ActorService;
import io.dapr.serializer.DefaultObjectSerializer;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ActivationDeactivationIT extends BaseIT {

  private static Logger logger = LoggerFactory.getLogger(ActivationDeactivationIT.class);

  @BeforeClass
  public static void init() throws Exception {
    // The call below will fail if service cannot start successfully.
    startDaprApp(
      "actors: established connection to placement service at localhost",
      ActorService.class,
      true,
      30000);
  }

  @Test
  public void activateInvokeDeactivate() throws Exception {
    // The call below will fail if service cannot start successfully.
    startDaprApp("BUILD SUCCESS", EmptyService.class, false, 20000);

    final AtomicInteger atomicInteger = new AtomicInteger(1);
    String actorType = "DemoActorTest";
    DefaultObjectSerializer serializer = new DefaultObjectSerializer();
    logger.debug("Creating proxy builder");
    ActorProxyBuilder proxyBuilder = new ActorProxyBuilder(actorType, serializer);
    logger.debug("Creating actorId");
    ActorId actorId1 = new ActorId(Integer.toString(atomicInteger.getAndIncrement()));
    logger.debug("Building proxy");
    ActorProxy proxy = proxyBuilder.build(actorId1);

    callWithRetry(() -> {
        logger.debug("Invoking Say from Proxy");
        String sayResponse = proxy.invokeActorMethod("say", "message", String.class).block();
        logger.debug("asserting not null response: [" + sayResponse + "]");
        assertNotNull(sayResponse);
      }, 5000);

    logger.debug("Retrieving active Actors");
    List<String> activeActors = proxy.invokeActorMethod("retrieveActiveActors", null, List.class).block();
    logger.debug("Active actors: [" + activeActors.toString() + "]");
    assertTrue("Expecting actorId:[" + actorId1.toString() + "]", activeActors.contains(actorId1.toString()));

    ActorId actorId2 = new ActorId(Integer.toString(atomicInteger.getAndIncrement()));
    ActorProxy proxy2 = proxyBuilder.build(actorId2);
    callWithRetry(() -> {
      List<String> activeActorsSecondTry = proxy2.invokeActorMethod("retrieveActiveActors", null, List.class).block();
      logger.debug("Active actors: [" + activeActorsSecondTry.toString() + "]");
      assertFalse("NOT Expecting actorId:[" + actorId1.toString() + "]", activeActorsSecondTry.contains(actorId1.toString()));
    }, 15000);
  }
}
