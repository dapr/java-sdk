/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.examples.state;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.State;
import reactor.core.publisher.Mono;

/**
 * 1. Build and install jars:
 * mvn clean install
 * 2. send a message to be saved as state:
 * dapr run --port 3006 -- \
 * mvn exec:java -pl=examples -Dexec.mainClass=io.dapr.examples.state.StateClient -Dexec.args="'my message'"
 */
public class StateClient {

  public static class MyClass {
    public String message;
  }

  private static final String KEY_NAME = "myKey";

  /**
   * Executes the sate actions.
   * @param args messages to be sent as state value.
   */
  public static void main(String[] args) {
    DaprClient client = new DaprClientBuilder().build();
    String message = args.length == 0 ? " " : args[0];

    MyClass myClass = new MyClass();
    myClass.message = message;

    client.saveState(KEY_NAME, myClass).block();
    System.out.println("Saving class with message: " + message);

    Mono<State<MyClass>> retrievedMessageMono = client.getState(KEY_NAME, MyClass.class);
    System.out.println("Retrieved class message from state: " + (retrievedMessageMono.block().getValue()).message);

    System.out.println("Deleting state...");
    Mono<Void> mono = client.deleteState(KEY_NAME);
    mono.block();

    Mono<State<MyClass>> retrievedDeletedMessageMono = client.getState(KEY_NAME, MyClass.class);
    System.out.println("Trying to retrieve deleted state: " + retrievedDeletedMessageMono.block().getValue());
  }

}
