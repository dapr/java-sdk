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
  - "== APP == IsUnsubscribed : true"
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

