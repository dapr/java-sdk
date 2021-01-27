## Exception handling sample

This sample illustrates how to handle exceptions in Dapr.

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

Then get into the examples directory:
```sh
cd examples
```

### Running the StateClient
This example uses the Java SDK Dapr client in order perform an invalid operartion, causing Dapr runtime to return an error. See the code snippet bellow: 

```java
public class Client {

  public static void main(String[] args) throws Exception {
    try (DaprClient client = new DaprClientBuilder().build()) {

      try {
        client.getState("Unknown state store", "myKey", String.class).block();
      } catch (DaprException exception) {
        System.out.println("Error code: " + exception.getErrorCode());
        System.out.println("Error message: " + exception.getMessage());

        exception.printStackTrace();
      }

      System.out.println("Done");
    }
  }

}
```
The code uses the `DaprClient` created by the `DaprClientBuilder`. It tries to get a state from state store but provides an unknown state store. It causes Dapr sidecar to return error, which is converted to a `DaprException` to the application. To be compatible with Project Reactor, `DaprException` extends from `RuntimeException` - making it an unchecked exception. Applications might also get `IllegalArgumentException` when invoking methods with invalid input parameters that are validated at the client side.

The Dapr client is also within a try-with-resource block to properly close the client at the end.

### Running the example

Run this example with the following command:
```sh
dapr run -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.exception.Client
```
Once running, the OutputBindingExample should print the output as follows:

![stateouput](../../../../../resources/img/exception.png)
