package io.dapr.actors.it.actors;

import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorProxy;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DefaultObjectSerializer;
import io.dapr.client.domain.Verb;
import org.junit.ClassRule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class ActorActivationClient {
  private static Logger logger = LoggerFactory.getLogger(ActorActivationClient.class);

  public static void main(String[] args) throws Exception {
    try {
      Thread.sleep(30000);
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
      ActorId actorId2 = new ActorId(Integer.toString(atomicInteger.getAndIncrement()));
      ActorProxy proxy2 = proxyBuilder.build(actorId2);
      logger.debug("Waitng for 6 seconds so actor deactives itself");
      Thread.sleep(6000);
      List<String> activeActorsSecondtry = proxy2.invokeActorMethod("retrieveActiveActors", null, List.class).block();
      logger.debug("Active actors: [" + activeActorsSecondtry.toString() + "]");
      assertFalse("NOT Expecting actorId:[" + actorId1.toString() + "]", activeActorsSecondtry.contains(actorId1.toString()));
    } catch (Exception ex) {
      logger.error("Test Failed", ex);
      System.exit(1);
    }
  }
}
