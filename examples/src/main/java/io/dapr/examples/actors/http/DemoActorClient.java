/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.examples.actors.http;

import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorProxy;
import io.dapr.actors.client.ActorProxyBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Client for Actor runtime.
 * 1. Build and install jars:
 * mvn clean install
 * 2. Run the client:
 * dapr run --app-id demoactorclient --port 3006 -- mvn exec:java -pl=examples -Dexec.mainClass=io.dapr.examples.actors.http.DemoActorClient
 */
public class DemoActorClient {

  private static final int NUM_ACTORS = 3;

  private static final int NUM_MESSAGES_PER_ACTOR = 10;

  private static final String METHOD_NAME = "say";

  private static final ExecutorService POOL = Executors.newFixedThreadPool(NUM_ACTORS);

  public static void main(String[] args) throws Exception {
    ActorProxyBuilder builder = new ActorProxyBuilder("DemoActor", null);

    List<CompletableFuture<Void>> futures = new ArrayList<>(NUM_ACTORS);

    for (int i = 0; i < NUM_ACTORS; i++) {
      ActorProxy actor = builder.build(ActorId.createRandom());
      futures.add(callActorNTimes(actor));
    }

    futures.forEach(CompletableFuture::join);
    POOL.shutdown();
    POOL.awaitTermination(1, TimeUnit.MINUTES);

    System.out.println("Done.");
  }

  private static final CompletableFuture<Void> callActorNTimes(ActorProxy actor) {
    return CompletableFuture.runAsync(() -> {
      actor.invokeActorMethod("registerReminder").block();
      for (int i = 0; i < NUM_MESSAGES_PER_ACTOR; i++) {
        actor.invokeActorMethod("incrementAndGet", 1).block();
        String result = actor.invokeActorMethod(METHOD_NAME,
          String.format("Actor %s said message #%d", actor.getActorId().toString(), i), String.class).block();
        System.out.println(String.format("Actor %s got a reply: %s", actor.getActorId().toString(), result));
        try {
          Thread.sleep((long)(1000 * Math.random()));
        } catch (InterruptedException e) {
          e.printStackTrace();
          Thread.currentThread().interrupt();
          return;
        }
      }

      System.out.println(
        "Messages sent: " + actor.invokeActorMethod("incrementAndGet", 0, int.class).block());
    }, POOL);
  }
}
