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
import io.dapr.actors.client.ActorProxy;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.config.Properties;
import io.dapr.it.actors.app.TestApplication;
import io.dapr.it.testcontainers.actors.TestDaprActorsConfiguration;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import io.dapr.testcontainers.internal.DaprContainerFactory;
import io.dapr.testcontainers.internal.DaprSidecarContainer;
import io.dapr.testcontainers.internal.spring.DaprSpringBootTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.dapr.it.Retry.callWithRetry;
import static io.dapr.it.actors.MyActorTestUtils.fetchMethodCallLogs;
import static io.dapr.it.actors.MyActorTestUtils.validateMessageContent;
import static io.dapr.it.actors.MyActorTestUtils.validateMethodCalls;

@DaprSpringBootTest(classes = {
    TestApplication.class,
    TestDaprActorsConfiguration.class,
    MyActorRuntimeRegistrationConfiguration.class
})
public class ActorTimerRecoveryIT {

  private static final Logger logger = LoggerFactory.getLogger(ActorTimerRecoveryIT.class);

  @DaprSidecarContainer
  private static final DaprContainer DAPR_CONTAINER = DaprContainerFactory.createForSpringBootTest("actor-timer-recovery-it")
      .withComponent(new Component("kvstore", "state.in-memory", "v1", Map.of("actorStateStore", "true")))
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withLogConsumer(outputFrame -> logger.info(outputFrame.getUtf8String()));

  private static final String METHOD_NAME = "clock";

  private static final String ACTOR_TYPE = "MyActorTest";

  private ActorProxy proxy;

  @Autowired
  private ActorClient actorClient;

  /**
   * Create an actor, register a timer, validates its content, restarts the sidecar and confirms timer continues.
   * @throws Exception This test is not expected to throw.  Thrown exceptions are bugs.
   */
  @Test
  public void timerRecoveryTest() throws Exception {
    ActorTestBootstrap.exposeHostPortAndWaitForActorType(DAPR_CONTAINER, ACTOR_TYPE);

    ActorClient refreshedActorClient = actorClient;
    try {
      logger.debug("Creating proxy builder");
      ActorProxyBuilder<ActorProxy> proxyBuilder =
          new ActorProxyBuilder(ACTOR_TYPE, ActorProxy.class, refreshedActorClient);
      logger.debug("Creating actorId");
      ActorId actorId = new ActorId(UUID.randomUUID().toString());
      logger.debug("Building proxy");
      proxy = proxyBuilder.build(actorId);
      callWithRetry(() -> proxy.invokeMethod("say", "warm-up", String.class).block(), 60000);

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

      DAPR_CONTAINER.stop();
      DAPR_CONTAINER.start();
      ActorTestBootstrap.exposeHostPortAndWaitForActorType(DAPR_CONTAINER, ACTOR_TYPE);
      refreshedActorClient = new ActorClient(new Properties(Map.of(
          "dapr.http.endpoint", DAPR_CONTAINER.getHttpEndpoint(),
          "dapr.grpc.endpoint", DAPR_CONTAINER.getGrpcEndpoint()
      )));
      proxyBuilder = new ActorProxyBuilder(ACTOR_TYPE, ActorProxy.class, refreshedActorClient);
      proxy = proxyBuilder.build(actorId);
      callWithRetry(() -> proxy.invokeMethod("say", "warm-up", String.class).block(), 60000);

      final List<MethodEntryTracker> newLogs = new ArrayList<>();
      callWithRetry(() -> {
        newLogs.clear();
        newLogs.addAll(fetchMethodCallLogs(proxy));
        validateMethodCalls(newLogs, METHOD_NAME, 3);
        validateMessageContent(newLogs, METHOD_NAME, "ping!");
      }, 15000);

      logger.debug("Calling actor method 'stopTimer' to unregister timer");
      proxy.invokeMethod("stopTimer", "myTimer").block();
    } finally {
      if (refreshedActorClient != actorClient) {
        refreshedActorClient.close();
      }
    }
  }
}
