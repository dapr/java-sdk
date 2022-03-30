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

package io.dapr.it;

import io.dapr.actors.client.ActorClient;
import io.dapr.client.DaprApiProtocol;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.AfterClass;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import static io.dapr.client.DaprApiProtocol.GRPC;
import static io.dapr.client.DaprApiProtocol.HTTP;

public abstract class BaseIT {

  protected static final String STATE_STORE_NAME = "statestore";

  protected static final String QUERY_STATE_STORE = "mongo-statestore";

  private static final Map<String, DaprRun.Builder> DAPR_RUN_BUILDERS = new HashMap<>();

  private static final Queue<Stoppable> TO_BE_STOPPED = new LinkedList<>();

  private static final Queue<AutoCloseable> TO_BE_CLOSED = new LinkedList<>();

  protected static DaprRun startDaprApp(
      String testName,
      String successMessage,
      Class serviceClass,
      Boolean useAppPort,
      int maxWaitMilliseconds) throws Exception {
    return startDaprApp(testName, successMessage, serviceClass, useAppPort, maxWaitMilliseconds, GRPC);
  }

  protected static DaprRun startDaprApp(
      String testName,
      String successMessage,
      Class serviceClass,
      DaprApiProtocol appProtocol,
      int maxWaitMilliseconds) throws Exception {
    return startDaprApp(testName, successMessage, serviceClass, true, maxWaitMilliseconds, GRPC, appProtocol);
  }

  protected static DaprRun startDaprApp(
      String testName,
      String successMessage,
      Class serviceClass,
      Boolean useAppPort,
      int maxWaitMilliseconds,
      DaprApiProtocol protocol) throws Exception {
    return startDaprApp(
        testName,
        successMessage,
        serviceClass,
        useAppPort,
        true,
        maxWaitMilliseconds,
        protocol,
        HTTP);
  }

  protected static DaprRun startDaprApp(
      String testName,
      String successMessage,
      Class serviceClass,
      Boolean useAppPort,
      int maxWaitMilliseconds,
      DaprApiProtocol protocol,
      DaprApiProtocol appProtocol) throws Exception {
    return startDaprApp(
        testName,
        successMessage,
        serviceClass,
        useAppPort,
        true,
        maxWaitMilliseconds,
        protocol,
        appProtocol);
  }

  protected static DaprRun startDaprApp(
      String testName,
      int maxWaitMilliseconds) throws Exception {
    return startDaprApp(
        testName,
        "You're up and running!",
        null,
        false,
        true,
        maxWaitMilliseconds,
        GRPC,
        HTTP);
  }

  protected static DaprRun startDaprApp(
          String testName,
          String successMessage,
          Class serviceClass,
          Boolean useAppPort,
          Boolean useDaprPorts,
          int maxWaitMilliseconds,
          DaprApiProtocol protocol,
          DaprApiProtocol appProtocol) throws Exception {
    DaprRun.Builder builder = new DaprRun.Builder(
            testName,
            () -> DaprPorts.build(useAppPort, useDaprPorts, useDaprPorts),
            successMessage,
            maxWaitMilliseconds,
            protocol,
            appProtocol).withServiceClass(serviceClass);
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
    return startSplitDaprAndApp(
        testName, successMessage, serviceClass, useAppPort, maxWaitMilliseconds, DaprApiProtocol.GRPC);
  }

  protected static ImmutablePair<AppRun, DaprRun> startSplitDaprAndApp(
      String testName,
      String successMessage,
      Class serviceClass,
      Boolean useAppPort,
      int maxWaitMilliseconds,
      DaprApiProtocol protocol) throws Exception {
    return startSplitDaprAndApp(
        testName,
        successMessage,
        serviceClass,
        useAppPort,
        maxWaitMilliseconds,
        protocol,
        HTTP);
  }

  protected static ImmutablePair<AppRun, DaprRun> startSplitDaprAndApp(
          String testName,
          String successMessage,
          Class serviceClass,
          Boolean useAppPort,
          int maxWaitMilliseconds,
          DaprApiProtocol protocol,
          DaprApiProtocol appProtocol) throws Exception {
    DaprRun.Builder builder = new DaprRun.Builder(
            testName,
            () -> DaprPorts.build(useAppPort, true, true),
            successMessage,
            maxWaitMilliseconds,
            protocol,
            appProtocol).withServiceClass(serviceClass);
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
      TO_BE_CLOSED.remove().close();
    }

    while (!TO_BE_STOPPED.isEmpty()) {
      TO_BE_STOPPED.remove().stop();
    }
  }

  protected ActorClient newActorClient() {
    ActorClient client = new ActorClient();
    TO_BE_CLOSED.add(client);
    return client;
  }
}
