## Retrieve Configurations via Configuration API

This example provides the different capabilities provided by Dapr Java SDK for Configuration. For further information about Configuration APIs please refer to [this link](https://docs.dapr.io/developing-applications/building-blocks/configuration/)
**This API is available in Preview Mode**.

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

Get into the examples' directory:
```sh
cd examples
```

### Using the ConfigurationAPI

The java SDK exposes several methods for this -
* `client.getConfiguration(...)` for getting a configuration for a single key.
* `client.getConfigurations(...)` for getting a configurations by passing a variable no. of arguments or list of argumets.
* `client.getAllConfigurations(...)` for getting all configurations.
* `client.subscribeToConfigurations(...)` for subscribing to a list of keys for any change.

### Running the example
The code uses the `DaprPreviewClient` created by the `DaprPreviewClientBuilder`.

```bash
dapr run --app-id configgrpc --log-level debug -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.configuration.grpc.ConfigurationClient
```

### Sample output
```
== APP == Using preview client...
== APP == *******trying to retrieve configuration a key********
== APP == Value ->"value2" key ->myconfig2
== APP == *******trying to retrieve configurations for a variable no. of keys********
== APP == "wookie-pravin" : key ->configgrpc||myconfig
== APP == "value2" : key ->myconfig2
== APP == "value3" : key ->myconfig3
== APP == *******trying to retrieve configurations for a list of keys********
== APP == "value2" : key ->myconfig2
== APP == "value3" : key ->myconfig3
== APP == "value4" : key ->myconfig4
== APP == "value5" : key ->myconfig5
== APP == *****Retrieving all configurations*******
== APP == "value4" : key ->myconfig4
== APP == "wookie-pravin" : key ->configgrpc||myconfig
== APP == "value2" : key ->myconfig2
== APP == "value6" : key ->"myconfig6"
== APP == "value5" : key ->myconfig5
== APP == "value3" : key ->myconfig3
== APP == *****Subscribing to keys: [myconfig2, myconfig3, myconfig4] *****
DEBU[0030] Refreshing all mDNS addresses.                app_id=configgrpc instance=LAPTOP-OS4MBC7R scope=dapr.contrib type=log ver=1.5.1
DEBU[0030] no mDNS apps to refresh.                      app_id=configgrpc instance=LAPTOP-OS4MBC7R scope=dapr.contrib type=log ver=1.5.1
DEBU[0060] Refreshing all mDNS addresses.                app_id=configgrpc instance=LAPTOP-OS4MBC7R scope=dapr.contrib type=log ver=1.5.1
DEBU[0060] no mDNS apps to refresh.                      app_id=configgrpc instance=LAPTOP-OS4MBC7R scope=dapr.contrib type=log ver=1.5.1
== APP == "new-value" : key ->myconfig2

```
### Cleanup

To stop the apps run (or press CTRL+C):

<!-- STEP
name: Cleanup
-->

```bash
dapr stop --app-id configgrpc
```

<!-- END_STEP -->

Thanks for playing.

