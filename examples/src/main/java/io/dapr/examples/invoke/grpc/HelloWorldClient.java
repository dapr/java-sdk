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

package io.dapr.examples.invoke.grpc;

import java.util.concurrent.TimeUnit;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.HttpExtension;
import io.dapr.examples.DaprExamplesProtos.HelloReply;
import io.dapr.examples.DaprExamplesProtos.HelloRequest;
import io.dapr.examples.HelloWorldGrpc;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 1. Build and install jars:
 * mvn clean install
 * 2. cd [repo root]/examples
 * 2. Send messages to the server:
 * dapr run -- java -jar target/dapr-java-sdk-examples-exec.jar
 * io.dapr.examples.invoke.grpc.HelloWorldClient
 */
public class HelloWorldClient {

  private static final Logger logger = Logger.getLogger(HelloWorldClient.class.getName());

  /**
   * The main method of the client app.
   *
   * @param args Array of messages to be sent.
   */
  public static void main(String[] args) throws Exception {

    String user = "world";
    // Access a service running on the local machine on port 50051
    String target = "localhost:50051";
    // Allow passing in the user and target strings as command line arguments
    if (args.length > 0) {
      if ("--help".equals(args[0])) {
        System.err.println("Usage: [name [target]]");
        System.err.println("");
        System.err.println("  name    The name you wish to be greeted by. Defaults to " + user);
        System.err.println("  target  The server to connect to. Defaults to " + target);
        System.exit(1);
      }
      user = args[0];
    }
    if (args.length > 1) {
      target = args[1];
    }
    ManagedChannel channel = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create())
        .build();

    try {
      HelloWorldGrpc.HelloWorldBlockingStub blockingStub = HelloWorldGrpc.newBlockingStub(channel);

      Metadata headers = new Metadata();
      headers.put(Metadata.Key.of("dapr-app-id", Metadata.ASCII_STRING_MARSHALLER),
          "hellogrpc");
      blockingStub = MetadataUtils.attachHeaders(blockingStub, headers);

      logger.info("Will try to greet " + user + " ...");
      try {
        HelloRequest request = HelloRequest.newBuilder().setName(user).build();
        HelloReply response = blockingStub.sayHello(request);
        logger.info("Greeting: " + response.getMessage());
      } catch (StatusRuntimeException e) {
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
      }
    } finally {
      // ManagedChannels use resources like threads and TCP connections. To prevent
      // leaking these
      // resources the channel should be shut down when it will no longer be used. If
      // it may be used
      // again leave it running.
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

  }
}
