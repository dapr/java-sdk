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

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.examples.DaprExamplesProtos.HelloReply;
import io.dapr.examples.DaprExamplesProtos.HelloRequest;
import io.dapr.examples.HelloWorldGrpc;
import io.grpc.StatusRuntimeException;

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
    String user = "World";
    try (DaprClient client = new DaprClientBuilder().build()) {
      HelloWorldGrpc.HelloWorldBlockingStub blockingStub = client.newGrpcStub(
          "hellogrpc",
          channel -> HelloWorldGrpc.newBlockingStub(channel));

      logger.info("Will try to greet " + user + " ...");
      try {
        HelloRequest request = HelloRequest.newBuilder().setName(user).build();
        HelloReply response = blockingStub.sayHello(request);
        logger.info("Greeting: " + response.getMessage());
      } catch (StatusRuntimeException e) {
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
      }
    }
  }
}
