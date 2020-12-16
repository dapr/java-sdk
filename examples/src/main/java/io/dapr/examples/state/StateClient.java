/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.examples.state;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.State;
import io.dapr.client.domain.TransactionalStateOperation;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 1. Build and install jars:
 * mvn clean install
 * 2. cd [repo root]/examples
 * 3. send a message to be saved as state:
 * dapr run --components-path ./components/state -- \
 *   java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.state.StateClient 'my message'
 */
public class StateClient {

  public static class MyClass {
    public String message;

    @Override
    public String toString() {
      return message;
    }
  }

  private static final String STATE_STORE_NAME = "statestore";

  private static final String FIRST_KEY_NAME = "myKey";

  private static final String SECOND_KEY_NAME = "myKey2";

  /**
   * Executes the sate actions.
   * @param args messages to be sent as state value.
   */
  public static void main(String[] args) throws Exception {
    try (DaprClient client = new DaprClientBuilder().build()) {
      String message = args.length == 0 ? " " : args[0];

      MyClass myClass = new MyClass();
      myClass.message = message;
      MyClass secondState = new MyClass();
      secondState.message = "test message";

      client.saveState(STATE_STORE_NAME, FIRST_KEY_NAME, myClass).block();
      System.out.println("Saving class with message: " + message);

      Mono<State<MyClass>> retrievedMessageMono = client.getState(STATE_STORE_NAME, FIRST_KEY_NAME, MyClass.class);
      System.out.println("Retrieved class message from state: " + (retrievedMessageMono.block().getValue()).message);

      System.out.println("Updating previous state and adding another state 'test state'... ");
      myClass.message = message + " updated";
      System.out.println("Saving updated class with message: " + myClass.message);

      // execute transaction
      List<TransactionalStateOperation<?>> operationList = new ArrayList<>();
      operationList.add(new TransactionalStateOperation<>(TransactionalStateOperation.OperationType.UPSERT,
              new State<>(myClass, FIRST_KEY_NAME, "")));
      operationList.add(new TransactionalStateOperation<>(TransactionalStateOperation.OperationType.UPSERT,
              new State<>(secondState, SECOND_KEY_NAME, "")));

      client.executeStateTransaction(STATE_STORE_NAME, operationList).block();

      // get multiple states
      Mono<List<State<MyClass>>> retrievedMessagesMono = client.getBulkState(STATE_STORE_NAME,
          Arrays.asList(FIRST_KEY_NAME, SECOND_KEY_NAME), MyClass.class);
      System.out.println("Retrieved messages using bulk get:");
      retrievedMessagesMono.block().forEach(System.out::println);

      System.out.println("Deleting states...");

      // delete state API
      Mono<Void> mono = client.deleteState(STATE_STORE_NAME, FIRST_KEY_NAME);
      mono.block();

      // Delete operation using transaction API
      operationList.clear();
      operationList.add(new TransactionalStateOperation<>(TransactionalStateOperation.OperationType.DELETE,
          new State<>(SECOND_KEY_NAME)));
      mono = client.executeStateTransaction(STATE_STORE_NAME, operationList);
      mono.block();

      Mono<List<State<MyClass>>> retrievedDeletedMessageMono = client.getBulkState(STATE_STORE_NAME,
          Arrays.asList(FIRST_KEY_NAME, SECOND_KEY_NAME), MyClass.class);
      System.out.println("Trying to retrieve deleted states: ");
      retrievedDeletedMessageMono.block().forEach(System.out::println);

      // This is an example, so for simplicity we are just exiting here.
      // Normally a dapr app would be a web service and not exit main.
      System.out.println("Done");
    }
  }
}
