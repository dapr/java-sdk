/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.examples.state;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.State;
import reactor.core.publisher.Mono;

import java.io.IOException;

/**
 * 1. Build and install jars:
 * mvn clean install
 * 2. send a message to be saved as state:
 * dapr run --components-path ./components --dapr-http-port 3006 -- \
 * java -jar examples/target/dapr-java-sdk-examples-exec.jar \
 * io.dapr.examples.state.StateClient 'my message'
 */
public class StateClient {

  public static class MyClass {
    public String message;
  }

  private static final String STATE_STORE_NAME = "statestore";

  private static final String KEY_NAME = "myKey";

  /**
   * Executes the sate actions.
   * @param args messages to be sent as state value.
   */
  public static void main(String[] args) throws IOException {
    try (DaprClient client = new DaprClientBuilder().build()) {
      String message = args.length == 0 ? " " : args[0];

      MyClass myClass = new MyClass();
      myClass.message = message;

      client.saveState(STATE_STORE_NAME, KEY_NAME, myClass).block();
      System.out.println("Saving class with message: " + message);

      Mono<State<MyClass>> retrievedMessageMono = client.getState(STATE_STORE_NAME, KEY_NAME, MyClass.class);
      System.out.println("Retrieved class message from state: " + (retrievedMessageMono.block().getValue()).message);

      System.out.println("Deleting state...");
      Mono<Void> mono = client.deleteState(STATE_STORE_NAME, KEY_NAME);
      mono.block();

      Mono<State<MyClass>> retrievedDeletedMessageMono = client.getState(STATE_STORE_NAME, KEY_NAME, MyClass.class);
      System.out.println("Trying to retrieve deleted state: " + retrievedDeletedMessageMono.block().getValue());

      // This is an example, so for simplicity we are just exiting here.
      // Normally a dapr app would be a web service and not exit main.
      System.out.println("Done");
    }
  }
}
