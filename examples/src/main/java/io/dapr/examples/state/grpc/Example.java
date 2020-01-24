package io.dapr.examples.state.grpc;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.dapr.DaprGrpc;
import io.dapr.DaprGrpc.DaprBlockingStub;
import io.dapr.DaprProtos.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.UUID;

/**
 * Simple example, to run:
 * mvn clean install
 * dapr run --grpc-port 50001 -- mvn exec:java -pl=examples -Dexec.mainClass=io.dapr.examples.Example
 */
public class Example {
  public static void main(String[] args) {
    ManagedChannel channel =
      ManagedChannelBuilder.forAddress("localhost", 50001).usePlaintext().build();
    DaprBlockingStub client = DaprGrpc.newBlockingStub(channel);

    String key = "mykey";
    // First, write key-value pair.
    {
      String value = UUID.randomUUID().toString();
      StateRequest req = StateRequest
        .newBuilder()
        .setKey(key)
        .setValue(Any.newBuilder().setValue(ByteString.copyFromUtf8(value)).build())
        .build();
      SaveStateEnvelope state = SaveStateEnvelope.newBuilder()
        .addRequests(req)
        .build();
      client.saveState(state);
      System.out.println("Saved!");
    }

    // Now, read it back.
    {
      GetStateEnvelope req = GetStateEnvelope
        .newBuilder()
        .setKey(key)
        .build();
      GetStateResponseEnvelope response = client.getState(req);
      String value = response.getData().getValue().toStringUtf8();
      System.out.println("Got: " + value);
    }

    // Then, delete it.
    {
      DeleteStateEnvelope req = DeleteStateEnvelope
        .newBuilder()
        .setKey(key)
        .build();
      client.deleteState(req);
      System.out.println("Deleted!");
    }
  }
}
