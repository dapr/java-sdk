package io.dapr.examples;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;

import io.dapr.DaprGrpc;
import io.dapr.DaprProtos.DeleteStateEnvelope;
import io.dapr.DaprProtos.GetStateEnvelope;
import io.dapr.DaprProtos.PublishEventEnvelope;
import io.dapr.DaprProtos.SaveStateEnvelope;
import io.dapr.DaprProtos.StateRequest;
import io.grpc.ManagedChannelBuilder;

/**
 * Simple example, to run:
 * 
 * <code>
 *  mvn compile 
 *  dapr run --grpc-port 50001 -- mvn
 *  exec:java -Dexec.mainClass=io.dapr.examples.Example
 * </code>
 */
public class Example {
    public static void main(String[] args) {
        var channel = ManagedChannelBuilder.forAddress("localhost", 50001).usePlaintext().build();
        var client = DaprGrpc.newBlockingStub(channel);

        var data = Any.newBuilder().setValue(ByteString.copyFromUtf8("foo")).build();
        client.publishEvent(PublishEventEnvelope.newBuilder().setTopic("foo").setData(data).build());
        System.out.println("Published!");

        var key = "mykey";
        var req = StateRequest.newBuilder().setKey(key)
                .setValue(Any.newBuilder().setValue(ByteString.copyFromUtf8("my value")).build()).build();
        var state = SaveStateEnvelope.newBuilder().addRequests(req).build();
        client.saveState(state);
        System.out.println("Saved!");

        var get = client.getState(GetStateEnvelope.newBuilder().setKey(key).build());
        System.out.println("Got: " + get.getData().getValue().toStringUtf8());

        client.deleteState(DeleteStateEnvelope.newBuilder().setKey(key).build());
        System.out.println("Deleted!");
    }
}
