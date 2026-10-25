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
import io.dapr.actors.client.ActorClient;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.it.AppRun;
import io.dapr.it.actors.app.MyActorGrpcService;
import io.dapr.it.actors.app.MyActorService;
import io.dapr.it.actors.app.ReentrantActor;
import io.dapr.it.actors.app.ReentrantActorImpl;
import io.dapr.it.containers.BaseContainerIT;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprProtocol;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies that an incoming reentrancy id is preserved for nested actor calls over both host protocols. */
public class ActorReentrancyIT extends BaseContainerIT {

  private static DaprContainer httpDapr;

  private static DaprContainer grpcDapr;

  private static ActorClient httpActorClient;

  private static ActorClient grpcActorClient;

  @BeforeAll
  public static void start() throws Exception {
    var httpPair = startAppAndAttach(
        "actor-reentrancy-http-it",
        MyActorService.class,
        AppRun.AppProtocol.HTTP,
        appPort -> daprBuilder("actor-reentrancy-http-it")
            .withAppPort(appPort)
            .withAppChannelAddress("host.testcontainers.internal")
            .withComponent(redisStateStore(STATE_STORE_NAME)));
    httpDapr = httpPair.dapr();
    httpActorClient = newActorClient(httpDapr);
    waitForActorsReady(httpDapr);

    var grpcPair = startActorHostAndAttach(
        "actor-reentrancy-grpc-it",
        MyActorGrpcService.class,
        () -> daprBuilder("actor-reentrancy-grpc-it")
            .withAppProtocol(DaprProtocol.GRPC)
            .withComponent(redisStateStore(STATE_STORE_NAME)));
    grpcDapr = grpcPair.dapr();
    grpcActorClient = newActorClient(grpcDapr);
    waitForActorsReady(grpcDapr);
  }

  @Test
  public void reentersActorOverHttpHost() {
    assertReentrantCallCompletes(httpActorClient);
  }

  @Test
  public void reentersActorOverGrpcHost() {
    assertReentrantCallCompletes(grpcActorClient);
  }

  private static void assertReentrantCallCompletes(ActorClient actorClient) {
    ActorProxyBuilder<ReentrantActor> builder =
        new ActorProxyBuilder<>(ReentrantActorImpl.TYPE, ReentrantActor.class, actorClient);
    String actorAId = UUID.randomUUID().toString();
    String actorBId = UUID.randomUUID().toString();
    ReentrantActor actorA = builder.build(new ActorId(actorAId));

    assertEquals("reentrancy-ok", actorA.call(actorBId).block());
  }
}
