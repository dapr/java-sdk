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
import io.dapr.exceptions.DaprException;
import io.dapr.it.actors.app.ActorReminderDataParam;
import io.dapr.it.actors.app.MyActorGrpcService;
import io.dapr.it.containers.BaseContainerIT;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprProtocol;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static io.dapr.it.Retry.callWithRetry;
import static io.dapr.it.actors.MyActorTestUtils.fetchMethodCallLogs;
import static io.dapr.it.actors.MyActorTestUtils.validateMessageContent;
import static io.dapr.it.actors.MyActorTestUtils.validateMethodCalls;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hosts actors over the Dapr actor event stream (SubscribeActorEventsAlpha1). The app listens on no port and
 * daprd runs without an app port.
 */
public class ActorGrpcHostIT extends BaseContainerIT {

  private static final String APP_NAME = "actor-grpc-host-it";

  private static DaprContainer dapr;

  private static ActorClient actorClient;

  @BeforeAll
  public static void start() throws Exception {
    var pair = startActorHostAndAttach(
        APP_NAME,
        MyActorGrpcService.class,
        () -> daprBuilder(APP_NAME)
            .withAppProtocol(DaprProtocol.GRPC)
            .withComponent(redisStateStore(STATE_STORE_NAME)));
    dapr = pair.dapr();
    actorClient = newActorClient(dapr);
    waitForActorsReady(dapr);
  }

  public static Stream<Arguments> actorTypes() {
    return Stream.of(
        Arguments.of("MyActorTest"),
        Arguments.of("MyActorBinaryTest"),
        Arguments.of("MyActorObjectTest"));
  }

  public static Stream<Arguments> reminderData() {
    return Stream.of(
        Arguments.of("MyActorTest", new ActorReminderDataParam("36", "String"), "36"),
        Arguments.of("MyActorBinaryTest", new ActorReminderDataParam(new byte[]{0, 1}, "Binary"), "AAE="),
        Arguments.of("MyActorObjectTest",
            new ActorReminderDataParam("{\"name\":\"abc\",\"age\":30}", "Object"), "abc,30"));
  }

  @ParameterizedTest
  @MethodSource("actorTypes")
  public void invokesActorMethod(String actorType) throws Exception {
    ActorProxy proxy = newProxy(actorType);

    callWithRetry(() -> assertEquals("olleh", proxy.invokeMethod("say", "hello", String.class).block()), 30000);
  }

  @ParameterizedTest
  @MethodSource("reminderData")
  public void firesReminder(String actorType, ActorReminderDataParam data, String expectedState) throws Exception {
    ActorProxy proxy = newProxy(actorType);
    String reminderName = UUID.randomUUID().toString();
    proxy.invokeMethod("setReminderData", data).block();
    proxy.invokeMethod("startReminder", reminderName).block();

    try {
      callWithRetry(() -> {
        List<MethodEntryTracker> logs = fetchMethodCallLogs(proxy);
        validateMethodCalls(logs, "receiveReminder", 2);
        validateMessageContent(logs, "receiveReminder", expectedState);
      }, 30000);
    } finally {
      proxy.invokeMethod("stopReminder", reminderName).block();
    }
  }

  @Test
  public void firesTimer() throws Exception {
    ActorProxy proxy = newProxy("MyActorTest");
    String timerName = UUID.randomUUID().toString();
    proxy.invokeMethod("startTimer", timerName).block();

    try {
      callWithRetry(() -> {
        List<MethodEntryTracker> logs = fetchMethodCallLogs(proxy);
        validateMethodCalls(logs, "clock", 2);
        validateMessageContent(logs, "clock", "ping!");
      }, 30000);
    } finally {
      proxy.invokeMethod("stopTimer", timerName).block();
    }
  }

  @Test
  public void unknownMethodFails() {
    ActorProxy proxy = newProxy("MyActorTest");

    DaprException exception = assertThrows(DaprException.class,
        () -> proxy.invokeMethod("doesNotExist").block());
    assertTrue(exception.getMessage().contains("doesNotExist"), exception.getMessage());
  }

  @Test
  public void deactivatesIdleActor() throws Exception {
    ActorProxy proxy = newProxy("MyActorTest");
    String actorId = proxy.getActorId().toString();
    proxy.invokeMethod("say", "hello", String.class).block();

    ActorProxy observer = newProxy("MyActorTest");
    assertTrue(activeActors(observer).contains(actorId));

    // The app sets a 5s idle timeout; daprd scans for idle actors every 30s.
    callWithRetry(() -> assertFalse(activeActors(observer).contains(actorId)), 60000);
  }

  @Test
  public void reconnectsAfterSidecarRestart() throws Exception {
    restartSidecar(dapr);

    ActorProxy proxy = newProxy("MyActorTest");
    callWithRetry(() -> assertEquals("olleh", proxy.invokeMethod("say", "hello", String.class).block()), 60000);
  }

  private static ActorProxy newProxy(String actorType) {
    return new ActorProxyBuilder<>(actorType, ActorProxy.class, actorClient)
        .build(new ActorId(UUID.randomUUID().toString()));
  }

  private static List<String> activeActors(ActorProxy proxy) {
    return proxy.invokeMethod("retrieveActiveActors", ArrayList.class).block();
  }
}
