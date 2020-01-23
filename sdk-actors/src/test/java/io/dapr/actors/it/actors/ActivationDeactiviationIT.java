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
import io.dapr.client.DefaultObjectSerializer;
import io.dapr.client.domain.Verb;
import org.junit.*;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

//@Ignore
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
  public void actorActivationAppTest2() throws Exception {
    Thread.sleep(20000);
    assertTrue("Service App did not started sucessfully", daprIntegrationTestingRunner.isAppRanOK());
    clientDaprIntegrationTestingRunner =
        createDaprIntegrationTestingRunner(
            "BUILD SUCCESS",
            ActorActivationClient.class,
            false,
            false,
            true,
            20000,
            true
        );
    clientDaprIntegrationTestingRunner.initializeDapr();
    Thread.sleep(120000);
    boolean clientOK = clientDaprIntegrationTestingRunner.isAppRanOK();
    assertTrue("Client app did complete sucessfully.", clientOK);
  }

  //@Test
  public void getConfigTest() throws InterruptedException {
    String actorType = "DemoActor";
    Thread.sleep(5000);
    ActorId actorId = new ActorId(Integer.toString(atomicInteger.getAndIncrement()));
    ActorProxy proxy = new ActorProxyBuilder(actorType, this.serializer).build(actorId);
    DaprClient client = new DaprClientBuilder(this.serializer).build();

    byte[] daprConfig = client.invokeService(Verb.GET, daprIntegrationTestingRunner.getAppName(), "dapr/config", null, null, byte[].class).block();
    assertNotNull(daprConfig);
    logger.debug("daprConfig: [" + new String (daprConfig) + "]");
    String sayResponse = proxy.invokeActorMethod("say", "message", String.class).block();
    assertNotNull(sayResponse);
    logger.debug("sayResponse: [" + sayResponse + "]");
    String activationService = String.format(BASE_URL, actorType, actorId.toString());
    String retriveActiveActorsMethod = "actor/DemoActor/actives";
    logger.debug("Activating actor: [" + activationService + "]");
//    client.invokeService(Verb.POST, daprIntegrationTestingRunner.getAppName(), activationService, null).block();
    List<String> activeActors = client.invokeService(Verb.GET, daprIntegrationTestingRunner.getAppName(), retriveActiveActorsMethod, null, null, List.class).block();
    assertTrue(activeActors.contains(actorId.toString()));
    int sleepTime = 8000;
    logger.debug("Sleep for " + sleepTime + " milliseconds" );
    Thread.sleep(sleepTime);
    logger.debug("Deactivating actor: [" + activationService + "]");
//    client.invokeService(Verb.DELETE, daprIntegrationTestingRunner.getAppName(), activationService, null).block();
    activeActors = client.invokeService(Verb.GET, daprIntegrationTestingRunner.getAppName(), retriveActiveActorsMethod, null, null, List.class).block();
    assertFalse(activeActors.contains(actorId.toString()));

  }
}
