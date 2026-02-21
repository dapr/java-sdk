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
import io.dapr.actors.client.ActorClient;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.config.Properties;
import io.dapr.it.actors.app.MyActor;
import io.dapr.it.actors.app.TestApplication;
import io.dapr.it.testcontainers.actors.TestDaprActorsConfiguration;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import io.dapr.testcontainers.internal.DaprContainerFactory;
import io.dapr.testcontainers.internal.DaprSidecarContainer;
import io.dapr.testcontainers.internal.spring.DaprSpringBootTest;
import io.dapr.testcontainers.wait.strategy.DaprWait;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static io.dapr.it.Retry.callWithRetry;
import static io.dapr.it.TestUtils.assertThrowsDaprExceptionSubstring;

@DaprSpringBootTest(classes = {
    TestApplication.class,
    TestDaprActorsConfiguration.class,
    MyActorRuntimeRegistrationConfiguration.class
})
public class ActorExceptionIT {

  private static final Logger logger = LoggerFactory.getLogger(ActorExceptionIT.class);

  @DaprSidecarContainer
  private static final DaprContainer DAPR_CONTAINER = DaprContainerFactory.createForSpringBootTest("actor-exception-it")
      .withComponent(new Component("kvstore", "state.in-memory", "v1", Map.of("actorStateStore", "true")))
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withLogConsumer(outputFrame -> logger.info(outputFrame.getUtf8String()));

  @Autowired
  private ActorClient actorClient;

  @BeforeEach
  public void setUp() {
    org.testcontainers.Testcontainers.exposeHostPorts(DAPR_CONTAINER.getAppPort());
    DaprWait.forActors().waitUntilReady(DAPR_CONTAINER);
  }

  private ActorClient newActorClient(Map<String, String> metadata) {
    return new ActorClient(new Properties(Map.of(
        "dapr.http.endpoint", "http://127.0.0.1:" + DAPR_CONTAINER.getHttpPort(),
        "dapr.grpc.endpoint", "127.0.0.1:" + DAPR_CONTAINER.getGrpcPort())), metadata, null);
  }

  @Test
  public void exceptionTest() throws Exception {
    ActorProxyBuilder<MyActor> proxyBuilder =
        new ActorProxyBuilder<>("MyActorTest", MyActor.class, actorClient);
    MyActor proxy = proxyBuilder.build(new ActorId("1"));

    callWithRetry(() -> {
      assertThrowsDaprExceptionSubstring(
          "INTERNAL",
          "INTERNAL: error invoke actor method: error from actor service",
          () ->  proxy.throwException());
    }, 10000);
  }

  @Test
  public void exceptionDueToMetadataTest() throws Exception {
    // Setting this HTTP header via actor metadata will cause the Actor HTTP server to error.
    Map<String, String> metadata = Map.of("Content-Length", "9999");
    try (ActorClient actorClientWithMetadata = newActorClient(metadata)) {
      ActorProxyBuilder<MyActor> proxyBuilderMetadataOverride =
          new ActorProxyBuilder<>("MyActorTest", MyActor.class, actorClientWithMetadata);

      MyActor proxyWithMetadata = proxyBuilderMetadataOverride.build(new ActorId("2"));
      callWithRetry(() -> {
        assertThrowsDaprExceptionSubstring(
            "INTERNAL",
            "ContentLength=9999 with Body length 13",
            () -> proxyWithMetadata.say("hello world"));
      }, 10000);
    }
  }
}
