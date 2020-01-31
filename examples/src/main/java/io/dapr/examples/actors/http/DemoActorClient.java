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

/**
 * Client for Actor runtime to invoke actor methods.
 * 1. Build and install jars:
 * mvn clean install
 * 2. Run the client:
 * dapr run --app-id demoactorclient --port 3006 -- mvn exec:java \
 * -pl=examples -Dexec.mainClass=io.dapr.examples.actors.http.DemoActorClient
 */
public class DemoActorClient {

  private static final int NUM_ACTORS = 3;

  /**
   * The main method.
   * @param args Input arguments (unused).
   * @throws InterruptedException If program has been interrupted.
   */
  public static void main(String[] args) throws InterruptedException {
    ActorProxyBuilder builder = new ActorProxyBuilder("DemoActor");

    List<Thread> threads = new ArrayList<>(NUM_ACTORS);

    // Creates multiple actors.
    for (int i = 0; i < NUM_ACTORS; i++) {
      ActorProxy actor = builder.build(ActorId.createRandom());

      // Start a thread per actor.
      Thread thread = new Thread(() -> callActorForever(actor));
      thread.start();
      threads.add(thread);
    }

    // Waits for threads to finish.
    for (Thread thread : threads) {
      thread.join();
    }

    System.out.println("Done.");
  }

  /**
   * Makes multiple method calls into actor until interrupted.
   * @param actor Actor to be invoked.
   */
  private static final void callActorForever(ActorProxy actor) {
    // First, register reminder.
    actor.invokeActorMethod("registerReminder").block();

    // Now, we run until thread is interrupted.
    while (!Thread.currentThread().isInterrupted()) {
      // Invoke actor method to increment counter by 1, then build message.
      int messageNumber = actor.invokeActorMethod("incrementAndGet", 1, int.class).block();
      String message = String.format("Actor %s said message #%d", actor.getActorId().toString(), messageNumber);

      // Invoke the 'say' method in actor.
      String result = actor.invokeActorMethod("say", message, String.class).block();
      System.out.println(String.format("Actor %s got a reply: %s", actor.getActorId().toString(), result));

      try {
        // Waits for up to 1 second.
        Thread.sleep((long) (1000 * Math.random()));
      } catch (InterruptedException e) {
        // We have been interrupted, so we set the interrupted flag to exit gracefully.
        Thread.currentThread().interrupt();
      }
    }
  }
}
