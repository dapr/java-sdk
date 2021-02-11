/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.it.actors;

import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.it.BaseIT;
import io.dapr.it.actors.services.springboot.DemoActor;
import io.dapr.it.actors.services.springboot.DemoActorService;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static io.dapr.it.Retry.callWithRetry;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ActivationDeactivationIT extends BaseIT {

  private static Logger logger = LoggerFactory.getLogger(ActivationDeactivationIT.class);

  @Test
  public void activateInvokeDeactivate() throws Exception {
    // The call below will fail if service cannot start successfully.
    startDaprApp(
        ActivationDeactivationIT.class.getSimpleName(),
        DemoActorService.SUCCESS_MESSAGE,
        DemoActorService.class,
        true,
        60000);

    final AtomicInteger atomicInteger = new AtomicInteger(1);
    logger.debug("Creating proxy builder");
    ActorProxyBuilder<DemoActor> proxyBuilder = new ActorProxyBuilder(DemoActor.class, newActorClient());
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
    assertTrue("Expecting actorId:[" + actorId1.toString() + "]", activeActors.contains(actorId1.toString()));

    ActorId actorId2 = new ActorId(Integer.toString(atomicInteger.getAndIncrement()));
    DemoActor proxy2 = proxyBuilder.build(actorId2);
    callWithRetry(() -> {
      List<String> activeActorsSecondTry = proxy2.retrieveActiveActors();
      logger.debug("Active actors: [" + activeActorsSecondTry.toString() + "]");
      assertFalse("NOT Expecting actorId:[" + actorId1.toString() + "]", activeActorsSecondTry.contains(actorId1.toString()));
    }, 15000);
  }
}
