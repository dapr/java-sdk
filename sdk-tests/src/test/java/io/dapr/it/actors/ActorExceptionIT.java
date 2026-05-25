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

import java.util.Map;

import static io.dapr.it.Retry.callWithRetry;
import static io.dapr.it.TestUtils.assertThrowsDaprExceptionSubstring;

public class ActorExceptionIT extends BaseContainerIT {

  private static Logger logger = LoggerFactory.getLogger(ActorExceptionIT.class);

  private static DaprContainer dapr;
  private static AppRun app;
  private static ActorClient actorClient;

  @BeforeAll
  public static void start() throws Exception {
    var pair = startAppAndAttach(
        "actor-exception-it",
        MyActorService.class,
        AppRun.AppProtocol.HTTP,
        appPort -> {
          DaprContainer d = daprBuilder("actor-exception-it")
              .withAppPort(appPort)
              .withAppChannelAddress("host.testcontainers.internal")
              .withComponent(redisStateStore(STATE_STORE_NAME));
          d.start();
          return d;
        });
    dapr = pair.dapr();
    app = pair.app();
    actorClient = newActorClient(dapr);
  }

  @Test
  public void exceptionTest() {
    ActorProxyBuilder<MyActor> proxyBuilder =
        new ActorProxyBuilder("MyActorTest", MyActor.class, actorClient);
    MyActor proxy = proxyBuilder.build(new ActorId("1"));

    callWithRetry(() -> {
      assertThrowsDaprExceptionSubstring(
          "INTERNAL",
          "INTERNAL: error invoke actor method: error from actor service",
          () -> proxy.throwException());
    }, 10000);
  }

  @Test
  public void exceptionDueToMetadataTest() {
    // Setting this HTTP header via actor metadata will cause the Actor HTTP server to error.
    Map<String, String> metadata = Map.of("Content-Length", "9999");
    ActorClient metadataClient = newActorClient(dapr, metadata);
    ActorProxyBuilder<MyActor> proxyBuilderMetadataOverride =
        new ActorProxyBuilder("MyActorTest", MyActor.class, metadataClient);

    MyActor proxyWithMetadata = proxyBuilderMetadataOverride.build(new ActorId("2"));
    callWithRetry(() -> {
      assertThrowsDaprExceptionSubstring(
          "INTERNAL",
          "ContentLength=9999 with Body length 13",
          () -> proxyWithMetadata.say("hello world"));
    }, 10000);
  }
}
