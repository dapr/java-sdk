## Distributed Lock API Example

This example provides the different capabilities provided by Dapr Java SDK for Distributed Lock. For further information about Distributed Lock APIs please refer to [this link](https://docs.dapr.io/developing-applications/building-blocks/distributed-lock/)
**This API is available in Preview Mode**.

### Using the Distributed Lock API

The java SDK exposes several methods for this -
* `client.tryLock(...)` for getting a free distributed lock
* `client.unlock(...)` for unlocking a lock

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

<!-- END_STEP -->

### Running the example

Get into the examples' directory:
```sh
cd examples
```

Use the following command to run this example-

<!-- STEP
name: Run DistributedLockHttpClient example
expected_stdout_lines:
  - "== APP == Using preview client..."
  - "== APP == *******trying to get a free distributed lock********"
  - "== APP == Lock result -> SUCCESS"
  - "== APP == *******unlock a distributed lock********"
  - "== APP == Unlock result -> SUCCESS"
background: true
sleep: 5
-->

```bash
dapr run --components-path ./components/lock --app-id lockhttp --log-level debug -- java -Ddapr.api.protocol=HTTP -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.lock.http.DistributedLockHttpClient
```

<!-- END_STEP -->

### Sample output
```
== APP == Using preview client...
== APP == *******trying to get a free distributed lock********
== APP == Lock result -> SUCCESS
== APP == *******unlock a distributed lock********
== APP == Unlock result -> SUCCESS
```
### Cleanup

To stop the app, run (or press CTRL+C):

<!-- STEP
name: Cleanup
-->

```bash
dapr stop --app-id lockhttp
```

<!-- END_STEP -->

