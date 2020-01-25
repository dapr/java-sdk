/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.actors.services;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.dapr.DaprGrpc;
import io.dapr.DaprGrpc.DaprBlockingStub;
import io.dapr.DaprProtos.SaveStateEnvelope;
import io.dapr.DaprProtos.StateRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;


/**
 * Simple example.
 * To run manually, from repo root:
 * 1. mvn clean install
 * 2. dapr run --grpc-port 50001 -- mvn exec:java -Dexec.mainClass=io.dapr.it.services.HelloWorldGrpcStateService -Dexec.classpathScope="test"  -pl=sdk
 */
public class HelloWorldGrpcStateService {

  public static void main(String[] args) {
    String grpcPort = System.getenv("DAPR_GRPC_PORT");

    // If port string is not valid, it will throw an exception.
    int grpcPortInt = Integer.parseInt(grpcPort);
    ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", grpcPortInt).usePlaintext().build();
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
