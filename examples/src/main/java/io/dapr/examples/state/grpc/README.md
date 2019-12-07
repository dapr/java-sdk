## State management via Dapr's Grpc endpoint using the Java-SDK

This example shows how to write and read data on Dapr via Grpc. 

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

### Understanding the code

Open the file `Example.java`, it contains the client to communicate to Dapr's runtime. In this example, we will be using port 50001 and use the blocking (instead of asynchronous) client:
```
        ManagedChannel channel =
                ManagedChannelBuilder.forAddress("localhost", 50001).usePlaintext().build();
        DaprBlockingStub client = DaprGrpc.newBlockingStub(channel);
``` 

The code has 3 parts: save, read and delete a key-value pair.

First, save a key-value pair to the state store using the `saveState` method.
```
            StateRequest req = StateRequest
                    .newBuilder()
                    .setKey(key)
                    .setValue(Any.newBuilder().setValue(ByteString.copyFromUtf8(value)).build())
                    .build();
            SaveStateEnvelope state = SaveStateEnvelope.newBuilder()
                    .addRequests(req)
                    .build();
            client.saveState(state);
```

Then, read it:
```
            GetStateEnvelope req = GetStateEnvelope
                    .newBuilder()
                    .setKey(key)
                    .build();
            GetStateResponseEnvelope response = client.getState(req);
            String value = response.getData().getValue().toStringUtf8();
```

In the end, delete it:
```
            DeleteStateEnvelope req = DeleteStateEnvelope
                    .newBuilder()
                    .setKey(key)
                    .build();
            client.deleteState(req);
```

### Running the example

Finally, run this example with the following command:
```sh
dapr run --grpc-port 50001 -- mvn exec:java -pl=examples -Dexec.mainClass=io.dapr.examples.state.grpc.Example
```

To find more features available in the Dapr's Grpc API, see [dapr.proto](../../../../../../../../../proto/dapr/dapr.proto).

Thanks for playing.