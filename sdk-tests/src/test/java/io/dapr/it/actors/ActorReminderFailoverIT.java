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
 * limitations under the License.
*/

package io.dapr.it.actors;

import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorProxy;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.it.AppRun;
import io.dapr.it.actors.app.MyActorService;
import io.dapr.it.containers.BaseContainerIT;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprPlacementContainer;
import io.dapr.testcontainers.DaprSchedulerContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

import static io.dapr.it.actors.MyActorTestUtils.countMethodCalls;
import static io.dapr.it.actors.MyActorTestUtils.fetchMethodCallLogs;
import static io.dapr.it.actors.MyActorTestUtils.validateMethodCalls;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Verifies that actor reminders fail over to a surviving sidecar when the sidecar
 * currently hosting the actor is killed. Actor failover is keyed on actor type
 * ("MyActorTest"): placement distributes actors of a type across every sidecar that
 * registered that type, and rebalances when one host's app dies. This test starts two
 * actor-hosting app+sidecar pairs plus a third, client-only sidecar (no app) whose
 * ActorClient builds the proxy; all three share one placement, one scheduler, and Redis.
 */
public class ActorReminderFailoverIT extends BaseContainerIT {

  private static final Logger logger = LoggerFactory.getLogger(ActorReminderFailoverIT.class);

  private static final String METHOD_NAME = "receiveReminder";

  private static final String ACTOR_TYPE = "MyActorTest";

  private DaprPlacementContainer placement;

  private DaprSchedulerContainer scheduler;

  private DaprAndApp one;

  private DaprAndApp two;

  private DaprContainer client;

  private ActorProxy proxy;

  @BeforeEach
  public void init() throws Exception {
    placement = startSharedPlacement();
    scheduler = startSharedScheduler();

    one = startAppAndAttach(
        "actor-reminder-failover-it-one",
        MyActorService.class,
        AppRun.AppProtocol.HTTP,
        appPort -> daprBuilder("actor-reminder-failover-it-one")
            .withPlacementContainer(placement)
            .withSchedulerContainer(scheduler)
            .withAppPort(appPort)
            .withAppChannelAddress("host.testcontainers.internal")
            .withComponent(redisStateStore(STATE_STORE_NAME)));

    two = startAppAndAttach(
        "actor-reminder-failover-it-two",
        MyActorService.class,
        AppRun.AppProtocol.HTTP,
        appPort -> daprBuilder("actor-reminder-failover-it-two")
            .withPlacementContainer(placement)
            .withSchedulerContainer(scheduler)
            .withAppPort(appPort)
            .withAppChannelAddress("host.testcontainers.internal")
            .withComponent(redisStateStore(STATE_STORE_NAME)));

    client = daprBuilder("actor-reminder-failover-it-client")
        .withPlacementContainer(placement)
        .withSchedulerContainer(scheduler)
        .withComponent(redisStateStore(STATE_STORE_NAME));
    client.start();
    deferStop(client);

    // Prove both actor hosts have registered MyActorTest with the shared placement
    // service before exercising failover. startAppAndAttach's waitForSidecar only proves
    // daprd's gRPC channel is up, not that actor types are registered -- a fixed sleep
    // alone is a flakiness risk under CI load.
    waitForActorsReady(one.dapr());
    waitForActorsReady(two.dapr());

    Thread.sleep(3000);

    ActorId actorId = new ActorId(UUID.randomUUID().toString());
    logger.debug("Creating proxy builder");

    ActorProxyBuilder<ActorProxy> proxyBuilder =
        new ActorProxyBuilder(ACTOR_TYPE, ActorProxy.class, newActorClient(client));
    logger.debug("Creating actorId");
    logger.debug("Building proxy");
    proxy = proxyBuilder.build(actorId);
  }

  @AfterEach
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

    int originalActorHostIdentifier = Integer.parseInt(
        proxy.invokeMethod("getIdentifier", String.class).block());
    // Stop BOTH the app and its sidecar for the host currently hosting the actor. Killing
    // only the app subprocess leaves daprd connected to placement, so placement never
    // reassigns the actor type away from that host (daprd just keeps retrying the dead
    // app). The legacy harness's DaprRun.stop() ran `dapr stop --app-id`, which tore down
    // the sidecar process along with its supervised app -- this mirrors that.
    if (originalActorHostIdentifier == one.dapr().getHttpPort()) {
      one.app().stop();
      one.dapr().stop();
    }
    if (originalActorHostIdentifier == two.dapr().getHttpPort()) {
      two.app().stop();
      two.dapr().stop();
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
