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
This example uses the Java SDK Dapr client in order to save bulk state and query state, in this case, an instance of a class. See the code snippets below:

The class saved and queried for is as below:

```java
public class Listing {

  @JsonProperty
  private String propertyType;

  @JsonProperty
  private String id;

  @JsonProperty
  private String city;

  @JsonProperty
  private String state;

  public Listing() {
  }

  public String getPropertyType() {
    return propertyType;
  }

  public void setPropertyType(String propertyType) {
    this.propertyType = propertyType;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  @Override
  public String toString() {
    return "Listing{"
        + "propertyType='" + propertyType + '\''
        + ", id=" + id
        + ", city='" + city + '\''
        + ", state='" + state + '\''
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Listing listing = (Listing) o;
    return id == listing.id
        && propertyType.equals(listing.propertyType)
        && Objects.equals(city, listing.city)
        && Objects.equals(state, listing.state);
  }

  @Override
  public int hashCode() {
    return Objects.hash(propertyType, id, city, state);
  }
}
```

The main application class for the example is as follows: 

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

      Listing first = new Listing();
      first.setPropertyType("apartment");
      first.setId("1000");
      first.setCity("Seattle");
      first.setState("WA");
      Listing second = new Listing();
      second.setPropertyType("row-house");
      second.setId("1002");
      second.setCity("Seattle");
      second.setState("WA");
      Listing third = new Listing();
      third.setPropertyType("apartment");
      third.setId("1003");
      third.setCity("Portland");
      third.setState("OR");
      Listing fourth = new Listing();
      fourth.setPropertyType("apartment");
      fourth.setId("1001");
      fourth.setCity("Portland");
      fourth.setState("OR");
      Map<String, String> meta = new HashMap<>();
      meta.put("contentType", "application/json");

      SaveStateRequest request = new SaveStateRequest(STATE_STORE_NAME).setStates(
          new State<>("1", first, null, meta, null),
          new State<>("2", second, null, meta, null),
          new State<>("3", third, null, meta, null),
          new State<>("4", fourth, null, meta, null)
      );
      client.saveBulkState(request).block();

      System.out.println("Insert key: 1" + ", data: " + first);
      System.out.println("Insert key: 2" + ", data: " + second);
      System.out.println("Insert key: 3" + ", data: " + third);
      System.out.println("Insert key: 4" + ", data: " + fourth);


      Query query = new Query()
          .setFilter(new EqFilter<>("propertyType", "apartment"))
          .setSort(Arrays.asList(new Sorting("id", Sorting.Order.DESC)));

      QueryStateRequest queryStateRequest = new QueryStateRequest(STATE_STORE_NAME)
          .setQuery(query);

      QueryStateResponse<Listing> result = previewClient.queryState(queryStateRequest, Listing.class).block();

      System.out.println("Found " + result.getResults().size() + " items.");
      for (QueryStateItem<Listing> item : result.getResults()) {
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
* `client.saveBulkState(...)` for persisting an instance of `Listing`.
* `client.query(...)` operation in order to query for persisted state.

The Dapr clients are also within a try-with-resource block to properly close the clients at the end.

### Running the example
<!-- STEP
name: Check state example
expected_stdout_lines:
  - "== APP == Waiting for Dapr sidecar ..."
  - "== APP == Dapr sidecar is ready."
  - "== APP == Insert key: 1, data: Listing{propertyType='apartment', id=1000, city='Seattle', state='WA'}"
  - "== APP == Insert key: 2, data: Listing{propertyType='row-house', id=1002, city='Seattle', state='WA'}"
  - "== APP == Insert key: 3, data: Listing{propertyType='apartment', id=1003, city='Portland', state='OR'}"
  - "== APP == Insert key: 4, data: Listing{propertyType='apartment', id=1001, city='Portland', state='OR'}"
  - "== APP == Found 3 items."
  - "== APP == Key: 3"
  - "== APP == Data: Listing{propertyType='apartment', id=1003, city='Portland', state='OR'}"
  - "== APP == Key: 4"
  - "== APP == Data: Listing{propertyType='apartment', id=1001, city='Portland', state='OR'}"
  - "== APP == Key: 1"
  - "== APP == Data: Listing{propertyType='apartment', id=1000, city='Seattle', state='WA'}"
  - "== APP == Done"
background: true
sleep: 10 
-->

Run this example with the following command:
```bash
dapr run --components-path ./components/state --app-id query_state_example -H 3600 -- java -Ddapr.api.protocol=HTTP -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.querystate.QuerySavedState
```

<!-- END_STEP -->

Once running, the QuerySaveState example should print the output as follows:

```txt
== APP == Waiting for Dapr sidecar ...
== APP == Dapr sidecar is ready.
== APP == Insert key: 1, data: Listing{propertyType='apartment', id=1000, city='Seattle', state='WA'}
== APP == Insert key: 2, data: Listing{propertyType='row-house', id=1002, city='Seattle', state='WA'}
== APP == Insert key: 3, data: Listing{propertyType='apartment', id=1003, city='Portland', state='OR'}
== APP == Insert key: 4, data: Listing{propertyType='apartment', id=1001, city='Portland', state='OR'}
== APP == Found 3 items.
== APP == Key: 3
== APP == Data: Listing{propertyType='apartment', id=1003, city='Portland', state='OR'}
== APP == Key: 4
== APP == Data: Listing{propertyType='apartment', id=1001, city='Portland', state='OR'}
== APP == Key: 1
== APP == Data: Listing{propertyType='apartment', id=1000, city='Seattle', state='WA'}
== APP == Done
```
Note that the output is got in the descending order of the field `id` and all the `propertyType` field values are the same `apartment`.

### Cleanup

To close the app either press `CTRL+C` or run

<!-- STEP
name: Cleanup
-->

```bash
dapr stop --app-id query_state_example
```

<!-- END_STEP -->
