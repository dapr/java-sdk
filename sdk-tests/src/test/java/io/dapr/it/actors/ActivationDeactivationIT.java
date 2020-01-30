/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.actors;

import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorProxy;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.it.BaseIT;
import io.dapr.it.actors.services.springboot.DemoActorService;
import io.dapr.it.services.EmptyService;
import io.dapr.serializer.DefaultObjectSerializer;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.dapr.it.Retry.callWithRetry;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ActivationDeactivationIT extends BaseIT {

  private static Logger logger = LoggerFactory.getLogger(ActivationDeactivationIT.class);

  @BeforeClass
  public static void init() throws Exception {
    // The call below will fail if service cannot start successfully.
    startDaprApp(
        ActivationDeactivationIT.class.getSimpleName(),
        DemoActorService.SUCCESS_MESSAGE,
        DemoActorService.class,
        true,
        60000);
  }

  @Test
  public void activateInvokeDeactivate() throws Exception {
    // The call below will fail if service cannot start successfully.
    startDaprApp(
        this.getClass().getSimpleName(),
        EmptyService.SUCCESS_MESSAGE,
        EmptyService.class,
        false,
        20000);
    // TODO: Figure out why this wait is needed to make the actor calls work. Where is the delay coming from?
    Thread.sleep(120000);

    final AtomicInteger atomicInteger = new AtomicInteger(1);
    String actorType = "DemoActorTest";
    logger.debug("Creating proxy builder");
    ActorProxyBuilder proxyBuilder = new ActorProxyBuilder(actorType);
    logger.debug("Creating actorId");
    ActorId actorId1 = new ActorId(Integer.toString(atomicInteger.getAndIncrement()));
    logger.debug("Building proxy");
    ActorProxy proxy = proxyBuilder.build(actorId1);

    callWithRetry(() -> {
      logger.debug("Invoking Say from Proxy");
      String sayResponse = proxy.invokeActorMethod("say", "message", String.class).block();
      logger.debug("asserting not null response: [" + sayResponse + "]");
      assertNotNull(sayResponse);
    }, 60000);

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
