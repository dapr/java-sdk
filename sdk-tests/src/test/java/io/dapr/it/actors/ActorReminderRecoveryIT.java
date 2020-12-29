/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.actors;

import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorProxy;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.it.AppRun;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import io.dapr.it.actors.app.MyActorService;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

import static io.dapr.it.actors.MyActorTestUtils.*;

public class ActorReminderRecoveryIT extends BaseIT {

  private static final Logger logger = LoggerFactory.getLogger(ActorReminderRecoveryIT.class);

  private static final String METHOD_NAME = "receiveReminder";

  private ActorProxy proxy;

  private ImmutablePair<AppRun, DaprRun> runs;

  @Before
  public void init() throws Exception {
    runs = startSplitDaprAndApp(
        ActorReminderRecoveryIT.class.getSimpleName(),
        "Started MyActorService",
        MyActorService.class,
        true,
        60000);

    Thread.sleep(3000);

    ActorId actorId = new ActorId(UUID.randomUUID().toString());
    String actorType="MyActorTest";
    logger.debug("Creating proxy builder");

    ActorProxyBuilder<ActorProxy> proxyBuilder = deferClose(new ActorProxyBuilder(actorType, ActorProxy.class));
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
    logger.debug("Invoking actor method 'startReminder' which will register a reminder");
    proxy.invokeMethod("startReminder", "myReminder").block();

    logger.debug("Pausing 7 seconds to allow reminder to fire");
    Thread.sleep(7000);

    List<MethodEntryTracker> logs = fetchMethodCallLogs(proxy);
    validateMethodCalls(logs, METHOD_NAME, 3);

    // Restarts runtime only.
    logger.info("Stopping Dapr sidecar");
    runs.right.stop();
    logger.info("Starting Dapr sidecar");
    runs.right.start();
    logger.info("Dapr sidecar started");

    logger.debug("Pausing 7 seconds to allow sidecar to be healthy");
    Thread.sleep(7000);
    List<MethodEntryTracker> newLogs = fetchMethodCallLogs(proxy);
    logger.debug("Pausing 10 seconds to allow reminder to fire a few times");
    Thread.sleep(10000);
    List<MethodEntryTracker> newLogs2 = fetchMethodCallLogs(proxy);
    logger.debug("Check if there has been additional calls");
    validateMethodCalls(newLogs2, METHOD_NAME, countMethodCalls(newLogs, METHOD_NAME) + 3);
  }

}
