# Dapr Bindings Sample

In this sample, we'll create two java applications: an output binding application and an input binding application, using Dapr Java SDK. 
This sample includes two applications:

* InputBindingExample (Initializes the Dapr Spring boot application client)
* OutputBindingExample (pushes the event message)

Visit [this](https://docs.dapr.io/developing-applications/building-blocks/bindings/bindings-overview/) link for more information about Dapr and bindings concepts.
 
## Binding sample using the Java-SDK

In this example, the component used is Kafka, but others are also available.

Visit [this](https://github.com/dapr/components-contrib/tree/master/bindings) link for more information about binding implementations.


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

Then, go into the examples directory:

```sh
cd examples
```

### Initialize Dapr

Run `dapr init` to initialize Dapr in Self-Hosted Mode if it's not already initialized.

### Setting Kafka locally

Before getting into the application code, follow these steps in order to set up a local instance of Kafka. This is needed for the local instances.

1. Run the container locally:

<!-- STEP
name: Setup kafka container
expected_stderr_lines:
  - 'Creating network "http_default" with the default driver'
sleep: 5
-->

```bash
docker-compose -f ./src/main/java/io/dapr/examples/bindings/http/docker-compose-single-kafka.yml up -d
```

<!-- END_STEP -->

2. Run `docker ps` to see the container running locally: 

```bash
342d3522ca14        kafka-docker_kafka                      "start-kafka.sh"         14 hours ago        Up About
a minute   0.0.0.0:9092->9092/tcp                               kafka-docker_kafka_1
0cd69dbe5e65        wurstmeister/zookeeper                  "/bin/sh -c '/usr/sb…"   8 days ago          Up About
a minute   22/tcp, 2888/tcp, 3888/tcp, 0.0.0.0:2181->2181/tcp   kafka-docker_zookeeper_1
```
Click [here](https://github.com/wurstmeister/kafka-docker) for more information about the kafka broker server.

### Running the Input binding sample

The input binding sample uses the Spring Boot´s DaprApplication class for initializing the `InputBindingController`. In `InputBindingExample.java` file, you will find the `InputBindingExample` class and the `main` method. See the code snippet below:

```java
public class InputBindingExample {

  public static void main(String[] args) throws Exception {
    ///..
    // If port string is not valid, it will throw an exception.
    int port = Integer.parseInt(cmd.getOptionValue("port"));

    // Start Dapr's callback endpoint.
    DaprApplication.start(port);
  }
///...
}
```

`DaprApplication.start()` Method will run an Spring Boot application that registers the `InputBindingController`, which exposes the actual handling of the event message as a POST request. The Dapr's sidecar is the one that performs the actual call to this controller, based on the binding features and the output binding action. 

```java
@RestController
public class InputBindingController {

  @PostMapping(path = "/bindingSample")
  public Mono<Void> handleInputBinding(@RequestBody(required = false) byte[] body) {
    return Mono.fromRunnable(() ->
      System.out.println("Received message through binding: " + (body == null ? "" : new String(body))));
  }

}
```

Execute the following command to run the Input Binding example:

<!-- STEP
name: Run input binding
expected_stdout_lines:
  - '== APP == Received message through binding: {"message":"Message #0"}'
  - '== APP == Received message through binding: "Message #1"'
  - '== APP == Received message through binding: {"message":"Message #2"}'
  - '== APP == Received message through binding: "Message #3"'
background: true
sleep: 10
-->

```bash
dapr run --components-path ./components/bindings --app-id inputbinding --app-port 3000 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.bindings.http.InputBindingExample -p 3000
```

<!-- END_STEP -->

### Running the Output binding sample

The output binding application is a simple Java class with a main method that uses the Dapr Client to invoke binding.

In the `OutputBindingExample.java` file, you will find the `OutputBindingExample` class, containing the main method. The main method declares a Dapr Client using the `DaprClientBuilder` class. Notice that this builder gets two serializer implementations in the constructor: one is for Dapr's sent and recieved objects, and the second is for objects to be persisted. The client publishes events using the `invokeBinding` method. The Dapr client is also within a try-with-resource block to properly close the client at the end. See the code snippet below: 
```java
public class OutputBindingExample{
///...
  static final String BINDING_NAME = "sample123";

  static final String BINDING_OPERATION = "create";
///...
  public static void main(String[] args) throws Exception {
      try (DaprClient client = new DaprClientBuilder().build()) {
  
        int count = 0;
        while (!Thread.currentThread().isInterrupted()) {
          String message = "Message #" + (count);
  
          // On even number, send class message
          if (count % 2 == 0) {
            // This is an example of sending data in a user-defined object.  The input binding will receive:
            //   {"message":"hello"}
            MyClass myClass = new MyClass();
            myClass.message = message;
  
            System.out.println("sending a class with message: " + myClass.message);
            client.invokeBinding(BINDING_NAME, BINDING_OPERATION, myClass).block();
          } else {
            System.out.println("sending a plain string: " + message);
            client.invokeBinding(BINDING_NAME, BINDING_OPERATION, message).block();
          }
          count++;
  
          try {
            Thread.sleep((long) (10000 * Math.random()));
          } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
          }
        }
  
        System.out.println("Done.");
      }
    }
///...
}
```

This example binds two events: A user-defined data object (using the `myClass` object as parameter) and a simple string using the same `invokeBinding` method.

Execute the following command to run the Output Binding example:

<!-- STEP
name: Run output binding
expected_stdout_lines:
  - '== APP == sending a class with message: Message #0'
  - '== APP == sending a plain string: Message #1'
  - '== APP == sending a class with message: Message #2'
  - '== APP == sending a plain string: Message #3'
background: true
sleep: 30
-->

```bash
dapr run --components-path ./components/bindings --app-id outputbinding -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.bindings.http.OutputBindingExample
```

<!-- END_STEP -->

Once running, the OutputBindingExample should print the output as follows:

```txt
== APP == sending a class with message: Message #0

== APP == sending a plain string: Message #1

== APP == sending a class with message: Message #2

== APP == sending a plain string: Message #3
```

Events have been sent.

Once running, the InputBindingExample should print the output as follows:

```txt
== APP == Received message through binding: {"message":"Message #0"}

== APP == Received message through binding: "Message #1"

== APP == Received message through binding: {"message":"Message #2"}

== APP == Received message through binding: "Message #3"
```

Events have been retrieved from the binding.

To stop both apps, press `CTRL+C` or run:

<!-- STEP
name: Cleanup apps
-->

```bash
dapr stop --app-id inputbinding
dapr stop --app-id outputbinding
```

<!-- END_STEP -->

For bringing down the kafka cluster that was started in the beginning, run

<!-- STEP
name: Cleanup Kafka containers
-->

```bash
docker-compose -f ./src/main/java/io/dapr/examples/bindings/http/docker-compose-single-kafka.yml down
```

<!-- END_STEP -->

For more details on the Dapr Spring Boot integration, please refer to the [Dapr Spring Boot](../../DaprApplication.java)  Application implementation.
