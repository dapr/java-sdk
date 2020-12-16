/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.actors;

import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorProxy;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.actors.runtime.DaprClientHttpUtils;
import io.dapr.client.DaprHttp;
import io.dapr.client.DaprHttpBuilder;
import io.dapr.it.BaseIT;
import io.dapr.it.actors.app.MyActorService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.dapr.it.Retry.callWithRetry;
import static io.dapr.it.actors.MyActorTestUtils.fetchMethodCallLogs;
import static io.dapr.it.actors.MyActorTestUtils.validateMethodCalls;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ActorTurnBasedConcurrencyIT extends BaseIT {

  private static final Logger logger = LoggerFactory.getLogger(ActorTurnBasedConcurrencyIT.class);

  private static final String TIMER_METHOD_NAME = "clock";

  private static final String REMINDER_METHOD_NAME = "receiveReminder";

  private static final String ACTOR_TYPE = "MyActorTest";

  private static final String REMINDER_NAME = UUID.randomUUID().toString();

  private static final String ACTOR_ID = "1";

  @After
  public void cleanUpTestCase() {
    // Delete the reminder in case the test failed, otherwise it may interfere with future tests since it is persisted.
    DaprHttp client = new DaprHttpBuilder().build();
    System.out.println("Invoking during cleanup");
    try {
      DaprClientHttpUtils.unregisterActorReminder(client, ACTOR_TYPE, ACTOR_ID, REMINDER_NAME);
    } catch(Exception e) {
      e.printStackTrace();
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
  public void actorTest1() throws Exception {
    System.out.println("Starting test 'actorTest1'");

    startDaprApp(
      ActorTurnBasedConcurrencyIT.class.getSimpleName(),
      MyActorService.SUCCESS_MESSAGE,
      MyActorService.class,
      true,
      60000);

    Thread.sleep(3000);
    String actorType="MyActorTest";
    logger.debug("Creating proxy builder");

    ActorProxyBuilder<ActorProxy> proxyBuilder = deferClose(new ActorProxyBuilder(actorType, ActorProxy.class));
    logger.debug("Creating actorId");
    ActorId actorId1 = new ActorId(ACTOR_ID);
    logger.debug("Building proxy");
    ActorProxy proxy = proxyBuilder.build(actorId1);

    logger.debug("Invoking Say from Proxy");
    callWithRetry(() -> {
      logger.debug("Invoking Say from Proxy");
      String sayResponse = proxy.invoke("say", "message", String.class).block();
      logger.debug("asserting not null response: [" + sayResponse + "]");
      assertNotNull(sayResponse);
    }, 60000);

    logger.debug("Invoking actor method 'startTimer' which will register a timer");
    proxy.invoke("startTimer", "myTimer").block();

    // invoke a bunch of calls in parallel to validate turn-based concurrency
    logger.debug("Invoking an actor method 'say' in parallel");
    List<String> sayMessages = new ArrayList<String>();
    for (int i = 0; i < 10; i++) {
      sayMessages.add("hello" + i);
    }

    sayMessages.parallelStream().forEach( i -> {
      // the actor method called below should reverse the input
      String msg = "message" + i;
      String reversedString = new StringBuilder(msg).reverse().toString();
      String output = proxy.invoke("say", "message" + i, String.class).block();
      assertTrue(reversedString.equals(output));
    });

    logger.debug("Calling method to register reminder named " + REMINDER_NAME);
    proxy.invoke("startReminder", REMINDER_NAME).block();

    logger.debug("Pausing 7 seconds to allow timer and reminders to fire");
    Thread.sleep(7000);

    List<MethodEntryTracker> logs = fetchMethodCallLogs(proxy);
    validateTurnBasedConcurrency(logs);

    validateMethodCalls(logs, TIMER_METHOD_NAME, 2);
    validateMethodCalls(logs, REMINDER_METHOD_NAME, 3);

    // call unregister
    logger.debug("Calling actor method 'stopTimer' to unregister timer");
    proxy.invoke("stopTimer", "myTimer").block();

    logger.debug("Calling actor method 'stopReminder' to unregister reminder");
    proxy.invoke("stopReminder", REMINDER_NAME).block();

    // make some more actor method calls and sleep a bit to see if the timer fires (it should not)
    sayMessages.parallelStream().forEach( i -> {
      proxy.invoke("say", "message" + i, String.class).block();
    });

    logger.debug("Pausing 5 seconds to allow time for timer and reminders to fire if there is a bug.  They should not since we have unregistered them.");
    Thread.sleep(5000);

    // get history again, we don't additional timer/reminder calls
    logs = fetchMethodCallLogs(proxy);
    validateEventNotObserved(logs, "stopTimer", TIMER_METHOD_NAME);
    validateEventNotObserved(logs, "stopReminder", REMINDER_METHOD_NAME);

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
        System.out.println(msg);
        Assert.fail(msg);
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
    System.out.println("Validating event " + methodNameThatShouldNotAppear + " does not appear after event " + startingPointMethodName);
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
        System.out.println(errorMessage);
        System.out.println("Dumping all logs");
        for(MethodEntryTracker l : logs) {
          System.out.println("    " + l.toString());
        }

        throw new RuntimeException(errorMessage);
      }
    }
  }
}
