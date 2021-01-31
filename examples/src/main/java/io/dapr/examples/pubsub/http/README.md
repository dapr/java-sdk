# Dapr Pub-Sub Sample

In this sample, we'll create a publisher and a subscriber java applications using Dapr, based on the publish-subcribe pattern. The publisher will generate messages of a specific topic, while subscriber will listen for messages of specific topic. See [Why Pub-Sub](#why-pub-sub) to understand when this pattern might be a good choice for your software architecture.

Visit [this](https://docs.dapr.io/developing-applications/building-blocks/pubsub/pubsub-overview/) link for more information about Dapr and Pub-Sub.
 
## Pub-Sub Sample using the Java-SDK

This sample uses the HTTP Client provided in Dapr Java SDK for subscribing, and Dapr Spring Boot integration for publishing. This example uses Redis Streams (enabled in Redis versions => 5).
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

### Running the subscriber

The first is the subscriber. It will subscribe to the topic to be used by the publisher and read the messages published. The Subscriber uses the Spring Boot´s DaprApplication class for initializing the `SubscriberController`. In `Subscriber.java` file, you will find the `Subscriber` class and the `main` method. See the code snippet below:

```java
public class Subscriber {

  public static void main(String[] args) throws Exception {
    ///...
    // Start Dapr's callback endpoint.
    DaprApplication.start(port);
  }
}
```
`DaprApplication.start()` Method will run an Spring Boot application that registers the `SubscriberController`, which exposes the message retrieval as a POST request. The Dapr's sidecar is the one that performs the actual call to the controller, based on the pubsub features.

This Spring Controller handles the message endpoint, Printing the message which is received as the POST body. The topic subscription in Dapr is handled automatically via the `@Topic` annotation. See the code snippet below:

```java
@RestController
public class SubscriberController {
  ///...
  @Topic(name = "testingtopic", pubsubName = "messagebus")
  @PostMapping(path = "/testingtopic")
  public Mono<Void> handleMessage(@RequestBody(required = false) byte[] body,
                                   @RequestHeader Map<String, String> headers) {
    return Mono.fromRunnable(() -> {
      try {
        // Dapr's event is compliant to CloudEvent.
        CloudEventEnvelope envelope = SERIALIZER.deserialize(body, CloudEventEnvelope.class);

        String message = envelope.getData() == null ? "" : envelope.getData();
        System.out.println("Subscriber got message: " + message);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }
}
```
Execute the follow script in order to run the Subscriber example:

<!-- STEP
name: Run Subscriber
expected_stdout_lines:
  - '== APP == Subscriber got: This is message #1'
  - '== APP == Subscriber got: This is message #2'
background: true
sleep: 5
-->

```bash
dapr run --components-path ./components/pubsub --app-id subscriber --app-port 3000 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.pubsub.http.Subscriber -p 3000
```

<!-- END_STEP -->

### Running the publisher

The other component is the publisher. It is a simple java application with a main method that uses the Dapr HTTP Client to publish 10 messages to an specific topic.

In the `Publisher.java` file, you will find the `Publisher` class, containing the main method. The main method declares a Dapr Client using the `DaprClientBuilder` class. Notice that this builder gets two serializer implementations in the constructor: One is for Dapr's sent and recieved objects, and second is for objects to be persisted. The client publishes messages using `publishEvent` method. The Dapr client is also within a try-with-resource block to properly close the client at the end. See the code snippet below:  
```java
public class Publisher {
    private static final int NUM_MESSAGES = 10;
    private static final String TOPIC_NAME = "testingtopic";
    private static final String PUBSUB_NAME = "messagebus";

///...
  public static void main(String[] args) throws Exception {
      //Creating the DaprClient: Using the default builder client produces an HTTP Dapr Client
      try (DaprClient client = new DaprClientBuilder().build()) {
        for (int i = 0; i < NUM_MESSAGES; i++) {
          String message = String.format("This is message #%d", i);
          //Publishing messages
          client.publishEvent(PUBSUB_NAME, TOPIC_NAME, message).block();
          System.out.println("Published message: " + message);
          //...
        }
      }
  }
///...
}
```

This example also pushes a non-string content event, the follow code in same `Publisher` main method publishes a bite:

```java
public class Publisher {
///...
    public static void main(String[] args) throws Exception {
///...
    //Publishing a single bite: Example of non-string based content published
    client.publishEvent(
        TOPIC_NAME,
        new byte[] { 1 },
        Collections.singletonMap("content-type", "application/octet-stream")).block();
    System.out.println("Published one byte.");
    System.out.println("Done.");
  }
///...
}
```

Use the follow command to execute the Publisher example:

<!-- STEP
name: Run Publisher
expected_stdout_lines:
  - '== APP == Published message: This is message #0'
  - '== APP == Published message: This is message #1'
background: true
sleep: 15
-->

```bash
dapr run --components-path ./components/pubsub --app-id publisher -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.pubsub.http.Publisher
```

<!-- END_STEP -->

Once running, the Publisher should print the output as follows:

```txt
✅  You're up and running! Both Dapr and your app logs will appear here.

== APP == Published message: This is message #0

== APP == Published message: This is message #1

== APP == Published message: This is message #2

== APP == Published message: This is message #3

== APP == Published message: This is message #4

== APP == Published message: This is message #5

== APP == Published message: This is message #6

== APP == Published message: This is message #7

== APP == Published message: This is message #8

== APP == Published message: This is message #9

== APP == Done.

```

Messages have been published in the topic.

Once running, the Subscriber should print the output as follows:

```txt
== APP == Subscriber got: This is message #3
== APP == Subscriber got: {"id":"1f646657-0032-4797-b59b-c57b4f40743b","source":"publisher","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"application/json","data":"This is message #3","expiration":"2020-12-24T05:29:12Z"}

== APP == Subscriber got: This is message #8
== APP == Subscriber got: {"id":"a22b97ce-9008-4fba-8b57-c3c3e1f031b6","source":"publisher","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"application/json","data":"This is message #8","expiration":"2020-12-24T05:29:15Z"}

== APP == Subscriber got: This is message #0
== APP == Subscriber got: {"id":"abb2f110-6862-49f7-8c8d-189f6dcd177d","source":"publisher","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"application/json","data":"This is message #0","expiration":"2020-12-24T05:29:11Z"}

== APP == Subscriber got: This is message #7
== APP == Subscriber got: {"id":"043f31d3-c13a-4a02-ac89-64ecca946598","source":"publisher","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"application/json","data":"This is message #7","expiration":"2020-12-24T05:29:14Z"}

== APP == Subscriber got: This is message #2
== APP == Subscriber got: {"id":"acc554f4-7109-4c31-9374-0e5936b90180","source":"publisher","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"application/json","data":"This is message #2","expiration":"2020-12-24T05:29:12Z"}

== APP == Subscriber got: This is message #9
== APP == Subscriber got: {"id":"8b3ad160-368d-4b0f-9925-8fa2a2fbf5ca","source":"publisher","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"application/json","data":"This is message #9","expiration":"2020-12-24T05:29:15Z"}

== APP == Subscriber got: This is message #1
== APP == Subscriber got: {"id":"e41d4512-511a-4a2b-80f3-a0a4d091c9a5","source":"publisher","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"application/json","data":"This is message #1","expiration":"2020-12-24T05:29:11Z"}

== APP == Subscriber got: This is message #4
== APP == Subscriber got: {"id":"33e21664-128e-4fc4-b5c4-ed257f758336","source":"publisher","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"application/json","data":"This is message #4","expiration":"2020-12-24T05:29:13Z"}

== APP == Subscriber got: This is message #6
== APP == Subscriber got: {"id":"bd14f1ee-ca6b-47f7-8130-dd1e6de5b03c","source":"publisher","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"application/json","data":"This is message #6","expiration":"2020-12-24T05:29:14Z"}

== APP == Subscriber got: This is message #5
== APP == Subscriber got: {"id":"acc57cd6-71da-4ba3-9a12-9c921ca49af7","source":"publisher","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"application/json","data":"This is message #5","expiration":"2020-12-24T05:29:13Z"}

```

Messages have been retrieved from the topic.

### Message expiration (Optional)

Optionally, you can see how Dapr can automatically drop expired messages on behalf of the subscriber.
First, make sure the publisher and the subscriber applications are stopped.
Then, change the TTL constant in the `Publisher.java` file from:
```java
private static final String MESSAGE_TTL_IN_SECONDS = "1000";
```
To:
```java
private static final String MESSAGE_TTL_IN_SECONDS = "1";
```

Now rebuild the example:
```sh
mvn install
```

Run the publisher app:
```sh
dapr run --components-path ./components/pubsub --app-id publisher -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.pubsub.http.Publisher
```

Wait until all 10 messages are published like before, then wait for a few more seconds and run the subscriber app:
```sh
dapr run --components-path ./components/pubsub --app-id subscriber --app-port 3000 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.pubsub.http.Subscriber -p 3000
```

No message is consumed by the subscriber app and warnings messages are emitted from Dapr sidecar:
```txt
== DAPR == time="2020-12-23T21:21:59.085797-08:00" level=warning msg="dropping expired pub/sub event 461546c1-d2df-42bd-a6b8-3beeb952fe1e as of 2020-12-24T05:21:50Z" app_id=subscriber instance=myhost scope=dapr.runtime type=log ver=edge

== DAPR == time="2020-12-23T21:21:59.085841-08:00" level=warning msg="dropping expired pub/sub event 2d8cf9a6-4019-4dda-95fd-59218a19381b as of 2020-12-24T05:21:48Z" app_id=subscriber instance=myhost scope=dapr.runtime type=log ver=edge

== DAPR == time="2020-12-23T21:21:59.085871-08:00" level=warning msg="dropping expired pub/sub event d2a199e0-a4b8-4067-9618-6688391ad68f as of 2020-12-24T05:21:53Z" app_id=subscriber instance=myhost scope=dapr.runtime type=log ver=edge

== DAPR == time="2020-12-23T21:21:59.085894-08:00" level=warning msg="dropping expired pub/sub event 30719f17-ad8f-4dea-91b5-b77958f360d4 as of 2020-12-24T05:21:49Z" app_id=subscriber instance=myhost scope=dapr.runtime type=log ver=edge

== DAPR == time="2020-12-23T21:21:59.085797-08:00" level=warning msg="dropping expired pub/sub event d136d5ae-5561-418c-a850-9d1698bc8840 as of 2020-12-24T05:21:51Z" app_id=subscriber instance=myhost scope=dapr.runtime type=log ver=edge

== DAPR == time="2020-12-23T21:21:59.085958-08:00" level=warning msg="dropping expired pub/sub event 82b334a2-e295-48ea-8c6c-c45b1c4fcd2d as of 2020-12-24T05:21:50Z" app_id=subscriber instance=myhost scope=dapr.runtime type=log ver=edge

== DAPR == time="2020-12-23T21:21:59.085973-08:00" level=warning msg="dropping expired pub/sub event f6eb3f9f-185f-492f-9df9-45af8c91932b as of 2020-12-24T05:21:53Z" app_id=subscriber instance=myhost scope=dapr.runtime type=log ver=edge

== DAPR == time="2020-12-23T21:21:59.086041-08:00" level=warning msg="dropping expired pub/sub event a536eb9f-34e0-49fc-ba29-a34854398d96 as of 2020-12-24T05:21:52Z" app_id=subscriber instance=myhost scope=dapr.runtime type=log ver=edge

== DAPR == time="2020-12-23T21:21:59.085995-08:00" level=warning msg="dropping expired pub/sub event 52cc9528-f9d4-44f4-8f78-8f32341a743a as of 2020-12-24T05:21:49Z" app_id=subscriber instance=myhost scope=dapr.runtime type=log ver=edge

== DAPR == time="2020-12-23T21:21:59.085797-08:00" level=warning msg="dropping expired pub/sub event 7cf927e8-e832-4f8a-911a-1cae5a1369d2 as of 2020-12-24T05:21:48Z" app_id=subscriber instance=myhost scope=dapr.runtime type=log ver=edge

```

For more details on Dapr Spring Boot integration, please refer to [Dapr Spring Boot](../../DaprApplication.java) Application implementation.

### Cleanup

<!-- STEP
name: Cleanup
-->

```bash
dapr stop --app-id publisher
dapr stop --app-id subscriber
```

<!-- END_STEP -->
