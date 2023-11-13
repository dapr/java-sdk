## Retrieve Configurations via Configuration API

This example provides the different capabilities provided by Dapr Java SDK for Configuration. For further information about Configuration APIs please refer to [this link](https://docs.dapr.io/developing-applications/building-blocks/configuration/)

### Using the ConfigurationAPI

The Java SDK exposes several methods for this -
* `client.getConfiguration(...)` for getting configuration for a single/multiple key(s).
* `client.subscribeConfiguration(...)` for subscribing to a list of keys for any change.
* `client.unsubscribeConfiguration(...)` for unsubscribing to changes from subscribed items.

## Pre-requisites

* [Dapr CLI](https://docs.dapr.io/getting-started/install-dapr-cli/).
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

Then get into the examples directory:

```sh
cd examples
```

### Initialize Dapr

Run `dapr init` to initialize Dapr in Self-Hosted Mode if it's not already initialized.

## Store dummy configurations in configuration store
<!-- STEP
name: Set configuration value
expected_stdout_lines:
  - "OK"
timeout_seconds: 20
-->

```bash
docker exec dapr_redis redis-cli MSET myconfig1 "val1||1" myconfig2 "val2||1" myconfig3 "val3||1"
```
<!-- END_STEP -->

### Running the example

This example uses the Java SDK Dapr client in order to **Get, Subscribe and Unsubscribe** from configuration items, and utilizes `Redis` as configuration store.
`ConfigurationClient.java` is the example class demonstrating all 3 features.
Kindly check [DaprPreviewClient.java](https://github.com/dapr/java-sdk/blob/master/sdk/src/main/java/io/dapr/client/DaprPreviewClient.java) for a detailed description of the supported APIs.

```java
public class ConfigurationClient {
    // ... 
  /**
   * Executes various methods to check the different apis.
   * @param args arguments
   * @throws Exception throws Exception
   */
  public static void main(String[] args) throws Exception {
    try (DaprPreviewClient client = (new DaprClientBuilder()).buildPreviewClient()) {
      System.out.println("Using Dapr client...");
      getConfigurations(client);
      subscribeConfigurationRequestWithSubscribe(client);
      unsubscribeConfigurationItems(client);
    }
  }

  /**
   * Gets configurations for a list of keys.
   *
   * @param client DaprPreviewClient object
   */
  public static void getConfigurations(DaprPreviewClient client) {
    System.out.println("*******trying to retrieve configurations for a list of keys********");
    List<String> keys = new ArrayList<>();
    // ...
    GetConfigurationRequest req = new GetConfigurationRequest(CONFIG_STORE_NAME, keys);
    try {
      Mono<List<ConfigurationItem>> items = client.getConfiguration(req);
      // ..
    } catch (Exception ex) {}
  }

  /**
   * Subscribe to a list of keys.Optional to above iterator way of retrieving the changes
   *
   * @param client DaprPreviewClient object
   */
  public static void subscribeConfigurationRequestWithSubscribe(DaprPreviewClient client) {
    System.out.println("Subscribing to key: myconfig1");
    // ...
    Runnable subscribeTask = () -> {
      Flux<SubscribeConfigurationResponse> outFlux = client.subscribeConfiguration(req);
      // ...
    };
    // ..
  }

  /**
   * Unsubscribe using subscription id.
   *
   * @param client DaprPreviewClient object
   */
  public static void unsubscribeConfigurationItems(DaprPreviewClient client) {
    System.out.println("Subscribing to key: myconfig2");
    // ..
    Runnable subscribeTask = () -> {
      Flux<SubscribeConfigurationResponse> outFlux = client.subscribeConfiguration(CONFIG_STORE_NAME, "myconfig2");
      // ...
    };
    // ...

    UnsubscribeConfigurationResponse res = client.unsubscribeConfiguration(
        subscriptionId.get(),
        CONFIG_STORE_NAME
    ).block();
    // ..
  }
}
```

Get into the examples' directory:
```sh
cd examples
```

Use the following command to run this example-

<!-- STEP
name: Run ConfigurationClient example
expected_stdout_lines:
  - "== APP == Using Dapr client..."
  - "== APP == *******trying to retrieve configurations for a list of keys********"
  - "== APP == val1 : key ->myconfig1"
  - "== APP == val2 : key ->myconfig2"
  - "== APP == val3 : key ->myconfig3"
  - "== APP == Subscribing to key: myconfig1"
  - "== APP == subscription ID :"
  - "== APP == subscribing to key myconfig1 is successful"
background: true
output_match_mode: substring
sleep: 10
-->

```bash
dapr run --components-path ./components/configuration --app-id configgrpc --log-level debug -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.configuration.grpc.ConfigurationClient
```

<!-- END_STEP -->

### Sample output
```
== APP == Using Dapr client...
== APP == *******trying to retrieve configurations for a list of keys********
== APP == val1 : key ->myconfig1
== APP == val2 : key ->myconfig2
== APP == val3 : key ->myconfig3
== APP == Subscribing to key: myconfig1
== APP == subscription ID : 82bb8e24-f69d-477a-9126-5ffaf995f498
== APP == subscribing to key myconfig1 is successful


```
### Cleanup

To stop the app, run (or press CTRL+C):

<!-- STEP
name: Cleanup
-->

```bash
dapr stop --app-id configgrpc
```

<!-- END_STEP -->

