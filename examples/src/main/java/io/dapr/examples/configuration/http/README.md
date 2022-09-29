## Retrieve Configurations via Configuration API

This example provides the different capabilities provided by Dapr Java SDK for Configuration. For further information about Configuration APIs please refer to [this link](https://docs.dapr.io/developing-applications/building-blocks/configuration/)
**This API is available in Preview Mode**.

### Using the ConfigurationAPI

The java SDK exposes several methods for this -
* `client.getConfiguration(...)` for getting a configuration for a single/multiple keys.
* `client.subscribeConfiguration(...)` for subscribing to a list of keys for any change.
* `client.unsubscribeConfiguration(...)` for unsubscribing to changes from subscribed items.

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
    System.getProperties().setProperty(Properties.API_PROTOCOL.getName(), DaprApiProtocol.HTTP.name());
    try (DaprPreviewClient client = (new DaprClientBuilder()).buildPreviewClient()) {
      System.out.println("Using preview client...");
      getConfigurations(client);
      subscribeConfigurationRequest(client);
    }
  }

  /**
   * Gets configurations for a list of keys.
   *
   * @param client DaprPreviewClient object
   */
  public static void getConfigurations(DaprPreviewClient client) {
    System.out.println("*******trying to retrieve configurations for a list of keys********");
    GetConfigurationRequest req = new GetConfigurationRequest(CONFIG_STORE_NAME, keys);
    try {
      Mono<List<ConfigurationItem>> items = client.getConfiguration(req);
      // ...
    } catch (Exception ex) {
      // ...
    }
  }

  /**
   * Subscribe to a list of keys.
   *
   * @param client DaprPreviewClient object
   */
  public static void subscribeConfigurationRequest(DaprPreviewClient client) {
    // ...
    SubscribeConfigurationRequest req = new SubscribeConfigurationRequest(
        CONFIG_STORE_NAME, Collections.singletonList("myconfig2"));
    Runnable subscribeTask = () -> {
      Flux<SubscribeConfigurationResponse> outFlux = client.subscribeConfiguration(req);
      // ...
    };
    // ..
  }
}
```

Get into the examples' directory:
```sh
cd examples
```

#### Running the configuration subscriber app:

`DaprApplication.start()` Method will run an Spring Boot application that registers the `DaprController`. This controller exposes a `POST`
route which dapr sidecar calls invokes whenever any update happens to subscribed config keys.
`ConfigurationHandler` is a sample springboot application to register and receive for updates on configuration items sent by dapr. 
springboot-sdk implements a generic route and enables users to register for different handlers per configuration store. 
Users are free to write their own controllers to handle any specific route suited to the need.

```java
@RestController
public class ConfigurationHandler {
  //...
  BiConsumer<String, SubscribeConfigurationResponse> biConsumer = (store, resp) -> {
    System.out.println("Configuration update received for store : " + store);
    resp.getItems().forEach((k,v) -> {
      System.out.println("Key: "+ k + " Value :" + v.getValue());
    });
  };
  
  //....
}
```
Execute the following script to run the ConfigSubscriber app:

<!-- STEP
name: Run ConfigurationHandler
expected_stdout_lines:
  - '== APP == Configuration update received for store: configstore'
  - '== APP == Key: myconfig2 Value :updated_val2'
background: true
output_match_mode: substring
background: true
sleep: 5
-->

```bash
dapr run --app-id confighandler -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.configuration.http.ConfigurationHandler -p 3009
```
<!-- END_STEP -->

#### Running the ConfigurationClient app:

Use the following command to run this example-

<!-- STEP
name: Run ConfigurationClient example
expected_stdout_lines:
  - "== APP == Using preview client..."
  - "== APP == *******trying to retrieve configurations for a list of keys********"
  - "== APP == val1 : key ->myconfig1"
  - "== APP == val2 : key ->myconfig2"
  - "== APP == val3 : key ->myconfig3"
  - "== APP == Subscribing to key: myconfig2"
  - "== APP == subscribing to myconfig2 is successful"
background: true
output_match_mode: substring
sleep: 10
-->

```bash
dapr run --components-path ./components/configuration --app-id confighttp --log-level debug --app-port 3009 --dapr-http-port 3500 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.configuration.http.ConfigurationClient
```

#### Update myconfig2 key in configurationstore
<!-- END_STEP -->

<!-- STEP
name: Update configuration value
timeout_seconds: 20
-->

```bash
docker exec dapr_redis redis-cli MSET myconfig2 "updated_val2||1"
```
<!-- END_STEP -->

### Sample output
```
== APP == Using preview client...
== APP == *******trying to retrieve configurations for a list of keys********
== APP == val1 : key ->myconfig1
== APP == val2 : key ->myconfig2
== APP == val3 : key ->myconfig3
== APP == Subscribing to key: myconfig2
== APP == subscribing to myconfig2 is successful


```
### Cleanup

To stop the app, run (or press CTRL+C):

<!-- STEP
name: Cleanup
-->

```bash
dapr stop --app-id confighttp
dapr stop --app-id confighandler
```

<!-- END_STEP -->
