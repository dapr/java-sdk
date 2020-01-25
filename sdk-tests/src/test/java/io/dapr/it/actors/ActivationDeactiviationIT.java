package io.dapr.it.actors;

import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorProxy;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.actors.it.services.springboot.ActorService;
import io.dapr.actors.it.services.springboot.EmptyService;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import io.dapr.serializer.DefaultObjectSerializer;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ActivationDeactiviationIT extends BaseIT {

  private static Logger logger = LoggerFactory.getLogger(ActivationDeactiviationIT.class);

  private static final AtomicInteger atomicInteger = new AtomicInteger(1);

  private final String BASE_URL = "actors/%s/%s";

  private final DefaultObjectSerializer serializer = new DefaultObjectSerializer();

  private static DaprRun daprActorServiceRun;

  private static DaprRun daprActorClientRun;

  @BeforeClass
  public static void init() throws Exception {
    daprActorServiceRun =
      startDaprApp("dapr initialized", ActorService.class, true,30000);
  }

  @Test
  public void testThatWhenInvokingMethodActorActivatesItselfAndDeactivesIteselfAfterElepsedTime() throws Exception {
    Thread.sleep(20000);
    assertTrue("Service App did not started sucessfully", daprActorServiceRun.hasStarted());
    daprActorClientRun = startDaprApp("BUILD SUCCESS", EmptyService.class,false,20000);

    final AtomicInteger atomicInteger = new AtomicInteger(1);
    String actorType = "DemoActorTest";
    DefaultObjectSerializer serializer = new DefaultObjectSerializer();
    logger.debug("Creating proxy builder");
    ActorProxyBuilder proxyBuilder = new ActorProxyBuilder(actorType, serializer);
    logger.debug("Creating actorId");
    ActorId actorId1 = new ActorId(Integer.toString(atomicInteger.getAndIncrement()));
    logger.debug("Building proxy");
    ActorProxy proxy = proxyBuilder.build(actorId1);

    logger.debug("Invoking Say from Proxy");
    String sayResponse = proxy.invokeActorMethod("say", "message", String.class).block();
    logger.debug("asserting not null response: [" + sayResponse + "]");
    assertNotNull(sayResponse);

    logger.debug("Retrieving active Actors");
    List<String> activeActors = proxy.invokeActorMethod("retrieveActiveActors", null, List.class).block();
    logger.debug("Active actors: [" + activeActors.toString() + "]");
    assertTrue("Expecting actorId:[" + actorId1.toString() + "]", activeActors.contains(actorId1.toString()));

    callWithRetry(() -> {
      ActorId actorId2 = new ActorId(Integer.toString(atomicInteger.getAndIncrement()));
      ActorProxy proxy2 = proxyBuilder.build(actorId2);
      List<String> activeActorsSecondtry = proxy2.invokeActorMethod("retrieveActiveActors", null, List.class).block();
      logger.debug("Active actors: [" + activeActorsSecondtry.toString() + "]");
      assertFalse("NOT Expecting actorId:[" + actorId1.toString() + "]", activeActorsSecondtry.contains(actorId1.toString()));
    }, 15000);
  }
}
