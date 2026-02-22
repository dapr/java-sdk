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
import io.dapr.actors.runtime.DaprClientHttpUtils;
import io.dapr.it.actors.app.TestApplication;
import io.dapr.it.testcontainers.actors.TestDaprActorsConfiguration;
import io.dapr.testcontainers.Component;
import io.dapr.testcontainers.DaprContainer;
import io.dapr.testcontainers.DaprLogLevel;
import io.dapr.testcontainers.internal.DaprContainerFactory;
import io.dapr.testcontainers.internal.DaprSidecarContainer;
import io.dapr.testcontainers.internal.spring.DaprSpringBootTest;
import io.dapr.testcontainers.wait.strategy.DaprWait;
import io.dapr.utils.Version;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static io.dapr.it.Retry.callWithRetry;
import static io.dapr.it.actors.MyActorTestUtils.fetchMethodCallLogs;
import static io.dapr.it.actors.MyActorTestUtils.validateMethodCalls;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DaprSpringBootTest(classes = {
    TestApplication.class,
    TestDaprActorsConfiguration.class,
    MyActorRuntimeRegistrationConfiguration.class
})
public class ActorTurnBasedConcurrencyIT {

  private static final Logger logger = LoggerFactory.getLogger(ActorTurnBasedConcurrencyIT.class);

  @DaprSidecarContainer
  private static final DaprContainer DAPR_CONTAINER = DaprContainerFactory.createForSpringBootTest("actor-turn-based-concurrency-it")
      .withComponent(new Component("kvstore", "state.in-memory", "v1", Map.of("actorStateStore", "true")))
      .withDaprLogLevel(DaprLogLevel.DEBUG)
      .withLogConsumer(outputFrame -> logger.info(outputFrame.getUtf8String()));

  private static final String TIMER_METHOD_NAME = "clock";

  private static final String REMINDER_METHOD_NAME = "receiveReminder";

  private static final String ACTOR_TYPE = "MyActorTest";

  @Autowired
  private ActorClient actorClient;

  private String reminderName;

  private String actorId;

  @BeforeEach
  public void setUp() {
    org.testcontainers.Testcontainers.exposeHostPorts(DAPR_CONTAINER.getAppPort());
    DaprWait.forActorType(ACTOR_TYPE).waitUntilReady(DAPR_CONTAINER);
  }

  @AfterEach
  public void cleanUpTestCase() throws Exception {
    if (actorId == null || reminderName == null) {
      return;
    }

    // Delete the reminder in case the test failed, otherwise it may interfere with future tests since it is persisted.
    var channel = buildManagedChannel();
    try {
      String cleanupActorId = actorId;
      String cleanupReminderName = reminderName;
      callWithRetry(() -> DaprClientHttpUtils.unregisterActorReminder(
          channel, ACTOR_TYPE, cleanupActorId, cleanupReminderName), 10000);
    } finally {
      channel.shutdown();
      actorId = null;
      reminderName = null;
    }
  }

  /**
   * Create an actor, register a timer and a reminder, then invoke additional actor method calls.
   * Validate turn-based concurrency by getting logs from the actor through an actor method, and asserting that:
   *   - "Enter" and "Exit" calls alternate
   *   - An entered actor method is exited before a subsequent actor method call.
   * Unregister the timer and reminder and verify they do not fire subsequently.  Also verify the timer and reminder
   * did fire a reasonable (with delta) number of times when they were registered.
   * @throws Exception This test is not expected to throw.  Thrown exceptions are bugs.
   */
  @Test
  public void invokeOneActorMethodReminderAndTimer() throws Exception {
    String actorType = ACTOR_TYPE;
    actorId = UUID.randomUUID().toString();
    reminderName = UUID.randomUUID().toString();
    logger.debug("Creating proxy builder");

    ActorProxyBuilder<ActorProxy> proxyBuilder =
        new ActorProxyBuilder(actorType, ActorProxy.class, actorClient);
    logger.debug("Creating actorId");
    ActorId actorId1 = new ActorId(actorId);
    logger.debug("Building proxy");
    ActorProxy proxy = proxyBuilder.build(actorId1);

    final AtomicInteger expectedSayMethodInvocations = new AtomicInteger();
    logger.debug("Invoking Say from Proxy");
    String sayResponse = proxy.invokeMethod("say", "message", String.class).block();
    logger.debug("asserting not null response: [" + sayResponse + "]");
    assertNotNull(sayResponse);
    expectedSayMethodInvocations.incrementAndGet();

    logger.debug("Invoking actor method 'startTimer' which will register a timer");
    proxy.invokeMethod("startTimer", "myTimer").block();

    // invoke a bunch of calls in parallel to validate turn-based concurrency
    logger.debug("Invoking an actor method 'say' in parallel");
    List<String> sayMessages = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      sayMessages.add("hello" + i);
    }

