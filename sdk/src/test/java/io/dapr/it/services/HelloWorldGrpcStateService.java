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
import org.apache.commons.cli.*;


/**
 * Simple example, to run:
 * mvn clean install
 * dapr run --grpc-port 50001 -- mvn exec:java -pl=examples -Dexec.mainClass=io.dapr.examples.Example
 */
public class HelloWorldGrpcStateService {

    public static void main(String[] args) throws ParseException {
        Options options = new Options();
        options.addRequiredOption("grpcPort", "grpcPort", true, "Dapr GRPC.");
        options.addRequiredOption("httpPort", "httpPort", true, "Dapr HTTP port.");
        options.addRequiredOption("p", "port", true, "Port Dapr will listen to.");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        // If port string is not valid, it will throw an exception.
        int port = Integer.parseInt(cmd.getOptionValue("grpcPort"));
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
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
