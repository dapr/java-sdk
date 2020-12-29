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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

import static io.dapr.it.actors.MyActorTestUtils.fetchMethodCallLogs;
import static io.dapr.it.actors.MyActorTestUtils.validateMethodCalls;
import static org.junit.Assert.assertNotEquals;

public class ActorTimerRecoveryIT extends BaseIT {

  private static final Logger logger = LoggerFactory.getLogger(ActorTimerRecoveryIT.class);

  private static final String METHOD_NAME = "clock";

  /**
   * Create an actor, register a timer, validates its content, restarts the Actor and confirms timer continues.
   * @throws Exception This test is not expected to throw.  Thrown exceptions are bugs.
   */
  @Test
  public void timerRecoveryTest() throws Exception {
    ImmutablePair<AppRun, DaprRun> runs = startSplitDaprAndApp(
      ActorTimerRecoveryIT.class.getSimpleName(),
      "Started MyActorService",
      MyActorService.class,
      true,
      60000);

    Thread.sleep(3000);
    String actorType="MyActorTest";
    logger.debug("Creating proxy builder");

    ActorProxyBuilder<ActorProxy> proxyBuilder = deferClose(new ActorProxyBuilder(actorType, ActorProxy.class));
    logger.debug("Creating actorId");
    ActorId actorId = new ActorId(UUID.randomUUID().toString());
    logger.debug("Building proxy");
    ActorProxy proxy = proxyBuilder.build(actorId);

    logger.debug("Invoking actor method 'startTimer' which will register a timer");
    proxy.invokeMethod("startTimer", "myTimer").block();

    logger.debug("Pausing 7 seconds to allow timer to fire");
    Thread.sleep(7000);

    List<MethodEntryTracker> logs = fetchMethodCallLogs(proxy);
    validateMethodCalls(logs, METHOD_NAME, 3);

    // Restarts app only.
    runs.left.stop();
    runs.left.start();

    logger.debug("Pausing 10 seconds to allow timer to fire");
    Thread.sleep(10000);
    List<MethodEntryTracker> newLogs = fetchMethodCallLogs(proxy);
    validateMethodCalls(newLogs, METHOD_NAME, 3);

    // Check that the restart actually happened by confirming the old logs are not in the new logs.
    for (MethodEntryTracker oldLog: logs) {
      for (MethodEntryTracker newLog: newLogs) {
        assertNotEquals(oldLog.toString(), newLog.toString());
      }
    }

    // call unregister
    logger.debug("Calling actor method 'stopTimer' to unregister timer");
    proxy.invokeMethod("stopTimer", "myTimer").block();
  }

}
