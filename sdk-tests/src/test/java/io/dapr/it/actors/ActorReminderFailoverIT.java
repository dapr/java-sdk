/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.it.actors;

import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorProxy;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import io.dapr.it.actors.app.MyActorService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

import static io.dapr.it.actors.MyActorTestUtils.countMethodCalls;
import static io.dapr.it.actors.MyActorTestUtils.fetchMethodCallLogs;
import static io.dapr.it.actors.MyActorTestUtils.validateMethodCalls;
import static org.junit.Assert.assertNotEquals;

public class ActorReminderFailoverIT extends BaseIT {

  private static Logger logger = LoggerFactory.getLogger(ActorReminderFailoverIT.class);

  private static final String METHOD_NAME = "receiveReminder";

  private ActorProxy proxy;

  private DaprRun firstAppRun;

  private DaprRun secondAppRun;

  private DaprRun clientAppRun;

  @Before
  public void init() throws Exception {
    firstAppRun = startDaprApp(
        ActorReminderFailoverIT.class.getSimpleName() + "One",
        "Started MyActorService",
        MyActorService.class,
        true,
        60000);
    secondAppRun = startDaprApp(
        ActorReminderFailoverIT.class.getSimpleName() + "Two",
        "Started MyActorService",
        MyActorService.class,
        true,
        60000);
    clientAppRun = startDaprApp(
        ActorReminderFailoverIT.class.getSimpleName() + "Client",
        60000);

    Thread.sleep(3000);

    ActorId actorId = new ActorId(UUID.randomUUID().toString());
    String actorType="MyActorTest";
    logger.debug("Creating proxy builder");

    ActorProxyBuilder<ActorProxy> proxyBuilder =
        new ActorProxyBuilder(actorType, ActorProxy.class, newActorClient());
    logger.debug("Creating actorId");
    logger.debug("Building proxy");
    proxy = proxyBuilder.build(actorId);
  }

  @After
  public void tearDown() {
    // call unregister
    logger.debug("Calling actor method 'stopReminder' to unregister reminder");
    proxy.invokeMethod("stopReminder", "myReminder").block();
  }

  /**
   * Create an actor, register a reminder, validates its content, restarts the runtime and confirms reminder continues.
   * @throws Exception This test is not expected to throw.  Thrown exceptions are bugs.
   */
  @Test
  public void reminderRecoveryTest() throws Exception {
    clientAppRun.use();

    logger.debug("Invoking actor method 'startReminder' which will register a reminder");
    proxy.invokeMethod("startReminder", "myReminder").block();

    logger.debug("Pausing 7 seconds to allow reminder to fire");
    Thread.sleep(7000);

    List<MethodEntryTracker> logs = fetchMethodCallLogs(proxy);
    validateMethodCalls(logs, METHOD_NAME, 3);

    int originalActorHostIdentifier = Integer.parseInt(
        proxy.invokeMethod("getIdentifier", String.class).block());
    if (originalActorHostIdentifier == firstAppRun.getHttpPort()) {
      firstAppRun.stop();
    }
    if (originalActorHostIdentifier == secondAppRun.getHttpPort()) {
      secondAppRun.stop();
    }

    logger.debug("Pausing 10 seconds to allow failover to take place");
    Thread.sleep(10000);
    List<MethodEntryTracker> newLogs = fetchMethodCallLogs(proxy);
    logger.debug("Pausing 10 seconds to allow reminder to fire a few times");
    Thread.sleep(10000);
    List<MethodEntryTracker> newLogs2 = fetchMethodCallLogs(proxy);
    logger.debug("Check if there has been additional calls");
    validateMethodCalls(newLogs2, METHOD_NAME, countMethodCalls(newLogs, METHOD_NAME) + 4);

    int newActorHostIdentifier = Integer.parseInt(
        proxy.invokeMethod("getIdentifier", String.class).block());
    assertNotEquals(originalActorHostIdentifier, newActorHostIdentifier);
  }

}
