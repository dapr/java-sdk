## Retrieve Configurations via Configuration API

This example provides the different capabilities provided by Dapr Java SDK for Configuration. For further information about Configuration APIs please refer to [this link](https://docs.dapr.io/developing-applications/building-blocks/configuration/)
**This API is available in Preview Mode**.

### Using the ConfigurationAPI

The java SDK exposes several methods for this -
* `client.getConfiguration(...)` for getting a configuration for a single/multiple keys.
* `client.subscribeToConfigurations(...)` for subscribing to a list of keys for any change.

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
## Store few dummy configurations in configurationstore
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

This example uses the Java SDK Dapr client in order to **Get, Subscribe and Unsubscribe** from configuration items and utilizes `Redis` as configuration store.
`ConfigurationClient.java` is the example class demonstrating all 3 features.
Kindly check [DaprPreviewClient.java](https://github.com/dapr/java-sdk/blob/master/sdk/src/main/java/io/dapr/client/DaprPreviewClient.java) for detailed description of the supported APIs.

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
      System.out.println("Using preview client...");
      getConfigurationForSingleKey(client);
      getConfigurationsUsingVarargs(client);
      getConfigurations(client);
      subscribeConfigurationRequestWithSubscribe(client);
      unsubscribeConfigurationItems(client);
    }
  }

  /**
   * Gets configuration for a single key.
   *
   * @param client DaprPreviewClient object
   */
  public static void getConfigurationForaSingleKey(DaprPreviewClient client) {
    System.out.println("*******trying to retrieve configuration given a single key********");
    try {
      Mono<ConfigurationItem> item = client.getConfiguration(CONFIG_STORE_NAME, keys.get(0));
      // ..
    } catch (Exception ex) {}
  }

  /**
   * Gets configurations for varibale no. of arguments.
   *
   * @param client DaprPreviewClient object
   */
  public static void getConfigurationsUsingVarargs(DaprPreviewClient client) {
    System.out.println("*******trying to retrieve configurations for a variable no. of keys********");
    try {
      Mono<List<ConfigurationItem>> items =
          client.getConfiguration(CONFIG_STORE_NAME, "myconfig1", "myconfig3");
      // ..
    } catch (Exception ex) {}
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
  - "== APP == Using preview client..."
  - "== APP == *******trying to retrieve configuration given a single key********"
  - "== APP == Value ->val1 key ->myconfig1"
  - "== APP == *******trying to retrieve configurations for a variable no. of keys********"
  - "== APP == val1 : key ->myconfig1"
  - "== APP == val3 : key ->myconfig3"
  - "== APP == *******trying to retrieve configurations for a list of keys********"
  - "== APP == val1 : key ->myconfig1"
  - "== APP == val2 : key ->myconfig2"
  - "== APP == val3 : key ->myconfig3"
  - "== APP == Subscribing to key: myconfig1"
  - "== APP == update_myconfigvalue1 : key ->myconfig1"
  - "== APP == update_myconfigvalue2 : key ->myconfig1"
  - "== APP == update_myconfigvalue3 : key ->myconfig1"
  - "== APP == Subscribing to key: myconfig2"
  - "== APP == update_myconfigvalue1 : key ->myconfig2"
  - "== APP == update_myconfigvalue2 : key ->myconfig2"
  - "== APP == update_myconfigvalue3 : key ->myconfig2"
  - "== APP == update_myconfigvalue4 : key ->myconfig2"
  - "== APP == update_myconfigvalue5 : key ->myconfig2"
  - "== APP == Is Unsubscribe successful: true"
background: true
sleep: 10
-->

```bash
dapr run --components-path ./components/configuration --app-id configgrpc --log-level debug -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.configuration.grpc.ConfigurationClient
```

<!-- END_STEP -->

### Sample output
```
== APP == Using preview client...
== APP == *******trying to retrieve configuration given a single key********
== APP == Value ->val1 key ->myconfig1
== APP == *******trying to retrieve configurations for a variable no. of keys********
== APP == val1 : key ->myconfig1
== APP == val3 : key ->myconfig3
== APP == *******trying to retrieve configurations for a list of keys********
== APP == val1 : key ->myconfig1
== APP == val2 : key ->myconfig2
== APP == val3 : key ->myconfig3
== APP == Subscribing to key: myconfig1"
== APP == update_myconfigvalue1 : key ->myconfig1
== APP == update_myconfigvalue2 : key ->myconfig1
== APP == update_myconfigvalue3 : key ->myconfig1
== APP == Subscribing to key: myconfig2"
== APP == update_myconfigvalue1 : key ->myconfig2
== APP == update_myconfigvalue2 : key ->myconfig2
== APP == update_myconfigvalue3 : key ->myconfig2
== APP == update_myconfigvalue4 : key ->myconfig2
== APP == update_myconfigvalue5 : key ->myconfig2
== APP == IsUnsubscribed : true


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

