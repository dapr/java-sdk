/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.AfterClass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseIT {

  protected static final String STATE_STORE_NAME = "statestore";

  private static final Map<String, DaprRun.Builder> DAPR_RUN_BUILDERS = new HashMap<>();

  private static final Collection<Stoppable> TO_BE_STOPPED = new ArrayList<>();

  protected static DaprRun startDaprApp(
      String testName,
      String successMessage,
      Class serviceClass,
      Boolean useAppPort,
      int maxWaitMilliseconds) throws Exception {
    return startDaprApp(testName, successMessage, serviceClass, useAppPort, true, maxWaitMilliseconds);
  }

  protected static DaprRun startDaprApp(
      String testName,
      int maxWaitMilliseconds) throws Exception {
    return startDaprApp(testName, "You're up and running!", null, false, true, maxWaitMilliseconds);
  }

  protected static DaprRun startDaprApp(
          String testName,
          String successMessage,
          Class serviceClass,
          Boolean useAppPort,
          Boolean useDaprPorts,
          int maxWaitMilliseconds) throws Exception {
    DaprRun.Builder builder = new DaprRun.Builder(
            testName,
            () -> DaprPorts.build(useAppPort, useDaprPorts, useDaprPorts),
            successMessage,
            maxWaitMilliseconds).withServiceClass(serviceClass);
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
    DaprRun.Builder builder = new DaprRun.Builder(
            testName,
            () -> DaprPorts.build(useAppPort, true, true),
            successMessage,
            maxWaitMilliseconds).withServiceClass(serviceClass);
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
    for (Stoppable toBeStopped : TO_BE_STOPPED) {
      toBeStopped.stop();
    }
  }

}
