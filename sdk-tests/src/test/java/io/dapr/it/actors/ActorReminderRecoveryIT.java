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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

public class ActorReminderRecoveryIT extends BaseIT {

  private static Logger logger = LoggerFactory.getLogger(ActorReminderRecoveryIT.class);

  private ActorProxy proxy;

  private ImmutablePair<AppRun, DaprRun> runs;

  @Before
  public void init() throws Exception {
    runs = startSplitDaprAndApp(
        ActorReminderRecoveryIT.class.getSimpleName(),
        "Started MyActorService",
        MyActorService.class,
        true,
        60000);

    Thread.sleep(3000);

    ActorId actorId = new ActorId(UUID.randomUUID().toString());
    String actorType="MyActorTest";
    logger.debug("Creating proxy builder");

    ActorProxyBuilder<ActorProxy> proxyBuilder = deferClose(new ActorProxyBuilder(actorType, ActorProxy.class));
    logger.debug("Creating actorId");
    logger.debug("Building proxy");
    proxy = proxyBuilder.build(actorId);
  }

  @After
  public void tearDown() {
    // call unregister
    logger.debug("Calling actor method 'stopReminder' to unregister reminder");
    proxy.invokeActorMethod("stopReminder", "myReminder").block();
  }

  /**
   * Create an actor, register a reminder, validates its content, restarts the runtime and confirms reminder continues.
   * @throws Exception This test is not expected to throw.  Thrown exceptions are bugs.
   */
  @Test
  public void reminderRecoveryTest() throws Exception {
    logger.debug("Invoking actor method 'startReminder' which will register a reminder");
    proxy.invokeActorMethod("startReminder", "myReminder").block();

    logger.debug("Pausing 7 seconds to allow reminder to fire");
    Thread.sleep(7000);

    ArrayList<MethodEntryTracker> logs = getAppMethodCallLogs(proxy);
    validateReminderCalls(logs, 3);

    // Restarts runtime only.
    runs.right.stop();
    runs.right.start();

    logger.debug("Pausing 5 seconds to allow sidecar to be healthy");
    Thread.sleep(5000);
    ArrayList<MethodEntryTracker> newLogs = getAppMethodCallLogs(proxy);
    logger.debug("Pausing 10 seconds to allow reminder to fire a few times");
    Thread.sleep(10000);
    ArrayList<MethodEntryTracker> newLogs2 = getAppMethodCallLogs(proxy);
    logger.debug("Check if there has been additional calls");
    validateReminderCalls(newLogs2, countReminderCalls(newLogs) + 3);
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
   * Validate the reminder has been invoked at least x times.
   * @param logs logs with info about method entries and exits returned from the app
   * @return number of successful invocations of reminder
   */
  private int countReminderCalls(ArrayList<MethodEntryTracker> logs) {
    // Counts number of times reminder is invoked.
    // Events for each actor method call include "enter" and "exit" calls, so they are divided by 2.
    List<MethodEntryTracker> calls =
        logs.stream().filter(x -> x.getMethodName().equals(("receiveReminder"))).collect(Collectors.toList());
    System.out.printf(
        "Size of reminder count list is %d, which means it's been invoked half that many times.", calls.size());
    return calls.size() / 2;
  }

  /**
   * Validate the reminder has been invoked at least x times.
   * @param logs logs with info about method entries and exits returned from the app
   * @param minimum minimum number of entries.
   */
  void validateReminderCalls(ArrayList<MethodEntryTracker> logs, int minimum) {
    // Validate the reminder has been invoked at least x times. We cannot validate precisely because of
    // differences due issues like how loaded the machine may be. Based on its dueTime and period, and our sleep above,
    // we validate below with some margin.
    int callsCount = countReminderCalls(logs);
    assertTrue(callsCount >= minimum);
  }

}
