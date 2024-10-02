/*
 * Copyright 2021 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
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
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.dapr.it.Retry.callWithRetry;
import static io.dapr.it.actors.MyActorTestUtils.fetchMethodCallLogs;
import static io.dapr.it.actors.MyActorTestUtils.validateMessageContent;
import static io.dapr.it.actors.MyActorTestUtils.validateMethodCalls;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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

    ActorProxyBuilder<ActorProxy> proxyBuilder =
        new ActorProxyBuilder(actorType, ActorProxy.class, deferClose(runs.right.newActorClient()));
    logger.debug("Creating actorId");
    ActorId actorId = new ActorId(UUID.randomUUID().toString());
    logger.debug("Building proxy");
    ActorProxy proxy = proxyBuilder.build(actorId);

    logger.debug("Invoking actor method 'startTimer' which will register a timer");
    proxy.invokeMethod("startTimer", "myTimer").block();

    logger.debug("Pausing 7 seconds to allow timer to fire");
    Thread.sleep(7000);

    final List<MethodEntryTracker> logs = new ArrayList<>();
    callWithRetry(() -> {
      logs.clear();
      logs.addAll(fetchMethodCallLogs(proxy));
      validateMethodCalls(logs, METHOD_NAME, 3);
      validateMessageContent(logs, METHOD_NAME, "ping!");
    }, 5000);

    // Restarts app only.
    runs.left.stop();
    // Cannot sleep between app's stop and start since it can trigger unhealthy actor in runtime and lose timers.
    // Timers will survive only if the restart is "quick" and survives the runtime's actor health check.
    // Starting in 1.13, sidecar is more sensitive to an app restart and will not keep actors active for "too long".
    runs.left.start();

    final List<MethodEntryTracker> newLogs = new ArrayList<>();
    callWithRetry(() -> {
      newLogs.clear();
      newLogs.addAll(fetchMethodCallLogs(proxy));
      validateMethodCalls(newLogs, METHOD_NAME, 3);
    }, 10000);

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
