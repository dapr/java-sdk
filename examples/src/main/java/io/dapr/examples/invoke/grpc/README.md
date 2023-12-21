## Invoking a Grpc endpoint using the Java-SDK

In this example, you will run a Grpc service and client using Dapr's invoke feature.

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

Get into the examples' directory:
```sh
cd examples
```

### Running the example's service

The first component is the service. It has a simple API with the `Say` method. This method will print out each message received from the client. The proto file below contains the description of the HelloWorld service found in the `./proto/examples/helloworld.proto` file:

```text
 service HelloWorld {
   rpc Say (SayRequest) returns (SayResponse) {}
 }
 
 message SayRequest {
   string message = 1;
 }
 
 message SayResponse {
   string timestamp = 1;
 }
```

In the `HelloWorldService.java` file, you will find the `HelloWorldService` class, containing the main method. The service implementation happens in the `GrpcHelloWorldDaprService` class. You can see that it extends `DaprClientImplBase` instead of `HelloWorldImplBase`. This is because this service will be called by Dapr, so it implements the service API expected by Dapr. The `DaprClientImplBase` class is part of this SDK. In a real-world application, the service would still implement it's main API as well. The Dapr's API would be exposed as an additional service. In this example, we are implementing Dapr's API only. Modifying this example to expose `HelloWorldService` is offered as an exercise to the reader.
```java
private static class GrpcHelloWorldDaprService extends DaprClientGrpc.DaprClientImplBase {
///...
        @Override
        public void onInvoke(DaprClientProtos.InvokeEnvelope request, StreamObserver<Any> responseObserver) {
          try {
            if ("say".equals(request.getMethod())) {
              SayRequest sayRequest =
                  SayRequest.newBuilder().setMessage(request.getData().getValue().toStringUtf8()).build();
              SayResponse sayResponse = this.say(sayRequest);
              CommonProtos.InvokeResponse.Builder responseBuilder = CommonProtos.InvokeResponse.newBuilder();
              responseBuilder.setData(Any.pack(sayResponse));
              responseObserver.onNext(responseBuilder.build());
            }
          } finally {
            responseObserver.onCompleted();
          }
        }
///...
}
```
In the `GrpcHelloWorldDaprService` class, the `onInvoke` method is the most important. It is called by Dapr's runtime containing information that this code needs to redirect the request to the correct underlying method. In this case, the only method supported is the `say` method. So, it checks for the method requested and builds the `SayRequest` object from Dapr's envelope request. Once a `SayResponse` instance is ready, it serializes it into Dapr's envelope response object and returns.

Now run the service code:

<!-- STEP
name: Run demo service
expected_stdout_lines:
  - '== APP == Server: "Message #0"'
  - '== APP == Server: "Message #1"'
background: true
sleep: 1
-->

```bash
dapr run --app-id hellogrpc --app-port 5000 --app-protocol grpc -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.invoke.grpc.HelloWorldService -p 5000
```

<!-- END_STEP -->

The `app-id` argument is used to identify this service in Dapr's runtime. The `app-port` determines which port Dapr's runtime should call into this service.  The `protocol` argument informs Dapr which protocol it should use to invoke the application: `grpc` or `http`(default).

### Running the example's client

The other component is the client. It will send one message per second to the service via Dapr's invoke API using Dapr's SDK. Open the `HelloWorldClient.java` file, it uses the Dapr's Java SDK to invoke the `say` method on the service above:

```java
private static class HelloWorldClient {
///...
  public static void main(String[] args) throws Exception {
    try (DaprClient client = new DaprClientBuilder().build()) {
    
      String serviceAppId = "hellogrpc";
      String method = "say";

      int count = 0;
      while (true) {
        String message = "Message #" + (count++);
        System.out.println("Sending message: " + message);
        client.invokeMethod(serviceAppId, method, message, HttpExtension.NONE).block();
        System.out.println("Message sent: " + message);

        Thread.sleep(1000);

        // This is an example, so for simplicity we are just exiting here.  
        // Normally a dapr app would be a web service and not exit main.
        System.out.println("Done");
      }
    }
  }
///...
}
```

First, it creates an instance of `DaprClient` via `DaprClientBuilder`. The protocol used by DaprClient is transparent to the application. The HTTP and GRPC ports used by Dapr's sidecar are automatically chosen and exported as environment variables: `DAPR_HTTP_PORT` and `DAPR_GRPC_PORT`. Dapr's Java SDK references these environment variables when communicating to Dapr's sidecar. The Dapr client is also within a try-with-resource block to properly close the client at the end.

Finally, it will go through in an infinite loop and invoke the `say` method every second. Notice the use of `block()` on the return from `invokeMethod` - it is required to actually make the service invocation via a [Mono](https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html) object.

Finally, open a new command line terminal and run the client code to send some messages.

<!-- STEP
name: Run demo client
expected_stdout_lines:
  - '== APP == Sending message: Message #0'
  - '== APP == Message sent: Message #0'
  - '== APP == Sending message: Message #1'
  - '== APP == Message sent: Message #1'
background: true
sleep: 10
-->

```bash
dapr run --app-id invokegrpc -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.invoke.grpc.HelloWorldClient
```

<!-- END_STEP -->

### Cleanup

To stop the apps run (or press CTRL+C):

<!-- STEP
name: Cleanup
-->

```bash
dapr stop --app-id hellogrpc
dapr stop --app-id invokegrpc
```

<!-- END_STEP -->

Thanks for playing.

