/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.it.actors;

import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorProxyBuilder;
import io.dapr.client.DaprApiProtocol;
import io.dapr.it.BaseIT;
import io.dapr.it.actors.app.MyActor;
import io.dapr.it.actors.app.MyActorService;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static io.dapr.it.Retry.callWithRetry;
import static io.dapr.it.actors.MyActorTestUtils.validateMethodCalls;

public class ActorReentrancyIT extends BaseIT {

  private static final String FIRST_ACTOR = UUID.randomUUID().toString();
  private static final String SECOND_ACTOR = UUID.randomUUID().toString();
  private static final String THIRD_ACTOR = UUID.randomUUID().toString();
  private static final String REENTRANT_METHOD_NAME = "reentrantCall";
  private static final String SAY_METHOD_NAME = "say";
  private static final String ACTOR_TYPE = "MyActorTest";

  @Test
  public void testActorCanMakeReentrantCalls() throws Exception {
    startDaprApp(
        ActorReentrancyIT.class.getSimpleName(),
        "Started MyActorService",
        MyActorService.class,
        true,
        true,
        60000,
        DaprApiProtocol.HTTP,
        DaprApiProtocol.HTTP,
        "./components/featureconfig.yaml");

    final ActorProxyBuilder<MyActor> proxyBuilder =
        new ActorProxyBuilder<>(ACTOR_TYPE, MyActor.class, newActorClient());

    final ActorId firstId = new ActorId(FIRST_ACTOR);
    final MyActor mainActor = proxyBuilder.build(firstId);

    callWithRetry(() -> mainActor.reentrantCall(FIRST_ACTOR), 5000);
    final List<MethodEntryTracker> logs = new ArrayList<>();
    callWithRetry(() -> {
      logs.clear();
      logs.addAll(fetchMethodCallLogs(mainActor));
      validateMethodCalls(logs, REENTRANT_METHOD_NAME, 2);
      validateMethodCalls(logs, SAY_METHOD_NAME, 1);
    }, 5000);

    callWithRetry(mainActor::clearCallLog, 5000);

    callWithRetry(() ->
        mainActor.reentrantCall(String.join(",", Arrays.asList(SECOND_ACTOR, THIRD_ACTOR, FIRST_ACTOR))),
        5000);
    callWithRetry(() -> {
      logs.clear();
      logs.addAll(fetchMethodCallLogs(mainActor));
      validateMethodCalls(logs, REENTRANT_METHOD_NAME, 2);
      validateMethodCalls(logs, SAY_METHOD_NAME, 1);
    }, 5000);

    final ActorId secondId = new ActorId(SECOND_ACTOR);
    final MyActor secondActor = proxyBuilder.build(secondId);

    callWithRetry(() -> {
      logs.clear();
      logs.addAll(fetchMethodCallLogs(secondActor));
      validateMethodCalls(logs, REENTRANT_METHOD_NAME, 1);
    }, 5000);

    final ActorId thirdId = new ActorId(THIRD_ACTOR);
    final MyActor thirdActor = proxyBuilder.build(thirdId);

    callWithRetry(() -> {
      logs.clear();
      logs.addAll(fetchMethodCallLogs(thirdActor));
      validateMethodCalls(logs, REENTRANT_METHOD_NAME, 1);
    }, 5000);
  }

  private List<MethodEntryTracker> fetchMethodCallLogs(MyActor actor) {
    ArrayList<String> logs = actor.getCallLog();
    ArrayList<MethodEntryTracker> trackers = new ArrayList<MethodEntryTracker>();
    for(String t : logs) {
      String[] toks = t.split("\\|");
      MethodEntryTracker m = new MethodEntryTracker(
          toks[0].equals("Enter"),
          toks[1],
          new Date(toks[2]));
      trackers.add(m);
    }

    return trackers;
  }
}
