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
limitations under the License.
*/

package io.dapr.it.actors;

import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorClient;
import io.dapr.actors.client.ActorProxy;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.it.actors.app.SpringBootGrpcActorService;
import io.dapr.it.containers.BaseContainerIT;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprProtocol;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies Spring Boot can host actors over the gRPC stream without an app port. */
public class SpringBootActorGrpcIT extends BaseContainerIT {

  private static DaprContainer dapr;

  private static ActorClient actorClient;

  @BeforeAll
  public static void start() throws Exception {
    var pair = startActorHostAndAttach(
        "springboot-actor-grpc-it",
        SpringBootGrpcActorService.class,
        () -> daprBuilder("springboot-actor-grpc-it")
            .withAppProtocol(DaprProtocol.GRPC)
            .withComponent(redisStateStore(STATE_STORE_NAME)));
    dapr = pair.dapr();
    actorClient = newActorClient(dapr);
    waitForActorsReady(dapr);
  }

  @Test
  public void invokesActorHostedBySpringBootOverGrpcWithoutAppPort() {
    ActorProxy proxy = new ActorProxyBuilder<>("MyActorTest", ActorProxy.class, actorClient)
        .build(new ActorId(UUID.randomUUID().toString()));

    assertEquals("olleh", proxy.invokeMethod("say", "hello", String.class).block());
  }
}
