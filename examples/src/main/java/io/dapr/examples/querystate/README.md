## State management sample

This sample illustrates the capabilities provided by Dapr Java SDK for querying states. For further information about querying saved state please refer to [this link](https://docs.dapr.io/developing-applications/building-blocks/state-management/howto-state-query-api/)

## Pre-requisites

* [Dapr and Dapr Cli](https://docs.dapr.io/getting-started/install-dapr/).
* Java JDK 11 (or greater): [Oracle JDK](https://www.oracle.com/technetwork/java/javase/downloads/index.html#JDK11) or [OpenJDK](https://jdk.java.net/13/).
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
public class QuerySavedState {

  public static class MyData {
    ///...
  }

  private static final String STATE_STORE_NAME = "querystatestore";

  private static final String FIRST_KEY_NAME = "key1";

  private static final String SECOND_KEY_NAME = "key2";

  private static final String THIRD_KEY_NAME = "key3";

  /**
   * Executes the sate actions.
   * @param args messages to be sent as state value.
   */
  public static void main(String[] args) throws Exception {
    DaprClientBuilder builder = new DaprClientBuilder();
    try (DaprClient client = builder.build(); DaprPreviewClient previewClient = builder.buildPreviewClient()) {
      System.out.println("Waiting for Dapr sidecar ...");
      client.waitForSidecar(10000).block();
      System.out.println("Dapr sidecar is ready.");

      String searchVal = args.length == 0 ? "searchValue" : args[0];

      MyData dataQueried = new MyData();
      dataQueried.setPropertyA(searchVal);
      dataQueried.setPropertyB("query");
      MyData dataNotQueried = new MyData();
      dataNotQueried.setPropertyA("no query");
      dataNotQueried.setPropertyB("no query");


      System.out.println("Insert key: " + FIRST_KEY_NAME + ", data: " + dataQueried);
      client.saveState(STATE_STORE_NAME, FIRST_KEY_NAME, dataQueried).block();
      System.out.println("Insert key: " + SECOND_KEY_NAME + ", data: " + dataQueried);
      client.saveState(STATE_STORE_NAME, SECOND_KEY_NAME, dataQueried).block();
      System.out.println("Insert key: " + THIRD_KEY_NAME + ", data: " + dataNotQueried);
      client.saveState(STATE_STORE_NAME, THIRD_KEY_NAME, dataNotQueried).block();
      Query query = new Query().setFilter(new EqFilter<>("propertyA", searchVal))
          .setSort(new Sorting[]{new Sorting("propertyB", Sorting.Order.ASC)});

      QueryStateRequest request = new QueryStateRequest(STATE_STORE_NAME)
          .setQuery(query);


      QueryStateResponse<MyData> result = previewClient.queryState(request, MyData.class).block();

      System.out.println("Found " + result.getResults().size() + " items.");
      for (QueryStateItem<MyData> item : result.getResults()) {
        System.out.println("Key: " + item.getKey());
        System.out.println("Data: " + item.getValue());
      }

      // This is an example, so for simplicity we are just exiting here.
      // Normally a dapr app would be a web service and not exit main.
      System.out.println("Done");
      System.exit(0);
    }
  }
}
```
The code uses the `DaprClient` created by the `DaprClientBuilder` for waiting for sidecar to start as well as to save state. Notice that this builder uses default settings. Internally, it is using `DefaultObjectSerializer` for two properties: `objectSerializer` is for Dapr's sent and received objects, and `stateSerializer` is for objects to be persisted.

The code uses the `DaprPreviewClient` created by the `DaprClientBuilder` is used for the `queryState` preview API.

This example performs multiple operations:
* `client.waitForSidecar(...)` for waiting until Dapr sidecar is ready.
* `client.saveState(...)` for persisting an instance of `MyData`.
* `client.query(...)` operation in order to query for persisted state.

The Dapr clients are also within a try-with-resource block to properly close the clients at the end.

### Running the example
<!-- STEP
name: Check state example
expected_stdout_lines:
  - "== APP == Waiting for Dapr sidecar ..."
  - "== APP == Dapr sidecar is ready."
  - "== APP == Insert key: key1, data: MyData{propertyA='querySearch', propertyB='query'}"
  - "== APP == Insert key: key2, data: MyData{propertyA='querySearch', propertyB='query'}"
  - "== APP == Insert key: key3, data: MyData{propertyA='no query', propertyB='no query'}"
  - "== APP == Found 2 items."
  - "== APP == Key: key1"
  - "== APP == Data: MyData{propertyA='querySearch', propertyB='query'}"
  - "== APP == Key: key2"
  - "== APP == Data: MyData{propertyA='querySearch', propertyB='query'}"
  - "== APP == Done"
background: true
sleep: 10 
-->

Run this example with the following command:
```bash
dapr run --components-path ./components/state --app-id query_state_example -H 3600 -- java -Ddapr.api.protocol=HTTP -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.querystate.QuerySavedState 'querySearch'
```

<!-- END_STEP -->

Once running, the QuerySaveState example should print the output as follows:

```txt
== APP == Waiting for Dapr sidecar ...
== APP == Dapr sidecar is ready.
== APP == Insert key: key1, data: MyData{propertyA='querySearch', propertyB='query'}
== APP == Insert key: key2, data: MyData{propertyA='querySearch', propertyB='query'}
== APP == Insert key: key3, data: MyData{propertyA='no query', propertyB='no query'}
== APP == Found 2 items.
== APP == Key: key1
== APP == Data: MyData{propertyA='querySearch', propertyB='query'}
== APP == Key: key2
== APP == Data: MyData{propertyA='querySearch', propertyB='query'}
== APP == Done
```

### Cleanup

To close the app either press `CTRL+C` or run

<!-- STEP
name: Cleanup
-->

```bash
dapr stop --app-id query_state_example
```

<!-- END_STEP -->
