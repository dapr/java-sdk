# Dapr Streaming Subscription Sample

In this sample, we'll create a publisher and a subscriber java applications using Dapr, based on the publish-subscribe pattern. The publisher will generate messages of a specific topic, while a subscriber will listen for messages of a specific topic via a bi-directional stream. All is abstracted by the SDK. See the [Dapr Pub-Sub docs](https://docs.dapr.io/developing-applications/building-blocks/pubsub/) to understand when this pattern might be a good choice for your software architecture.

Visit [this](https://docs.dapr.io/developing-applications/building-blocks/pubsub/pubsub-overview/) link for more information about Dapr and Pub-Sub.
 
## Pub-Sub Sample using the Java-SDK

This sample shows how the subscription to events no longer requires the application to listen to an HTTP or gRPC port. This example uses Redis Streams (enabled in Redis versions => 5).
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

### Running the subscriber

The subscriber uses the `DaprPreviewClient` interface to use a new feature where events are subscribed via a streaming and processed via a callback interface.



The publisher is a simple Java application with a main method that uses the Dapr gRPC Client to publish 10 messages to a specific topic.

In the `Subscriber.java` file, you will find the `Subscriber` class, containing the main method. The main method declares a `DaprPreviewClient` using the `DaprClientBuilder` class. When invoking `subscribeToEvents`, the method returns a `Flux<CloudEvent<T>>` that can be processed using reactive operators like `doOnNext()` for event handling and `doOnError()` for error handling. The example uses `blockLast()` to keep the subscriber running indefinitely. For production use cases requiring explicit subscription lifecycle control, you can use `.subscribe()` which returns a `Disposable` that can be disposed via `disposable.dispose()`.

```java
public class Subscriber {

  // ...

  public static void main(String[] args) throws Exception {
    String topicName = getTopicName(args);
    try (var client = new DaprClientBuilder().buildPreviewClient()) {
      // Subscribe to events using the Flux-based reactive API
      // The stream will emit CloudEvent<String> objects as they arrive
      client.subscribeToEvents(
          PUBSUB_NAME,
          topicName,
          TypeRef.STRING)
          .doOnNext(event -> {
            System.out.println("Subscriber got: " + event.getData());
          })
          .doOnError(throwable -> {
            System.out.println("Subscriber got exception: " + throwable.getMessage());
          })
          .blockLast();  // Blocks indefinitely until the stream completes (keeps the subscriber running)
    }
  }
  
  // ...
}
```

Execute the following command to run the Subscriber example:

<!-- STEP
name: Run Subscriber
expected_stdout_lines:
  - '== APP == Subscriber got: This is message #0'
  - '== APP == Subscriber got: This is message #1'
background: true
sleep: 15
timeout_seconds: 30
-->

```bash
dapr run --resources-path ./components/pubsub --app-id subscriber -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.pubsub.stream.Subscriber
```

<!-- END_STEP -->

Once the subscriber is running, run the publisher in a new terminal to see the events in the subscriber's side:

<!-- STEP
name: Run Publisher
expected_stdout_lines:
  - '== APP == Published message: This is message #0'
  - '== APP == Published message: This is message #1'
background: true
sleep: 15
timeout_seconds: 30
-->

```bash
dapr run --resources-path ./components/pubsub --app-id publisher -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.pubsub.Publisher
```

<!-- END_STEP -->


