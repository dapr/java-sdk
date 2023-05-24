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
import io.dapr.it.actors.app.ActorReminderDataParam;
import io.dapr.it.actors.app.MyActorService;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;

import static io.dapr.it.Retry.callWithRetry;
import static io.dapr.it.actors.MyActorTestUtils.*;

@RunWith(Parameterized.class)
public class ActorReminderRecoveryIT extends BaseIT {

  private static final Logger logger = LoggerFactory.getLogger(ActorReminderRecoveryIT.class);

  private static final String METHOD_NAME = "receiveReminder";

  /**
   * Parameters for this test.
   * Param #1: useGrpc.
   *
   * @return Collection of parameter tuples.
   */
  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
            {
                    "MyActorTest",
                    new ActorReminderDataParam("36", "String"),
                    "36"
            },
            {
                    "MyActorTest",
                    new ActorReminderDataParam("\"my_text\"", "String"),
                    "\"my_text\""
            },
            {
                    "MyActorBinaryTest",
                    new ActorReminderDataParam(new byte[]{0, 1}, "Binary"),
                    "AAE="
            },
            {
                    "MyActorObjectTest",
                    new ActorReminderDataParam("{\"name\":\"abc\",\"age\":30}", "Object"),
                    "abc,30"
            },
    });
  }

  public String actorType;

  public ActorReminderDataParam reminderDataParam;

  public String expectedReminderStateText;

  public String reminderName = UUID.randomUUID().toString();

  private ActorProxy proxy;

  private ImmutablePair<AppRun, DaprRun> runs;

  private DaprRun clientRun;

  public ActorReminderRecoveryIT(
          String actorType,
          ActorReminderDataParam reminderDataParam,
          String expectedReminderStateText) {
    this.actorType = actorType;
    this.reminderDataParam = reminderDataParam;
    this.expectedReminderStateText = expectedReminderStateText;
  }

  @Before
  public void init() throws Exception {
    runs = startSplitDaprAndApp(
        ActorReminderRecoveryIT.class.getSimpleName(),
        "Started MyActorService",
        MyActorService.class,
        true,
        60000);

    // Run that will stay up for integration tests.
    // appId must not contain the appId from the other run, otherwise ITs will not run properly.
    clientRun = startDaprApp("ActorReminderRecoveryTestClient", 5000);
    clientRun.use();

    Thread.sleep(3000);

    ActorId actorId = new ActorId(UUID.randomUUID().toString());
    logger.debug("Creating proxy builder");

    ActorProxyBuilder<ActorProxy> proxyBuilder =
        new ActorProxyBuilder(this.actorType, ActorProxy.class, newActorClient());
    logger.debug("Creating actorId");
    logger.debug("Building proxy");
    proxy = proxyBuilder.build(actorId);
  }

  @After
  public void tearDown() {
    // call unregister
    logger.debug("Calling actor method 'stopReminder' to unregister reminder");
    proxy.invokeMethod("stopReminder", this.reminderName).block();
  }

  /**
   * Create an actor, register a reminder, validates its content, restarts the runtime and confirms reminder continues.
   * @throws Exception This test is not expected to throw.  Thrown exceptions are bugs.
   */
  @Test
  public void reminderRecoveryTest() throws Exception {
    logger.debug("Invoking actor method 'startReminder' which will register a reminder");
    proxy.invokeMethod("setReminderData", this.reminderDataParam).block();

    proxy.invokeMethod("startReminder",  this.reminderName).block();

    logger.debug("Pausing 7 seconds to allow reminder to fire");
    Thread.sleep(7000);

    final List<MethodEntryTracker> logs = new ArrayList<>();
    callWithRetry(() -> {
      logs.clear();
      logs.addAll(fetchMethodCallLogs(proxy));
      validateMethodCalls(logs, METHOD_NAME, 3);
      validateMessageContent(logs, METHOD_NAME, this.expectedReminderStateText);
    }, 5000);

    // Restarts runtime only.
    logger.info("Stopping Dapr sidecar");
    runs.right.stop();

    // Pause a bit to let placements settle.
    logger.info("Pausing 10 seconds to let placements settle.");
    Thread.sleep(Duration.ofSeconds(10).toMillis());

    logger.info("Starting Dapr sidecar");
    runs.right.start();
    logger.info("Dapr sidecar started");

    logger.info("Pausing 7 seconds to allow sidecar to be healthy");
    Thread.sleep(7000);

    callWithRetry(() -> {
      logger.info("Fetching logs for " + METHOD_NAME);
      List<MethodEntryTracker> newLogs = fetchMethodCallLogs(proxy);
      validateMethodCalls(newLogs, METHOD_NAME, 1);
      validateMessageContent(newLogs, METHOD_NAME, this.expectedReminderStateText);

      logger.info("Pausing 10 seconds to allow reminder to fire a few times");
      try {
        Thread.sleep(10000);
      } catch (InterruptedException e) {
        logger.error("Sleep interrupted");
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }

      logger.info("Fetching more logs for " + METHOD_NAME);
      List<MethodEntryTracker> newLogs2 = fetchMethodCallLogs(proxy);
      logger.info("Check if there has been additional calls");
      validateMethodCalls(newLogs2, METHOD_NAME, countMethodCalls(newLogs, METHOD_NAME) + 3);
    }, 60000);
  }

}
