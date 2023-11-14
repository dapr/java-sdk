# Dapr Pub-Sub Sample

In this sample, we'll create a publisher and a subscriber java applications using Dapr, based on the publish-subscribe pattern. The publisher will generate messages of a specific topic, while a subscriber will listen for messages of specific topic. See the [Dapr Pub-Sub docs](https://docs.dapr.io/developing-applications/building-blocks/pubsub/) to understand when this pattern might be a good choice for your software architecture.

Visit [this](https://docs.dapr.io/developing-applications/building-blocks/pubsub/pubsub-overview/) link for more information about Dapr and Pub-Sub.
 
## Pub-Sub Sample using the Java-SDK

This sample uses the HTTP Springboot integration provided in Dapr Java SDK for subscribing, and gRPC client for publishing. This example uses Redis Streams (enabled in Redis versions => 5).
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

The subscriber will subscribe to the topic to be used by the publisher and read the messages published. The subscriber uses the Spring Boot´s DaprApplication class for initializing the `SubscriberController`. There is a gRPC version and HTTP version of the subscriber in the grpc and http folders. In `Subscriber.java` file, you will find the `Subscriber` class and the `main` method. See the code snippet below:

```java
public class Subscriber {

  public static void main(String[] args) throws Exception {
    ///...
    // Start Dapr's callback endpoint.
    DaprApplication.start([PROTOCAL],port); 
  }
}
```
`DaprApplication.start()` Method will run a Spring Boot application that registers the `SubscriberController`, which exposes the message retrieval as a POST request, or the `SubscriberGrpcService`, which implement the grpc methods that the sidecar will call. 

**HTTP Version**

The Dapr sidecar is the one that performs the actual call to the controller, based on the pubsub features. This Spring Controller handles the message endpoint, printing the message which is received as the POST body. 

The subscription's topic in Dapr is handled automatically via the `@Topic` annotation - which also supports the same expressions in 
[Spring's @Value annotations](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#beans-value-annotations).

The code snippet below shows how to create a subscription using the `@Topic` annotation showcasing expression support. In this case, `myAppProperty` is a Java property that does not exist, so the expression resolves to the default value (`messagebus`).

```java
@RestController
public class SubscriberController {
  ///...
  @Topic(name = "testingtopic", pubsubName = "${myAppProperty:messagebus}")
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

The `@BulkSubscribe` annotation can be used with `@Topic` to receive multiple messages at once. See the example below on how to handle the bulk messages and respond correctly.

```java
@RestController
public class SubscriberController {
  ///...
  @BulkSubscribe()
  @Topic(name = "testingtopicbulk", pubsubName = "${myAppProperty:messagebus}")
  @PostMapping(path = "/testingtopicbulk")
  public Mono<BulkSubscribeAppResponse> handleBulkMessage(
          @RequestBody(required = false) BulkSubscribeMessage<CloudEvent<String>> bulkMessage) {
    return Mono.fromCallable(() -> {
      System.out.println("Bulk Subscriber received " + bulkMessage.getEntries().size() + " messages.");

      List<BulkSubscribeAppResponseEntry> entries = new ArrayList<BulkSubscribeAppResponseEntry>();
      for (BulkSubscribeMessageEntry<?> entry : bulkMessage.getEntries()) {
        try {
          System.out.printf("Bulk Subscriber message has entry ID: %s\n", entry.getEntryId());
          CloudEvent<?> cloudEvent = (CloudEvent<?>) entry.getEvent();
          System.out.printf("Bulk Subscriber got: %s\n", cloudEvent.getData());
          entries.add(new BulkSubscribeAppResponseEntry(entry.getEntryId(), BulkSubscribeAppResponseStatus.SUCCESS));
        } catch (Exception e) {
          e.printStackTrace();
          entries.add(new BulkSubscribeAppResponseEntry(entry.getEntryId(), BulkSubscribeAppResponseStatus.RETRY));
        }
      }
      return new BulkSubscribeAppResponse(entries);
    });
  }
}
```


Execute the following command to run the HTTP Subscriber example:

<!-- STEP
name: Run Subscriber
match_order: none
expected_stdout_lines:
  - '== APP == Subscriber got: This is message #1'
  - '== APP == Subscriber got: This is message #2'
  - '== APP == Subscriber got from bulk published topic: This is message #2'
  - '== APP == Subscriber got from bulk published topic: This is message #3'
  - '== APP == Bulk Subscriber got: This is message #1'
  - '== APP == Bulk Subscriber got: This is message #2'
background: true
sleep: 5
-->

```bash
dapr run --components-path ./components/pubsub --app-id subscriber --app-port 3000 --app-protocol http -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.pubsub.http.Subscriber -p 3000
```

<!-- END_STEP -->

**gRPC Version**

The Spring GrpcService implements the methods required for gRPC communication with Dapr\`s sidecar. 

The `SubscriberGrpcService.java` snippet below shows the details. Dapr\`s sidecar will call `listTopicSubscriptions` to get the topic and pubsub name that are contained in the response before the subscription starts. After the pubsub component in the sidecar subscribes successfully to the specified topic, a message will be sent to the method `onTopicEvent` in the request parameter.

```java
@GrpcService
public class SubscriberGrpcService extends AppCallbackGrpc.AppCallbackImplBase {
	private final List<DaprAppCallbackProtos.TopicSubscription> topicSubscriptionList = new ArrayList<>();
	private final DaprObjectSerializer objectSerializer = new DefaultObjectSerializer();
	
	@Override
	public void listTopicSubscriptions(Empty request,
			StreamObserver<DaprAppCallbackProtos.ListTopicSubscriptionsResponse> responseObserver) {
			registerConsumer("messagebus","testingtopic");
		try {
			DaprAppCallbackProtos.ListTopicSubscriptionsResponse.Builder builder = DaprAppCallbackProtos.ListTopicSubscriptionsResponse
					.newBuilder();
			topicSubscriptionList.forEach(builder::addSubscriptions);
			DaprAppCallbackProtos.ListTopicSubscriptionsResponse response = builder.build();
			responseObserver.onNext(response);
		} catch (Throwable e) {
			responseObserver.onError(e);
		} finally {
			responseObserver.onCompleted();
		}
	}

	@Override
	public void onTopicEvent(DaprAppCallbackProtos.TopicEventRequest request,
			StreamObserver<DaprAppCallbackProtos.TopicEventResponse> responseObserver) {
		try {
			System.out.println("Subscriber got: " + request.getData());
			DaprAppCallbackProtos.TopicEventResponse response = DaprAppCallbackProtos.TopicEventResponse.newBuilder()
					.setStatus(DaprAppCallbackProtos.TopicEventResponse.TopicEventResponseStatus.SUCCESS)
					.build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Throwable e) {
			responseObserver.onError(e);
		}
	}
  ///...
}
```

Execute the following command to run the gRPC Subscriber example:

```bash
dapr run --components-path ./components/pubsub --app-id subscriber --app-port 3000 --app-protocol grpc -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.pubsub.grpc.Subscriber -p 3000
```


### Running the publisher

The publisher is a simple Java application with a main method that uses the Dapr gRPC Client to publish 10 messages to a specific topic.

In the `Publisher.java` file, you will find the `Publisher` class, containing the main method. The main method declares a Dapr Client using the `DaprClientBuilder` class. Notice that this builder gets two serializer implementations in the constructor: one is for Dapr's sent and received objects, and the second is for objects to be persisted. The client publishes messages using the `publishEvent` method. The Dapr client is also within a try-with-resource block to properly close the client at the end. See the code snippet below:
Dapr's sidecar will automatically wrap the payload received into a CloudEvent object, which will later on be parsed by the subscriber.
```java
public class Publisher {
  private static final int NUM_MESSAGES = 10;
  private static final String DEFAULT_TOPIC_NAME = "testingtopic";
  private static final String PUBSUB_NAME = "messagebus";

  ///...
  public static void main(String[] args) throws Exception {
      String topicName = getTopicName(args); // Topic can be configured by args. 
      // Creating the DaprClient: Using the default builder client produces an HTTP Dapr Client
      try (DaprClient client = new DaprClientBuilder().build()) {
        for (int i = 0; i < NUM_MESSAGES; i++) {
          String message = String.format("This is message #%d", i);
          // Publishing messages
          client.publishEvent(
                  PUBSUB_NAME,
                  topicName,
                  message,
                  singletonMap(Metadata.TTL_IN_SECONDS, MESSAGE_TTL_IN_SECONDS)).block();

          System.out.println("Published message: " + message);
          //...
        }
        ///...
      }
  }
}
```

The `CloudEventPublisher.java` file shows how the same can be accomplished if the application must send a CloudEvent object instead of relying on Dapr's automatic CloudEvent "wrapping".
In this case, the app MUST override the content-type parameter via `withContentType()`, so that Dapr's sidecar knows that the payload is already a CloudEvent object.

```java
public class CloudEventPublisher {
  ///...
  public static void main(String[] args) throws Exception {
    //Creating the DaprClient: Using the default builder client produces an HTTP Dapr Client
    try (DaprClient client = new DaprClientBuilder().build()) {
      for (int i = 0; i < NUM_MESSAGES; i++) {
        CloudEvent cloudEvent = new CloudEvent();
        cloudEvent.setId(UUID.randomUUID().toString());
        cloudEvent.setType("example");
        cloudEvent.setSpecversion("1");
        cloudEvent.setDatacontenttype("text/plain");
        cloudEvent.setData(String.format("This is message #%d", i));

        //Publishing messages
        client.publishEvent(
            new PublishEventRequestBuilder(PUBSUB_NAME, TOPIC_NAME, cloudEvent)
                .withContentType(CloudEvent.CONTENT_TYPE)
                .withMetadata(singletonMap(Metadata.TTL_IN_SECONDS, MESSAGE_TTL_IN_SECONDS))
                .build()).block();
        System.out.println("Published cloud event with message: " + cloudEvent.getData());
        //...
      }
      //...
    }
  }
}
```

Execute the following command to run the Publisher example:

<!-- STEP
name: Run Publisher
expected_stdout_lines:
  - '== APP == Published message: This is message #0'
  - '== APP == Published message: This is message #1'
background: true
sleep: 15
-->

```bash
dapr run --components-path ./components/pubsub --app-id publisher -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.pubsub.Publisher
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

### Bulk Publish Messages 
> Note : This API is currently in Alpha stage in Dapr runtime, hence the API methods in SDK are part of the DaprPreviewClient class.  

Another feature provided by the SDK is to allow users to publish multiple messages in a single call to the Dapr sidecar.
For this example, we have a simple Java application with a main method that uses the Dapr gPRC Preview Client to publish 10 messages to a specific topic in a single call.

In the `BulkPublisher.java` file, you will find the `BulkPublisher` class, containing the main method. The main method declares a Dapr Preview Client using the `DaprClientBuilder` class. Notice that this builder gets two serializer implementations in the constructor: one is for Dapr's sent and recieved objects, and the second is for objects to be persisted.
The client publishes messages using `publishEvents` method. The Dapr client is also within a try-with-resource block to properly close the client at the end. See the code snippet below:
Dapr's sidecar will automatically wrap the payload received into a CloudEvent object, which will later on be parsed by the subscriber.

```java
public class BulkPublisher {
  private static final int NUM_MESSAGES = 10;
  private static final String TOPIC_NAME = "kafkatestingtopic";
  private static final String PUBSUB_NAME = "kafka-pubsub";

  ///...
  public static void main(String[] args) throws Exception {
    OpenTelemetry openTelemetry = OpenTelemetryConfig.createOpenTelemetry();
    Tracer tracer = openTelemetry.getTracer(BulkPublisher.class.getCanonicalName());
    Span span = tracer.spanBuilder("Bulk Publisher's Main").setSpanKind(Span.Kind.CLIENT).startSpan();
    try (DaprPreviewClient client = (new DaprClientBuilder()).buildPreviewClient()) {
      DaprClient c = (DaprClient)client;
      c.waitForSidecar(10000);
      try (Scope scope = span.makeCurrent()) {
        System.out.println("Using preview client...");
        List<String> messages = new ArrayList<>();
        System.out.println("Constructing the list of messages to publish");
        for (int i = 0; i < NUM_MESSAGES; i++) {
          String message = String.format("This is message #%d", i);
          messages.add(message);
          System.out.println("Going to publish message : " + message);
        }
        BulkPublishResponse res = client.publishEvents(PUBSUB_NAME, TOPIC_NAME, messages, "text/plain")
                .subscriberContext(getReactorContext()).block();
        System.out.println("Published the set of messages in a single call to Dapr");
        if (res != null) {
          if (res.getFailedEntries().size() > 0) {
            // Ideally this condition will not happen in examples
            System.out.println("Some events failed to be published");
            for (BulkPublishResponseFailedEntry entry : res.getFailedEntries()) {
              System.out.println("EntryId : " + entry.getEntryId() + " Error message : " + entry.getErrorMessage());
            }
          }
        } else {
          throw new Exception("null response from dapr");
        }
      }
      // Close the span.

      span.end();
      // Allow plenty of time for Dapr to export all relevant spans to the tracing infra.
      Thread.sleep(10000);
      // Shutdown the OpenTelemetry tracer.
      OpenTelemetrySdk.getGlobalTracerManagement().shutdown();
  }
}
```
The code uses the `DaprPreviewClient` created by the `DaprClientBuilder` is used for the `publishEvents` (BulkPublish) preview API.

In this case, when the `publishEvents` call is made, one of the arguments to the method is the content type of data, this being `text/plain` in the example.
In this case, when parsing and printing the response, there is a concept of EntryID, which is automatically generated or can be set manually when using the `BulkPublishRequest` object.
The EntryID is a request scoped ID, in this case automatically generated as the index of the message in the list of messages in the `publishEvents` call.

The response, will be empty if all events are published successfully or it will contain the list of events that have failed.

The code also shows the scenario where it is possible to start tracing in code and pass on that tracing context to Dapr.

The `CloudEventBulkPublisher.java` file shows how the same can be accomplished if the application must send a CloudEvent object instead of relying on Dapr's automatic CloudEvent "wrapping".
In this case, the application **MUST** override the content-type parameter via `withContentType()`, so Dapr's sidecar knows that the payload is already a CloudEvent object.

```java
public class CloudEventBulkPublisher {
  ///...
  public static void main(String[] args) throws Exception {
    try (DaprPreviewClient client = (new DaprClientBuilder()).buildPreviewClient()) {
      // Construct request
      BulkPublishRequest<CloudEvent<Map<String, String>>> request = new BulkPublishRequest<>(PUBSUB_NAME, TOPIC_NAME);
      List<BulkPublishRequestEntry<CloudEvent<Map<String, String>>>> entries = new ArrayList<>();
      for (int i = 0; i < NUM_MESSAGES; i++) {
        CloudEvent<Map<String, String>> cloudEvent = new CloudEvent<>();
        cloudEvent.setId(UUID.randomUUID().toString());
        cloudEvent.setType("example");
        cloudEvent.setSpecversion("1");
        cloudEvent.setDatacontenttype("application/json");
        String val = String.format("This is message #%d", i);
        cloudEvent.setData(new HashMap<>() {
          {
            put("dataKey", val);
          }
        });
        BulkPublishRequestEntry<CloudEvent<Map<String, String>>> entry = new BulkPublishRequestEntry<>();
        entry.setEntryID("" + (i + 1))
                .setEvent(cloudEvent)
                .setContentType(CloudEvent.CONTENT_TYPE);
        entries.add(entry);
      }
      request.setEntries(entries);

      // Publish events
      BulkPublishResponse res = client.publishEvents(request).block();
      if (res != null) {
        if (res.getFailedEntries().size() > 0) {
          // Ideally this condition will not happen in examples
          System.out.println("Some events failed to be published");
          for (BulkPublishResponseFailedEntry entry : res.getFailedEntries()) {
            System.out.println("EntryId : " + entry.getEntryId() + " Error message : " + entry.getErrorMessage());
          }
        }
      } else {
        throw new Exception("null response");
      }
      System.out.println("Done");
    }
  }
}
```

Execute the following command to run the BulkPublisher example:

<!-- STEP
name: Run Bulk Publisher
match_order: sequential
expected_stdout_lines:
  - '== APP == Published the set of messages in a single call to Dapr'
background: true
sleep: 20
-->

```bash
dapr run --components-path ./components/pubsub --app-id bulk-publisher -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.pubsub.BulkPublisher
```
<!-- END_STEP -->


Once running, the BulkPublisher should print the output as follows:

```txt
✅  You're up and running! Both Dapr and your app logs will appear here.

== APP == Using preview client...
== APP == Constructing the list of messages to publish
== APP == Going to publish message : This is message #0
== APP == Going to publish message : This is message #1
== APP == Going to publish message : This is message #2
== APP == Going to publish message : This is message #3
== APP == Going to publish message : This is message #4
== APP == Going to publish message : This is message #5
== APP == Going to publish message : This is message #6
== APP == Going to publish message : This is message #7
== APP == Going to publish message : This is message #8
== APP == Going to publish message : This is message #9
== APP == Published the set of messages in a single call to Dapr
== APP == Done

```

Messages have been published in the topic.

The Subscriber started previously [here](#running-the-subscriber) should print the output as follows:

```txt
== APP == Subscriber got from bulk published topic: This is message #1
== APP == Subscriber got: {"id":"323935ed-d8db-4ea2-ba28-52352b1d1b34","source":"bulk-publisher","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"text/plain","data":"This is message #1","data_base64":null}
== APP == Subscriber got from bulk published topic: This is message #0
== APP == Subscriber got: {"id":"bb2f4833-0473-446b-a6cc-04a36de5ac0a","source":"bulk-publisher","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"text/plain","data":"This is message #0","data_base64":null}
== APP == Subscriber got from bulk published topic: This is message #5
== APP == Subscriber got: {"id":"07bad175-4be4-4beb-a983-4def2eba5768","source":"bulk-publisher","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"text/plain","data":"This is message #5","data_base64":null}
== APP == Subscriber got from bulk published topic: This is message #6
== APP == Subscriber got: {"id":"b99fba4d-732a-4d18-bf10-b37916dedfb1","source":"bulk-publisher","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"text/plain","data":"This is message #6","data_base64":null}
== APP == Subscriber got from bulk published topic: This is message #2
== APP == Subscriber got: {"id":"2976f254-7859-449e-b66c-57fab4a72aef","source":"bulk-publisher","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"text/plain","data":"This is message #2","data_base64":null}
== APP == Subscriber got from bulk published topic: This is message #3
== APP == Subscriber got: {"id":"f21ff2b5-4842-481d-9a96-e4c299d1c463","source":"bulk-publisher","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"text/plain","data":"This is message #3","data_base64":null}
== APP == Subscriber got from bulk published topic: This is message #4
== APP == Subscriber got: {"id":"4bf50438-e576-4f5f-bb40-bd31c716ad02","source":"bulk-publisher","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"text/plain","data":"This is message #4","data_base64":null}
== APP == Subscriber got from bulk published topic: This is message #7
== APP == Subscriber got: {"id":"f0c8b53b-7935-478e-856b-164d329d25ab","source":"bulk-publisher","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"text/plain","data":"This is message #7","data_base64":null}
== APP == Subscriber got from bulk published topic: This is message #9
== APP == Subscriber got: {"id":"b280569f-cc29-471f-9cb7-682d8d6bd553","source":"bulk-publisher","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"text/plain","data":"This is message #9","data_base64":null}
== APP == Subscriber got from bulk published topic: This is message #8
== APP == Subscriber got: {"id":"df20d841-296e-4c6b-9dcb-dd17920538e7","source":"bulk-publisher","type":"com.dapr.event.sent","specversion":"1.0","datacontenttype":"text/plain","data":"This is message #8","data_base64":null}
```

> Note: the Redis pubsub component does not have a native bulk publish implementation, and uses Dapr runtime's default bulk publish implementation which is concurrent. Therefore, the order of the events that are published are not guaranteed.

Messages have been retrieved from the topic.

### Bulk Subscription

You can also run the publisher to publish messages to `testingtopicbulk` topic, and receive messages using the bulk subscription.

<!-- STEP
name: Run Publisher on bulk topic
expected_stdout_lines:
  - '== APP == Published message: This is message #0'
  - '== APP == Published message: This is message #1'
background: true
sleep: 15
-->

```bash
dapr run --components-path ./components/pubsub --app-id publisher -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.pubsub.Publisher testingtopicbulk
```

<!-- END_STEP -->

Once running, the Publisher should print the same output as seen [above](#running-the-publisher). The Subscriber should print the output as follows:

```txt
== APP == Bulk Subscriber got 10 messages.
== APP == Bulk Subscriber message has entry ID: d4d81c57-d75c-4a22-a747-e907099ca135
== APP == Bulk Subscriber got: This is message #0
== APP == Bulk Subscriber message has entry ID: f109c837-f7c8-4839-8d71-2df9c467875c
== APP == Bulk Subscriber got: This is message #1
== APP == Bulk Subscriber message has entry ID: d735044f-1320-43e1-bd41-787ad9d26427
== APP == Bulk Subscriber got: This is message #2
== APP == Bulk Subscriber message has entry ID: afe74e5a-1a2b-498a-beca-7a6383141ccf
== APP == Bulk Subscriber got: This is message #3
== APP == Bulk Subscriber message has entry ID: 1df3fa51-d137-4749-891d-973ce58f1e1c
== APP == Bulk Subscriber got: This is message #4
== APP == Bulk Subscriber message has entry ID: ecab82bd-77be-40a1-8b62-2dbb3388d726
== APP == Bulk Subscriber got: This is message #5
== APP == Bulk Subscriber message has entry ID: 49a63916-ed09-4101-969e-13a860e35c55
== APP == Bulk Subscriber got: This is message #6
== APP == Bulk Subscriber message has entry ID: 897ec32c-ad74-4512-8979-ee0a455433e8
== APP == Bulk Subscriber got: This is message #7
== APP == Bulk Subscriber message has entry ID: 67367edc-27a6-4c8c-9e39-31caa0f74b2d
== APP == Bulk Subscriber got: This is message #8
== APP == Bulk Subscriber message has entry ID: f134d21f-0a05-408d-977c-1397b999e908
== APP == Bulk Subscriber got: This is message #9

```

### Tracing

Dapr handles tracing in PubSub automatically. Open Zipkin on [http://localhost:9411/zipkin](http://localhost:9411/zipkin). You should see a screen like the one below:

![zipking-landing](https://raw.githubusercontent.com/dapr/java-sdk/master/examples/src/main/resources/img/zipkin-pubsub-landing.png)

Click on the search icon to see the latest query results. You should see a tracing diagram similar to the one below:

![zipking-landing](https://raw.githubusercontent.com/dapr/java-sdk/master/examples/src/main/resources/img/zipkin-pubsub-result.png)

Once you click on the tracing event, you will see the details of the call stack starting in the client and then showing the service API calls right below.

![zipking-details](https://raw.githubusercontent.com/dapr/java-sdk/master/examples/src/main/resources/img/zipkin-pubsub-details.png)


Once you click on the bulk publisher tracing event, you will see the details of the call stack starting in the client and then showing the service API calls right below.

![zipking-details](../../../../../../resources/img/zipkin-bulk-publish-details.png)

If you would like to add a tracing span as a parent of the span created by Dapr, change the publisher to handle that. See `PublisherWithTracing.java` to see the difference and run it with:

<!-- STEP
name: Run Publisher with tracing
expected_stdout_lines:
  - '== APP == Published message: This is message #0'
  - '== APP == Published message: This is message #1'
background: true
sleep: 15
-->

```bash
dapr run --components-path ./components/pubsub --app-id publisher-tracing -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.pubsub.PublisherWithTracing
```

<!-- END_STEP -->

Now, repeat the search on Zipkin website. All the publisher and subscriber spans are under the same parent span, like in the screen below:

![zipking-details-custom-span](https://raw.githubusercontent.com/dapr/java-sdk/master/examples/src/main/resources/img/zipkin-pubsub-details-custom-span.png)

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
dapr run --components-path ./components/pubsub --app-id publisher -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.pubsub.Publisher
```

Wait until all 10 messages are published like before, then wait for a few more seconds and run the subscriber app:
```sh
dapr run --components-path ./components/pubsub --app-id subscriber --app-port 3000 -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.pubsub.Subscriber -p 3000
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

For more details on Dapr Spring Boot integration, please refer to [Dapr Spring Boot](../DaprApplication.java) Application implementation.

### Cleanup

<!-- STEP
name: Cleanup
-->

```bash
dapr stop --app-id publisher
dapr stop --app-id bulk-publisher
dapr stop --app-id subscriber
```

<!-- END_STEP -->
