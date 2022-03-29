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

package io.dapr.examples.state;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.State;
import io.dapr.client.domain.TransactionalStateOperation;
import io.dapr.exceptions.DaprException;
import io.grpc.Status;
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
      System.out.println("Waiting for Dapr sidecar ...");
      client.waitForSidecar(10000).block();
      System.out.println("Dapr sidecar is ready.");

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
              new State<>(FIRST_KEY_NAME, myClass, "")));
      operationList.add(new TransactionalStateOperation<>(TransactionalStateOperation.OperationType.UPSERT,
              new State<>(SECOND_KEY_NAME, secondState, "")));

      client.executeStateTransaction(STATE_STORE_NAME, operationList).block();

      // get multiple states
      Mono<List<State<MyClass>>> retrievedMessagesMono = client.getBulkState(STATE_STORE_NAME,
          Arrays.asList(FIRST_KEY_NAME, SECOND_KEY_NAME), MyClass.class);
      System.out.println("Retrieved messages using bulk get:");
      retrievedMessagesMono.block().forEach(System.out::println);

      System.out.println("Deleting states...");

      System.out.println("Verify delete key request is aborted if an etag different from stored is passed.");
      // delete state API
      try {
        client.deleteState(STATE_STORE_NAME, FIRST_KEY_NAME, "100", null).block();
      } catch (DaprException ex) {
        if (ex.getErrorCode().equals(Status.Code.ABORTED.toString())) {
          // Expected error due to etag mismatch.
          System.out.println(String.format("Expected failure. %s", ex.getErrorCode()));
        } else {
          System.out.println("Unexpected exception.");
          throw ex;
        }
      }

      System.out.println("Trying to delete again with correct etag.");
      String storedEtag = client.getState(STATE_STORE_NAME, FIRST_KEY_NAME, MyClass.class).block().getEtag();
      client.deleteState(STATE_STORE_NAME, FIRST_KEY_NAME, storedEtag, null).block();
      // Delete operation using transaction API
      operationList.clear();
      operationList.add(new TransactionalStateOperation<>(TransactionalStateOperation.OperationType.DELETE,
          new State<>(SECOND_KEY_NAME)));
      client.executeStateTransaction(STATE_STORE_NAME, operationList).block();

      Mono<List<State<MyClass>>> retrievedDeletedMessageMono = client.getBulkState(STATE_STORE_NAME,
          Arrays.asList(FIRST_KEY_NAME, SECOND_KEY_NAME), MyClass.class);
      System.out.println("Trying to retrieve deleted states:");
      retrievedDeletedMessageMono.block().forEach(System.out::println);

      // This is an example, so for simplicity we are just exiting here.
      // Normally a dapr app would be a web service and not exit main.
      System.out.println("Done");
    }
  }
}
