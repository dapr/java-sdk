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
import io.dapr.it.actors.app.ActorReminderDataParam;
import io.dapr.it.actors.app.TestApplication;
import io.dapr.it.testcontainers.actors.TestDaprActorsConfiguration;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import io.dapr.testcontainers.internal.DaprContainerFactory;
import io.dapr.testcontainers.internal.DaprSidecarContainer;
import io.dapr.testcontainers.internal.spring.DaprSpringBootTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static io.dapr.it.Retry.callWithRetry;
import static io.dapr.it.actors.MyActorTestUtils.countMethodCalls;
import static io.dapr.it.actors.MyActorTestUtils.fetchMethodCallLogs;
import static io.dapr.it.actors.MyActorTestUtils.validateMessageContent;
import static io.dapr.it.actors.MyActorTestUtils.validateMethodCalls;

@DaprSpringBootTest(classes = {
    TestApplication.class,
    TestDaprActorsConfiguration.class,
    MyActorRuntimeRegistrationConfiguration.class
})
public class ActorReminderRecoveryIT {

  private static final Logger logger = LoggerFactory.getLogger(ActorReminderRecoveryIT.class);

  @DaprSidecarContainer
  private static final DaprContainer DAPR_CONTAINER = DaprContainerFactory.createForSpringBootTest("actor-reminder-recovery-it")
      .withComponent(new Component("kvstore", "state.in-memory", "v1", Map.of("actorStateStore", "true")))
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withLogConsumer(outputFrame -> logger.info(outputFrame.getUtf8String()));

  private static final String METHOD_NAME = "receiveReminder";

  private ActorProxy proxy;

  private String reminderName;

  private String actorType;

  private ActorId actorId;

  private ActorClient refreshedActorClient;

  /**
   * Parameters for this test.
   * Param #1: useGrpc.
   *
   * @return Collection of parameter tuples.
   */
  public static Stream<Arguments> data() {
    return Stream.of(Arguments.of(
                    "MyActorTest",
                    new ActorReminderDataParam("36", "String"),
                    "36"
            ),
            Arguments.of(
                    "MyActorTest",
                    new ActorReminderDataParam("\"my_text\"", "String"),
                    "\"my_text\""
            ),
            Arguments.of(
                    "MyActorBinaryTest",
                    new ActorReminderDataParam(new byte[]{0, 1}, "Binary"),
                    "AAE="
            ),
            Arguments.of(
                    "MyActorObjectTest",
                    new ActorReminderDataParam("{\"name\":\"abc\",\"age\":30}", "Object"),
                    "abc,30"
            )
    );
  }

  public void setup(String actorType) {
    ActorTestBootstrap.exposeHostPortAndWaitForActorType(DAPR_CONTAINER, actorType);

    this.actorType = actorType;
    this.actorId = new ActorId(UUID.randomUUID().toString());
    reminderName = UUID.randomUUID().toString();
    closeRefreshedActorClient();
    this.refreshedActorClient = newActorClientFromContainer();
    rebuildProxy();
  }

  @AfterEach
  public void tearDown() {
    if (proxy == null || reminderName == null) {
      return;
    }

    try {
      logger.debug("Calling actor method 'stopReminder' to unregister reminder");
      proxy.invokeMethod("stopReminder", reminderName).block();
    } catch (Exception e) {
      logger.warn("Reminder cleanup failed after sidecar lifecycle changes: {}", e.getMessage());
    } finally {
      closeRefreshedActorClient();
    }
  }

  /**
   * Create an actor, register a reminder, validates its content, restarts the runtime and confirms reminder continues.
   * @throws Exception This test is not expected to throw.  Thrown exceptions are bugs.
   */
  @ParameterizedTest
  @MethodSource("data")
  public void reminderRecoveryTest(
          String actorType,
          ActorReminderDataParam reminderDataParam,
          String expectedReminderStateText
  ) throws Exception {
    setup(actorType);

    logger.debug("Invoking actor method 'startReminder' which will register a reminder");
    proxy.invokeMethod("setReminderData", reminderDataParam).block();

    proxy.invokeMethod("startReminder", reminderName).block();

    logger.debug("Pausing 7 seconds to allow reminder to fire");
    Thread.sleep(7000);

    final List<MethodEntryTracker> logs = new ArrayList<>();
    callWithRetry(() -> {
      logs.clear();
      logs.addAll(fetchMethodCallLogs(proxy));
      validateMethodCalls(logs, METHOD_NAME, 3);
      validateMessageContent(logs, METHOD_NAME, expectedReminderStateText);
    }, 5000);

    callWithRetry(() -> {
      logger.info("Fetching logs for {}", METHOD_NAME);
      List<MethodEntryTracker> newLogs = fetchMethodCallLogs(proxy);
      validateMethodCalls(newLogs, METHOD_NAME, 3);
      validateMessageContent(newLogs, METHOD_NAME, expectedReminderStateText);

      logger.info("Pausing 10 seconds to allow reminder to fire a few times");
      try {
        Thread.sleep(10000);
      } catch (InterruptedException e) {
        logger.error("Sleep interrupted", e);
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }

      logger.info("Fetching more logs for {}", METHOD_NAME);
      List<MethodEntryTracker> newLogs2 = fetchMethodCallLogs(proxy);
      logger.info("Check if there has been additional calls");
      validateMethodCalls(newLogs2, METHOD_NAME, countMethodCalls(newLogs, METHOD_NAME) + 4);
    }, 60000);
  }

  private void rebuildProxy() {
    ActorProxyBuilder<ActorProxy> proxyBuilder = new ActorProxyBuilder(actorType, ActorProxy.class, refreshedActorClient);
    logger.debug("Building proxy");
    proxy = proxyBuilder.build(actorId);
  }

  private void closeRefreshedActorClient() {
    if (refreshedActorClient != null) {
      refreshedActorClient.close();
      refreshedActorClient = null;
    }
  }

  private ActorClient newActorClientFromContainer() {
    return new ActorClient(new Properties(Map.of(
        "dapr.http.endpoint", DAPR_CONTAINER.getHttpEndpoint(),
        "dapr.grpc.endpoint", DAPR_CONTAINER.getGrpcEndpoint()
    )));
  }
}
