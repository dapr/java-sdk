## Invoking a Grpc endpoint using the Java-SDK

In this example, you will run a Grpc service and client using Dapr's invoke feature.

## Pre-requisites

* [Dapr and Dapr Cli](https://github.com/dapr/docs/blob/master/getting-started/environment-setup.md#environment-setup).
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

In the `HelloWorldService.java` file, you will find the `HelloWorldService` class, containing the main method. The service implementation happens in the `GrpcHelloWorldDaprService` class. You can see that it extends `DaprClientImplBase` instead of `HelloWorldImplBase`. This is because this service will be called by Dapr, so it implements the service API expected by Dapr. The `DaprClientImplBase` class is part of this SDK. In a real-world application, the service would still implement its main API as well. The Dapr's API would be exposed as an additional service. In this example, we are implementing Dapr's API only. Modifying this example to expose `HelloWorldService` is offered as an exercise to the reader.
```java
private static class GrpcHelloWorldDaprService extends DaprClientGrpc.DaprClientImplBase {
///...
        @Override
        public void onInvoke(DaprClientProtos.InvokeEnvelope request, StreamObserver<Any> responseObserver) {
            try {
                if ("say".equals(request.getMethod())) {
                    // IMPORTANT: do not use Any.unpack(), use Type.ParseFrom() instead.
                    SayRequest sayRequest = SayRequest.parseFrom(request.getData().getValue());
                    SayResponse sayResponse = this.say(sayRequest);
                    responseObserver.onNext(Any.pack(sayResponse));
                }
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
                responseObserver.onError(e);
            } finally {
                responseObserver.onCompleted();
            }
        }
///...
}
```
In the `GrpcHelloWorldDaprService` class, the `onInvoke` method is the most important. It is called by Dapr's runtime containing information that this code needs to redirect the request to the correct underlying method. In this case, the only method supported is the `say` method. So, it checks for the method requested and extracts the `SayRequest` object from Dapr's envelope request. Once a `SayResponse` instance is ready, it serializes it into Dapr's envelope response object and returns.

Now run the service code:

```sh
dapr run --app-id hellogrpc --app-port 5000 --protocol grpc -- mvn exec:java -pl=examples -Dexec.mainClass=io.dapr.examples.invoke.grpc.HelloWorldService -Dexec.args="-p 5000"
```

The `app-id` argument is used to identify this service in Dapr's runtime. The `app-port` determines which port Dapr's runtime should call into this service.  The `protocol` argument informs Dapr which protocol it should use: `grpc` or `http`(default).

### Running the example's client

The other component is the client. It will take in the messages via command line arguments and send each one to the service via Dapr's invoke API over Grpc. Open the `HelloWorldClient.java` file, it contains the `HelloWorldClient` class with the main method and also the `GrpcHelloWorldDaprClient` class. The `GrpcHelloWorldDaprClient` encapsulates an instance of the `DaprFutureStub` class because it is calling Dapr's API. Creating a client to call `HelloWorldService` directly can be an exercise for the reader. In the `GrpcHelloWorldDaprClient` class, the most important method is `sendMessages`. See the code snippet below:

```java
private static class GrpcHelloWorldDaprClient {
///...
        private void sendMessages(String... messages) throws ExecutionException, InterruptedException, InvalidProtocolBufferException {
            List<ListenableFuture<InvokeServiceResponseEnvelope>> futureResponses = new ArrayList<>();
            for (String message : messages)
            {
                SayRequest request = SayRequest
                        .newBuilder()
                        .setMessage(message)
                        .build();

                // Now, wrap the request with Dapr's envelope.
                InvokeServiceEnvelope requestEnvelope = InvokeServiceEnvelope
                        .newBuilder()
                        .setId("hellogrpc")  // Service's identifier.
                        .setData(Any.pack(request))
                        .setMethod("say")  // The service's method to be invoked by Dapr.
                        .build();

                futureResponses.add(client.invokeService(requestEnvelope));
                System.out.println("Client: sent => " + message);
                Thread.sleep(TimeUnit.SECONDS.toMillis(10));
            }

            for (ListenableFuture<InvokeServiceResponseEnvelope> future : futureResponses) {
                Any data = future.get().getData();  // Blocks waiting for response.
                // IMPORTANT: do not use Any.unpack(), use Type.ParseFrom() instead.
                SayResponse response = SayResponse.parseFrom(data.getValue());
                System.out.println("Client: got response => " + response.getTimestamp());
            }
        }
///...
}
```

First, it goes through each message and creates a corresponding `SayRequest` object as if it would call the `HelloWorld` service directly. Then, the request object is wrapped into an instance of `InvokeServiceEnvelope`. As expected, the enveloped request is sent to Dapr's `invokeService` method. Once all responses are completed, they are unwrapped into `SayResponse` objects.

Finally, open a new command line terminal and run the client code to send some messages. Feel free to play with the command line to send different messages:

```sh
dapr run --protocol grpc --grpc-port 50001 -- mvn exec:java -pl=examples -Dexec.mainClass=io.dapr.examples.invoke.grpc.HelloWorldClient -Dexec.args="-p 50001 'message one' 'message two'"
```

Once the messages are sent, use `CTRL+C` to exit Dapr.

The `protocol` argument tells Dapr which protocol to use. In this command, `grpc-port` is specified so Dapr does not pick a random port and uses the requested port instead. The same port is passed in the client executable via the `p` argument. The last arguments into the Java's main method are the messages to be sent.

Thanks for playing.