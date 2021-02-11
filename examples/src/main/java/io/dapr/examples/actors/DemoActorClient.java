/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.examples.actors;

import io.dapr.actors.ActorId;
import io.dapr.actors.client.ActorClient;
import io.dapr.actors.client.ActorProxyBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Client for Actor runtime to invoke actor methods.
 * 1. Build and install jars:
 * mvn clean install
 * 2. cd to [repo-root]/examples
 * 3. Run the client:
 * dapr run --components-path ./components/actors --app-id demoactorclient -- java -jar \
 * target/dapr-java-sdk-examples-exec.jar io.dapr.examples.actors.DemoActorClient
 */
public class DemoActorClient {

  private static final int NUM_ACTORS = 3;

  /**
   * The main method.
   * @param args Input arguments (unused).
   * @throws InterruptedException If program has been interrupted.
   */
  public static void main(String[] args) throws InterruptedException {
    try (ActorClient client = new ActorClient()) {
      ActorProxyBuilder<DemoActor> builder = new ActorProxyBuilder(DemoActor.class, client);
      List<Thread> threads = new ArrayList<>(NUM_ACTORS);

      // Creates multiple actors.
      for (int i = 0; i < NUM_ACTORS; i++) {
        ActorId actorId = ActorId.createRandom();
        DemoActor actor = builder.build(actorId);

        // Start a thread per actor.
        Thread thread = new Thread(() -> callActorForever(actorId.toString(), actor));
        thread.start();
        threads.add(thread);
      }

      // Waits for threads to finish.
      for (Thread thread : threads) {
        thread.join();
      }
    }

    System.out.println("Done.");
  }

  /**
   * Makes multiple method calls into actor until interrupted.
   * @param actorId Actor's identifier.
   * @param actor Actor to be invoked.
   */
  private static final void callActorForever(String actorId, DemoActor actor) {
    // First, register reminder.
    actor.registerReminder();

    // Now, we run until thread is interrupted.
    while (!Thread.currentThread().isInterrupted()) {
      // Invoke actor method to increment counter by 1, then build message.
      int messageNumber = actor.incrementAndGet(1).block();
      String message = String.format("Actor %s said message #%d", actorId, messageNumber);

      // Invoke the 'say' method in actor.
      String result = actor.say(message);
      System.out.println(String.format("Actor %s got a reply: %s", actorId, result));

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
