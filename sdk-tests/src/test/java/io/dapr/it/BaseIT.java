/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.junit.AfterClass;

public abstract class BaseIT {

  protected static final String STATE_STORE_NAME = "statestore";

  private static final Map<String, DaprRun.Builder> DAPR_RUN_BUILDERS = new HashMap<>();

  private static final Collection<DaprRun> DAPR_RUNS = new ArrayList<>();

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
      String successMessage,
      Class serviceClass,
      Boolean useAppPort,
      Boolean useDaprPorts,
      int maxWaitMilliseconds) throws Exception {
    DaprRun.Builder builder = new DaprRun.Builder(
        testName,
        () -> DaprPorts.build(useAppPort, useDaprPorts, useDaprPorts),
        successMessage,
        serviceClass,
        maxWaitMilliseconds);
    DaprRun run = builder.build();
    DAPR_RUNS.add(run);
    DAPR_RUN_BUILDERS.put(run.getAppName(), builder);
    run.start();
    run.use();
    return run;
  }

  @AfterClass
  public static void cleanUp() throws Exception {
    for (DaprRun app : DAPR_RUNS) {
      app.stop();
    }
  }

}