    sayMessages.parallelStream().forEach(i -> {
      // the actor method called below should reverse the input
      String msg = "message" + i;
      String reversedString = new StringBuilder(msg).reverse().toString();
      String output = proxy.invokeMethod("say", "message" + i, String.class).block();
      assertEquals(reversedString, output);
      expectedSayMethodInvocations.incrementAndGet();
    });

    logger.debug("Calling method to register reminder named {}", reminderName);
    proxy.invokeMethod("startReminder", reminderName).block();

    logger.debug("Pausing 7 seconds to allow timer and reminders to fire");
    Thread.sleep(7000);

    List<MethodEntryTracker> logs = fetchMethodCallLogs(proxy);
    validateTurnBasedConcurrency(logs);

    validateMethodCalls(logs, TIMER_METHOD_NAME, 2);
    validateMethodCalls(logs, REMINDER_METHOD_NAME, 3);

    // call unregister
    logger.debug("Calling actor method 'stopTimer' to unregister timer");
    proxy.invokeMethod("stopTimer", "myTimer").block();

    logger.debug("Calling actor method 'stopReminder' to unregister reminder");
    proxy.invokeMethod("stopReminder", reminderName).block();
    reminderName = null;

    // make some more actor method calls and sleep a bit to see if the timer fires (it should not)
    sayMessages.parallelStream().forEach(i -> {
      proxy.invokeMethod("say", "message" + i, String.class).block();
      expectedSayMethodInvocations.incrementAndGet();
    });

    logger.debug("Pausing 5 seconds to allow time for timer and reminders to fire if there is a bug.  They should not since we have unregistered them.");
    Thread.sleep(5000);

    // get history again, we don't additional timer/reminder calls
    logs = fetchMethodCallLogs(proxy);
    validateEventNotObserved(logs, "stopTimer", TIMER_METHOD_NAME);
    validateEventNotObserved(logs, "stopReminder", REMINDER_METHOD_NAME);
    validateMethodCalls(logs, "say", expectedSayMethodInvocations.get());

  }

  /**
   * Validate turn-based concurrency enter and exit logging - we should see "Enter" and "Exit" alternate since
   * our app implementation service logs that on actor methods.
   * @param logs logs with info about method entries and exits returned from the app
   */
  void validateTurnBasedConcurrency(List<MethodEntryTracker> logs) {
    if (logs.size() == 0) {
      logger.warn("No logs");
      return;
    }

    String currentMethodName = "";
    for (MethodEntryTracker s : logs) {
      if (s.getIsEnter()) {
        currentMethodName = s.getMethodName();
      } else {
        assertTrue(currentMethodName.equals(s.getMethodName()));
      }
    }

    boolean flag = true;
    for (MethodEntryTracker s : logs) {
      if (s.getIsEnter() == flag) {
        flag = !flag;
      } else {
        String msg = "Error - Enter and Exit should alternate.  Incorrect entry: " + s.toString();
        logger.error(msg);
        Assertions.fail(msg);
      }
    }
  }

  /**
   * Validates that after an event in "startingPointMethodName", the events in "methodNameThatShouldNotAppear" do not appear.
   * This can be used to validate that timers and reminders are stopped.
   *
   * @param logs Call logs from the actor service
   * @param startingPointMethodName The name of the method after which "methodNameThatShouldNotAppear" should not appear
   * @param methodNameThatShouldNotAppear The method which should not appear
   */
  void validateEventNotObserved(List<MethodEntryTracker> logs, String startingPointMethodName, String methodNameThatShouldNotAppear) {
    logger.debug("Validating event {} does not appear after event {}",
        methodNameThatShouldNotAppear, startingPointMethodName);
    int index = -1;
    for (int i = 0; i < logs.size(); i++) {
      if (logs.get(i).getMethodName().equals(startingPointMethodName)) {
        index = i;
        break;
      }
    }

    if (index == -1) {
      throw new RuntimeException("Did not find expected trace for " + startingPointMethodName + " actor method");
    }

    List<MethodEntryTracker> logsAfter = logs.subList(index, logs.size());
    for (MethodEntryTracker m : logsAfter) {
      if (m.getMethodName().equals(methodNameThatShouldNotAppear)) {
        String errorMessage = "Timer method " + methodNameThatShouldNotAppear + " should not have been called after " + startingPointMethodName + ".  Observed at " + m.toString();
        logger.error(errorMessage);
        logger.error("Dumping all logs");
        for (MethodEntryTracker l : logs) {
          logger.error("    {}", l);
        }

        throw new RuntimeException(errorMessage);
      }
    }
  }

  private static ManagedChannel buildManagedChannel() {
    return ManagedChannelBuilder.forAddress("127.0.0.1", DAPR_CONTAINER.getGrpcPort())
        .usePlaintext()
        .userAgent(Version.getSdkVersion())
        .build();
  }
}
