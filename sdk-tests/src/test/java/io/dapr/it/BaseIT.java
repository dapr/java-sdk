/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.AfterClass;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public abstract class BaseIT {

  protected static final String STATE_STORE_NAME = "statestore";

  private static final Map<String, DaprRun.Builder> DAPR_RUN_BUILDERS = new HashMap<>();

  private static final Queue<Stoppable> TO_BE_STOPPED = new LinkedList<>();

  private static final Queue<Closeable> TO_BE_CLOSED = new LinkedList<>();

  protected static DaprRun startDaprApp(
      String testName,
      String successMessage,
      Class serviceClass,
      Boolean useAppPort,
      int maxWaitMilliseconds) throws Exception {
    return startDaprApp(testName, successMessage, serviceClass, useAppPort, maxWaitMilliseconds, true);
  }

  protected static DaprRun startDaprApp(
      String testName,
      String successMessage,
      Class serviceClass,
      Boolean useAppPort,
      int maxWaitMilliseconds,
      boolean useGRPC) throws Exception {
    return startDaprApp(testName, successMessage, serviceClass, useAppPort, true, maxWaitMilliseconds, useGRPC);
  }

  protected static DaprRun startDaprApp(
      String testName,
      int maxWaitMilliseconds) throws Exception {
    return startDaprApp(testName, "You're up and running!", null, false, true, maxWaitMilliseconds, true);
  }

  protected static DaprRun startDaprApp(
          String testName,
          String successMessage,
          Class serviceClass,
          Boolean useAppPort,
          Boolean useDaprPorts,
          int maxWaitMilliseconds,
          boolean useGRPC) throws Exception {
    DaprRun.Builder builder = new DaprRun.Builder(
            testName,
            () -> DaprPorts.build(useAppPort, useDaprPorts, useDaprPorts),
            successMessage,
            maxWaitMilliseconds,
            useGRPC).withServiceClass(serviceClass);
    DaprRun run = builder.build();
    TO_BE_STOPPED.add(run);
    DAPR_RUN_BUILDERS.put(run.getAppName(), builder);
    run.start();
    run.use();
    return run;
  }

  protected static ImmutablePair<AppRun, DaprRun> startSplitDaprAndApp(
      String testName,
      String successMessage,
      Class serviceClass,
      Boolean useAppPort,
      int maxWaitMilliseconds) throws Exception {
    return startSplitDaprAndApp(testName, successMessage, serviceClass, useAppPort, maxWaitMilliseconds, true);
  }

  protected static ImmutablePair<AppRun, DaprRun> startSplitDaprAndApp(
          String testName,
          String successMessage,
          Class serviceClass,
          Boolean useAppPort,
          int maxWaitMilliseconds,
          boolean useGRPC) throws Exception {
    DaprRun.Builder builder = new DaprRun.Builder(
            testName,
            () -> DaprPorts.build(useAppPort, true, true),
            successMessage,
            maxWaitMilliseconds,
            useGRPC).withServiceClass(serviceClass);
    ImmutablePair<AppRun, DaprRun> runs = builder.splitBuild();
    TO_BE_STOPPED.add(runs.left);
    TO_BE_STOPPED.add(runs.right);
    DAPR_RUN_BUILDERS.put(runs.right.getAppName(), builder);
    runs.left.start();
    runs.right.start();
    runs.right.use();
    return runs;
  }

  @AfterClass
  public static void cleanUp() throws Exception {
    while (!TO_BE_CLOSED.isEmpty()) {
      Closeable toBeClosed = TO_BE_CLOSED.remove();
      toBeClosed.close();
    }

    while (!TO_BE_STOPPED.isEmpty()) {
      Stoppable toBeStopped = TO_BE_STOPPED.remove();
      toBeStopped.stop();
    }
  }

  protected static <T extends Closeable> T deferClose(T closeable) {
    TO_BE_CLOSED.add(closeable);
    return closeable;
  }
}
