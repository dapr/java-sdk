package io.dapr.actors.it.actors;

import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorProxy;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.actors.it.BaseIT;
import io.dapr.actors.it.DaprIntegrationTestingRunner;
import io.dapr.actors.it.services.springboot.ActorService;
import io.dapr.actors.it.services.springboot.EmptyService;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.Verb;
import io.dapr.serializer.DefaultObjectSerializer;
import org.junit.*;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ActivationDeactiviationIT extends BaseIT {

  private static Logger logger = LoggerFactory.getLogger(ActivationDeactiviationIT.class);

  private static final AtomicInteger atomicInteger = new AtomicInteger(1);

  private final String BASE_URL = "actors/%s/%s";

  private final DefaultObjectSerializer serializer = new DefaultObjectSerializer();

  private DaprIntegrationTestingRunner clientDaprIntegrationTestingRunner;

  @Rule
  public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @After
  public void cleanUpTestCase() {
    Optional.ofNullable(clientDaprIntegrationTestingRunner).ifPresent(daprRunner ->  daprRunner.destroyDapr());
  }

  @BeforeClass
  public static void init() throws Exception {
    daprIntegrationTestingRunner =
        createDaprIntegrationTestingRunner(
            "dapr initialized",
            ActorService.class,
            true,
            true,
            true,
            30000,
            false
        );
    daprIntegrationTestingRunner.initializeDapr();
  }

  @Test
  public void testThatWhenInvokingMethodActorActivatesItselfAndDeactivesIteselfAfterElepsedTime() throws Exception {
    Thread.sleep(20000);
    assertTrue("Service App did not started sucessfully", daprIntegrationTestingRunner.isAppRanOK());
    clientDaprIntegrationTestingRunner =
        createDaprIntegrationTestingRunner(
            "BUILD SUCCESS",
            EmptyService.class,
            false,
            false,
            true,
            20000,
            true
        );
    clientDaprIntegrationTestingRunner.initializeDapr();
    environmentVariables.set("DAPR_HTTP_PORT", String.valueOf(clientDaprIntegrationTestingRunner.DAPR_FREEPORTS.getHttpPort()));
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

    logger.debug("Waitng for 15 seconds so actor deactives itself");
    Thread.sleep(15000);

    ActorId actorId2 = new ActorId(Integer.toString(atomicInteger.getAndIncrement()));
    ActorProxy proxy2 = proxyBuilder.build(actorId2);
    List<String> activeActorsSecondtry = proxy2.invokeActorMethod("retrieveActiveActors", null, List.class).block();
    logger.debug("Active actors: [" + activeActorsSecondtry.toString() + "]");
    assertFalse("NOT Expecting actorId:[" + actorId1.toString() + "]", activeActorsSecondtry.contains(actorId1.toString()));
  }
}
