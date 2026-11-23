/*
 * Copyright 2026 The Dapr Authors
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
import io.dapr.it.actors.services.springboot.StatefulActorService;
import io.dapr.it.containers.BaseContainerIT;
import io.dapr.testcontainers.Configuration;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.Feature;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.dapr.it.Retry.callWithRetry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Actor state TTL against a MySQL state store, with the ActorStateTTL
 * preview feature enabled. Redis does not return the ttlExpireTime of a
 * state key, so the other actor tests stay on Redis and only this test
 * uses MySQL.
 */
public class ActorStateTtlIT extends BaseContainerIT {

  private static Logger logger = LoggerFactory.getLogger(ActorStateTtlIT.class);

  private static DaprContainer dapr;
  private static AppRun app;

  @BeforeAll
  public static void start() throws Exception {
    var pair = startAppAndAttach(
        "actor-state-ttl-it",
        StatefulActorService.class,
        AppRun.AppProtocol.HTTP,
        appPort -> daprBuilder("actor-state-ttl-it")
            .withAppPort(appPort)
            .withAppChannelAddress("host.testcontainers.internal")
            .withComponent(mysqlStateStore(STATE_STORE_NAME))
            .withConfiguration(new Configuration("actor-state-ttl",
                null, null, null, null, null, null, null, null, null,
                List.of(new Feature("ActorStateTTL", true)))));
    dapr = pair.dapr();
    app = pair.app();
    waitForActorsReady(dapr);
  }

  @Test
  public void stateExpiresFromCacheAndStore() throws Exception {
    ActorId actorId = new ActorId(
        String.format("%d", System.currentTimeMillis()));
    String actorType = "StatefulActorTest";
    ActorProxyBuilder<ActorProxy> proxyBuilder =
        new ActorProxyBuilder(actorType, ActorProxy.class, newActorClient(dapr));
    ActorProxy proxy = proxyBuilder.build(actorId);

    // waiting for actor to be activated
    Thread.sleep(5000);

    // The value is readable while its TTL has not elapsed.
    callWithRetry(() -> {
      logger.debug("Invoking writeMessageWithTtl ... ");
      proxy.invokeMethod("writeMessageWithTtl", "expires soon").block();
    }, 5000);

    callWithRetry(() -> {
      logger.debug("Invoking readMessage where data is still cached ... ");
      String result = proxy.invokeMethod("readMessage", String.class).block();
      assertEquals("expires soon", result);
    }, 5000);

    // Once the TTL elapses the cached value is no longer served.
    Thread.sleep(3000);

    callWithRetry(() -> {
      logger.debug("Invoking readMessage after the TTL elapsed ... ");
      String result = proxy.invokeMethod("readMessage", String.class).block();
      assertNull(result);
    }, 5000);

    // The store itself expires the key too: write again, wait out the TTL,
    // then restart the app so the actor reloads from MySQL with a cold cache.
    callWithRetry(() -> {
      logger.debug("Invoking writeMessageWithTtl again ... ");
      proxy.invokeMethod("writeMessageWithTtl", "expires soon").block();
    }, 5000);

    Thread.sleep(3000);

    logger.debug("Restarting app ...");
    restartApp(app);

    // Need new proxy builder because the proxy builder holds the channel.
    proxyBuilder = new ActorProxyBuilder(actorType, ActorProxy.class, newActorClient(dapr));
    ActorProxy newProxy = proxyBuilder.build(actorId);

    // waiting for actor to be activated
    Thread.sleep(2000);

    callWithRetry(() -> {
      logger.debug("Invoking readMessage where data expired in the store ... ");
      String result = newProxy.invokeMethod("readMessage", String.class).block();
      assertNull(result);
    }, 5000);
  }
}
