## Exception handling sample

This sample illustrates how to handle exceptions in Dapr.

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
# make sure you are in the `java-sdk` (root) directory.
./mvnw clean install
```

Then get into the examples directory:
```sh
cd examples
```

### Understanding the code

This example uses the Java SDK Dapr client in order to perform an invalid operation, causing Dapr runtime to return an error. See the code snippet below, from `Client.java`: 

```java
public class Client {

  public static void main(String[] args) throws Exception {
    try (DaprClient client = new DaprClientBuilder().build()) {
      try {
        client.publishEvent("unknown_pubsub", "mytopic", "mydata").block();
      } catch (DaprException exception) {
        System.out.println("Dapr exception's error code: " + exception.getErrorCode());
        System.out.println("Dapr exception's message: " + exception.getMessage());
        System.out.println("Dapr exception's reason: " + exception.getStatusDetails().get(
                DaprErrorDetails.ErrorDetailType.ERROR_INFO,
                "reason",
                TypeRef.STRING));
        System.out.println("Error's payload size: " + exception.getPayload().length);
      }
    }
    System.out.println("Done");
  }

}
```

### Running the example

<!-- STEP
name: Run exception example 
expected_stdout_lines:
  - 'Error code: INVALID_ARGUMENT'
  - 'Error message: INVALID_ARGUMENT: pubsub unknown_pubsub is not found'
  - 'Reason: DAPR_PUBSUB_NOT_FOUND'
  - 'Error payload size: 116'
background: true
sleep: 5
-->

```bash
dapr run --app-id exception-example -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.exception.Client
```

<!-- END_STEP -->

Once running, the State Client Example should print the output as follows:

```txt
Error code: ERR_PUBSUB_NOT_FOUND

Error message: ERR_PUBSUB_NOT_FOUND: pubsub unknown_pubsub is not found

Reason: DAPR_PUBSUB_NOT_FOUND

Error payload size: 116
...

```

### Debug

You can further explore all the error details returned in the `DaprException` class.
Before running it in your favorite IDE (like IntelliJ), compile and run the Dapr sidecar first.

1. Pre-req:
```sh
# make sure you are in the `java-sdk` (root) directory.
./mvnw clean install
```
2. From the examples directory, run: `dapr run --app-id exception-example --dapr-grpc-port=50001 --dapr-http-port=3500`
3. From your IDE click the play button on the client code and put break points where desired.

### Cleanup

To stop the app run (or press `CTRL+C`):

<!-- STEP
name: Cleanup
-->

```bash
dapr stop --app-id exception-example
```

<!-- END_STEP -->