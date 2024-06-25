/*
 * Copyright 2021 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.it.state;

import com.google.protobuf.ByteString;
import io.dapr.config.Properties;
import io.dapr.v1.CommonProtos.StateItem;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprGrpc.DaprBlockingStub;
import io.dapr.v1.DaprProtos.SaveStateRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;


/**
 * Simple example.
 * To run manually, from repo root:
 * 1. mvn clean install
 * 2. dapr run --resources-path ./components --dapr-grpc-port 50001 -- mvn exec:java -Dexec.mainClass=io.dapr.it.state.HelloWorldGrpcStateService -Dexec.classpathScope="test"  -pl=sdk
 */
public class HelloWorldGrpcStateService {

  public static final String SUCCESS_MESSAGE = "Hello from " + HelloWorldGrpcStateService.class.getSimpleName();

  public static void main(String[] args) {
    String grpcPort = System.getenv("DAPR_GRPC_PORT");

    // If port string is not valid, it will throw an exception.
    int grpcPortInt = Integer.parseInt(grpcPort);
    ManagedChannel channel = ManagedChannelBuilder.forAddress(Properties.SIDECAR_IP.get(), grpcPortInt).usePlaintext().build();
    DaprBlockingStub client = DaprGrpc.newBlockingStub(channel);

    String key = "mykey";
    // First, write key-value pair.

    String value = "Hello World";
    StateItem req = StateItem
        .newBuilder()
        .setKey(key)
        .setValue(ByteString.copyFromUtf8(value))
        .build();
    SaveStateRequest state = SaveStateRequest.newBuilder()
        .setStoreName("statestore")
        .addStates(req)
        .build();
    client.saveState(state);
    System.out.println("Saved!");
    channel.shutdown();

    System.out.println(SUCCESS_MESSAGE);
  }
}
