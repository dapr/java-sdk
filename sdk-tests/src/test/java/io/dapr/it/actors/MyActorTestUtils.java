/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.it.actors;

import io.dapr.actors.client.ActorProxy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

/**
 * Utility class for tests that use MyActor class.
 */
public class MyActorTestUtils {

  private MyActorTestUtils() {}

  /**
   * Count number of calls.
   * @param logs logs with info about method entries and exits returned from the app
   * @param methodName name of the method to be counted
   * @return number of successful invocations of reminder
   */
  static int countMethodCalls(List<MethodEntryTracker> logs, String methodName) {
    // Counts number of times reminder is invoked.
    // Events for each actor method call include "enter" and "exit" calls, so they are divided by 2.
    List<MethodEntryTracker> calls =
        logs.stream().filter(x -> x.getMethodName().equals(methodName)).collect(Collectors.toList());
    System.out.printf(
        "Size of %s count list is %d, which means it's been invoked half that many times.\n", methodName, calls.size());
    return calls.size() / 2;
  }

  /**
   * Validate the number of call of a given method.
   * @param logs logs with info about method entries and exits returned from the app
   * @param methodName name of the method to be validated.
   * @param minimum minimum number of entries.
   */
  static void validateMethodCalls(List<MethodEntryTracker> logs, String methodName, int minimum) {
    int callsCount = countMethodCalls(logs, methodName);
    assertTrue(callsCount >= minimum);
  }

  /**
   * Fetches the call log for the given Actor.
   * @param proxy Actor proxy for the actor.
   * @return List of call log.
   */
  static List<MethodEntryTracker> fetchMethodCallLogs(ActorProxy proxy) {
    ArrayList<String> logs = proxy.invokeMethod("getCallLog", ArrayList.class).block();
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
}
