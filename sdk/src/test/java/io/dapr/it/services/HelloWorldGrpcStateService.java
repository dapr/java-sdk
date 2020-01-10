/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.services;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.dapr.DaprGrpc;
import io.dapr.DaprGrpc.DaprBlockingStub;
import io.dapr.DaprProtos.SaveStateEnvelope;
import io.dapr.DaprProtos.StateRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Simple example, to run:
 * mvn clean install
 * dapr run --grpc-port 50001 -- mvn exec:java -pl=examples -Dexec.mainClass=io.dapr.examples.Example
 */
public class HelloWorldGrpcStateService {

    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50002).usePlaintext().build();
        DaprBlockingStub client = DaprGrpc.newBlockingStub(channel);

        String key = "mykey";
        // First, write key-value pair.

        String value = "Hello World";
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
        channel.shutdown();
    }
}
