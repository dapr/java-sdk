/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.actors;

import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorProxy;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.it.AppRun;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import io.dapr.it.actors.app.MyActorService;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class ActorTimerRecoveryIT extends BaseIT {

  private static Logger logger = LoggerFactory.getLogger(ActorTimerRecoveryIT.class);

  /**
   * Create an actor, register a timer, validates its content, restarts the Actor and confirms timer continues.
   * @throws Exception This test is not expected to throw.  Thrown exceptions are bugs.
   */
  @Test
  public void timerRecoveryTest() throws Exception {
    ImmutablePair<AppRun, DaprRun> runs = startSplitDaprAndApp(
      ActorTimerRecoveryIT.class.getSimpleName(),
      "Started MyActorService",
      MyActorService.class,
      true,
      60000);

    Thread.sleep(3000);
    String actorType="MyActorTest";
    logger.debug("Creating proxy builder");

    ActorProxyBuilder<ActorProxy> proxyBuilder = new ActorProxyBuilder(actorType, ActorProxy.class);
    logger.debug("Creating actorId");
    ActorId actorId = new ActorId(UUID.randomUUID().toString());
    logger.debug("Building proxy");
    ActorProxy proxy = proxyBuilder.build(actorId);

    logger.debug("Invoking actor method 'startTimer' which will register a timer");
    proxy.invokeActorMethod("startTimer", "myTimer").block();

    logger.debug("Pausing 7 seconds to allow timer to fire");
    Thread.sleep(7000);

    ArrayList<MethodEntryTracker> logs = getAppMethodCallLogs(proxy);
    validateTimerCalls(logs, 3);

    // Restarts app only.
    runs.left.stop();
    runs.left.start();

    logger.debug("Pausing 10 seconds to allow timer to fire");
    Thread.sleep(10000);
    ArrayList<MethodEntryTracker> newLogs = getAppMethodCallLogs(proxy);
    validateTimerCalls(newLogs, 3);

    // Check that the restart actually happened by confirming the old logs are not in the new logs.
    for (MethodEntryTracker oldLog: logs) {
      for (MethodEntryTracker newLog: newLogs) {
        assertNotEquals(oldLog.toString(), newLog.toString());
      }
    }

    // call unregister
    logger.debug("Calling actor method 'stopTimer' to unregister timer");
    proxy.invokeActorMethod("stopTimer", "myTimer").block();
  }

  ArrayList<MethodEntryTracker> getAppMethodCallLogs(ActorProxy proxy) {
    ArrayList<String> logs = proxy.invokeActorMethod("getCallLog", ArrayList.class).block();
    ArrayList<MethodEntryTracker> trackers = new ArrayList<MethodEntryTracker>();
    for(String t : logs) {
      String[] toks = t.split("\\|");
      MethodEntryTracker m = new MethodEntryTracker(
        toks[0].equals("Enter") ? true : false,
        toks[1],
        new Date(toks[2]));
      trackers.add(m);
    }

    return trackers;
  }

  /**
   * Validate the timer and reminder has been invoked at least x times.
   * @param logs logs with info about method entries and exits returned from the app
   * @param minimum minimum number of entries.
   */
  void validateTimerCalls(ArrayList<MethodEntryTracker> logs, int minimum) {
    // Validate the timer has been invoked at least x times. We cannot validate precisely because of
    // differences due issues like how loaded the machine may be. Based on its dueTime and period, and our sleep above,
    // we validate below with some margin.  Events for each actor method call include "enter" and "exit"
    // calls, so they are divided by 2.
    List<MethodEntryTracker> timerInvocations = logs.stream().filter(x -> x.getMethodName().equals(("clock"))).collect(Collectors.toList());
    System.out.println("Size of timer count list is %d, which means it's been invoked half that many times" + timerInvocations.size());
    assertTrue(timerInvocations.size() / 2 >= minimum);
  }

}
