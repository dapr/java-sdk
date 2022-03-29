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
import io.dapr.client.domain.HttpExtension;

/**
 * 1. Build and install jars:
 * mvn clean install
 * 2. cd [repo root]/examples
 * 2. Send messages to the server:
 * dapr run -- java -jar target/dapr-java-sdk-examples-exec.jar io.dapr.examples.invoke.grpc.HelloWorldClient
 */
public class HelloWorldClient {

  /**
   * The main method of the client app.
   *
   * @param args Array of messages to be sent.
   */
  public static void main(String[] args) throws Exception {
    try (DaprClient client = new DaprClientBuilder().build()) {

      String serviceAppId = "hellogrpc";
      String method = "say";

      int count = 0;
      while (true) {
        String message = "Message #" + (count++);
        System.out.println("Sending message: " + message);
        client.invokeMethod(serviceAppId, method, message, HttpExtension.NONE).block();
        System.out.println("Message sent: " + message);

        Thread.sleep(1000);

        // This is an example, so for simplicity we are just exiting here.
        // Normally a dapr app would be a web service and not exit main.
        System.out.println("Done");
      }
    }
  }
}
