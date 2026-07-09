/*
 * Copyright 2025 The Dapr Authors
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
import io.dapr.actors.client.ActorClient;
import io.dapr.actors.client.ActorProxy;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.it.AppRun;
import io.dapr.it.actors.app.MyActor;
import io.dapr.it.actors.app.MyActorService;
import io.dapr.it.containers.BaseContainerIT;
import io.dapr.testcontainers.DaprContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.dapr.it.Retry.callWithRetry;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ActorMethodNameIT extends BaseContainerIT {

  private static Logger logger = LoggerFactory.getLogger(ActorMethodNameIT.class);

  private static DaprContainer dapr;
  private static AppRun app;
  private static ActorClient actorClient;

  @BeforeAll
  public static void start() throws Exception {
    var pair = startAppAndAttach(
        "actor-method-name-it",
        MyActorService.class,
        AppRun.AppProtocol.HTTP,
        appPort -> {
          DaprContainer d = daprBuilder("actor-method-name-it")
              .withAppPort(appPort)
              .withAppChannelAddress("host.testcontainers.internal")
              .withComponent(redisStateStore(STATE_STORE_NAME));
          return d;
        });
    dapr = pair.dapr();
    app = pair.app();
    actorClient = newActorClient(dapr);
    waitForActorsReady(dapr);
  }

  @Test
  public void actorMethodNameChange() throws Exception {
    logger.debug("Creating proxy builder");
    ActorProxyBuilder<MyActor> proxyBuilder =
        new ActorProxyBuilder("MyActorTest", MyActor.class, actorClient);
    logger.debug("Creating actorId");
    ActorId actorId1 = new ActorId("1");
    logger.debug("Building proxy");
    MyActor proxy = proxyBuilder.build(actorId1);

    callWithRetry(() -> {
      logger.debug("Invoking dotNetMethod from Proxy");
      boolean response = proxy.dotNetMethod();
      logger.debug("asserting true response: [" + response + "]");
      assertTrue(response);
    }, 60000);

    logger.debug("Creating proxy builder 2");
    ActorProxyBuilder<ActorProxy> proxyBuilder2 =
        new ActorProxyBuilder("MyActorTest", ActorProxy.class, actorClient);
    logger.debug("Building proxy 2");
    ActorProxy proxy2 = proxyBuilder2.build(actorId1);

    callWithRetry(() -> {
      logger.debug("Invoking DotNetMethodAsync from Proxy 2");
      boolean response = proxy2.invokeMethod("DotNetMethodAsync", boolean.class).block();
      logger.debug("asserting true response 2: [" + response + "]");
      assertTrue(response);
    }, 60000);
  }
}
