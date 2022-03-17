## State management sample

This sample illustrates the capabilities provided by Dapr Java SDK for state management. For further information about state management please refer to [this link](https://docs.dapr.io/developing-applications/building-blocks/state-management/state-management-overview/)

## Pre-requisites

* [Dapr and Dapr Cli](https://docs.dapr.io/getting-started/install-dapr/).
* Java JDK 11 (or greater):
    * [Microsoft JDK 11](https://docs.microsoft.com/en-us/java/openjdk/download#openjdk-11)
    * [Oracle JDK 11](https://www.oracle.com/technetwork/java/javase/downloads/index.html#JDK11)
    * [OpenJDK 11](https://jdk.java.net/11/)
* [Apache Maven](https://maven.apache.org/install.html) version 3.x.

### Checking out the code

Clone this repository:

```sh
git clone https://github.com/dapr/java-sdk.git
cd java-sdk
```

Then build the Maven project:

```sh
# make sure you are in the `java-sdk` directory.
mvn install
```

Then change into the `examples` directory:
```sh
cd examples
```

### Running the StateClient
This example uses the Java SDK Dapr client in order to save, retrieve and delete a state, in this case, an instance of a class. Multiple state stores are supported since Dapr 0.4. See the code snippet bellow: 

```java
public class StateClient {
  ///...
  private static final String STATE_STORE_NAME = "statestore";

  private static final String FIRST_KEY_NAME = "myKey";

  private static final String SECOND_KEY_NAME = "myKey2";
  ///...
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
        Mono<List<State<MyClass>>> retrievedMessagesMono = client.getStates(STATE_STORE_NAME,
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
            System.out.println(String.format("Expected failure. %s ", ex.getMessage()));
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
  
        Mono<List<State<MyClass>>> retrievedDeletedMessageMono = client.getStates(STATE_STORE_NAME,
            Arrays.asList(FIRST_KEY_NAME, SECOND_KEY_NAME), MyClass.class);
        System.out.println("Trying to retrieve deleted states: ");
        retrievedDeletedMessageMono.block().forEach(System.out::println);
  
        // This is an example, so for simplicity we are just exiting here.
        // Normally a dapr app would be a web service and not exit main.
        System.out.println("Done");
      }
    }
}
```
The code uses the `DaprClient` created by the `DaprClientBuilder`. Notice that this builder uses default settings. Internally, it is using `DefaultObjectSerializer` for two properties: `objectSerializer` is for Dapr's sent and received objects, and `stateSerializer` is for objects to be persisted. 

This example performs multiple operations:
* `client.waitForSidecar(...)` for waiting until Dapr sidecar is ready.
* `client.saveState(...)` for persisting an instance of `MyClass`.
* `client.getState(...)` operation in order to retrieve back the persisted state using the same key. 
* `client.executeStateTransaction(...)` operation in order to update existing state and add new state. 
* `client.getBulkState(...)` operation in order to retrieve back the persisted states using the same keys.
* `client.deleteState(...)` operation to remove  one of the persisted states. An example of etag mismatch error if a different than current etag is added to request.
* `client.executeStateTransaction(...)` operation in order to remove the other persisted state.

Finally, the code tries to retrieve the deleted states, which should not be found. 

The Dapr client is also within a try-with-resource block to properly close the client at the end.

### Running the example
<!-- STEP
name: Check state example
expected_stdout_lines:
  - "== APP == Waiting for Dapr sidecar ..."
  - "== APP == Dapr sidecar is ready."    
  - "== APP == Saving class with message: my message"
  - "== APP == Retrieved class message from state: my message"
  - "== APP == Updating previous state and adding another state 'test state'... "
  - "== APP == Saving updated class with message: my message updated"
  - "== APP == Retrieved messages using bulk get:"
  - "== APP == StateKeyValue{key='myKey', value=my message updated, etag='2', metadata={'{}'}, error='null', options={'null'}}"
  - "== APP == StateKeyValue{key='myKey2', value=test message, etag='1', metadata={'{}'}, error='null', options={'null'}}"
  - "== APP == Deleting states..."
  - "== APP == Verify delete key request is aborted if an etag different from stored is passed."
  - "== APP == Expected failure. ABORTED"
  - "== APP == Trying to delete again with correct etag."
  - "== APP == Trying to retrieve deleted states:"
  - "== APP == StateKeyValue{key='myKey', value=null, etag='null', metadata={'{}'}, error='null', options={'null'}}"
  - "== APP == StateKeyValue{key='myKey2', value=null, etag='null', metadata={'{}'}, error='null', options={'null'}}"
  - "== APP == Done"
background: true
sleep: 5 
-->

Run this example with the following command:
```bash
dapr run --components-path ./components/state --app-id state_example -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.state.StateClient 'my message'
```

<!-- END_STEP -->

Once running, the StateClient should print the output as follows:

```txt
== APP == Waiting for Dapr sidecar ...
== APP == Dapr sidecar is ready.
== APP == Saving class with message: my message
== APP == Retrieved class message from state: my message
== APP == Updating previous state and adding another state 'test state'... 
== APP == Saving updated class with message: my message updated
== APP == Retrieved messages using bulk get:
== APP == StateKeyValue{key='myKey', value=my message updated, etag='2', metadata={'{}'}, error='null', options={'null'}}
== APP == StateKeyValue{key='myKey2', value=test message, etag='1', metadata={'{}'}, error='null', options={'null'}}
== APP == Deleting states...
== APP == Verify delete key request is aborted if an etag different from stored is passed.
== APP == Expected failure. ABORTED: failed deleting state with key myKey: possible etag mismatch. error from state store: ERR Error running script (call to f_9b5da7354cb61e2ca9faff50f6c43b81c73c0b94): @user_script:1: user_script:1: failed to delete Tailmad-Fang||myKey 
== APP == Trying to delete again with correct etag.
== APP == Trying to retrieve deleted states: 
== APP == StateKeyValue{key='myKey', value=null, etag='null', metadata={'{}'}, error='null', options={'null'}}
== APP == StateKeyValue{key='myKey2', value=null, etag='null', metadata={'{}'}, error='null', options={'null'}}
== APP == Done
```

### Cleanup

To close the app either press `CTRL+C` or run

<!-- STEP
name: Cleanup
-->

```bash
dapr stop --app-id state_example
```

<!-- END_STEP -->
