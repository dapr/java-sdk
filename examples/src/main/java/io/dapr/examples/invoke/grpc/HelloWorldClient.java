/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.examples.invoke.grpc;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.Verb;

/**
 * 1. Build and install jars:
 * mvn clean install
 * 2. Send messages to the server:
 * dapr run -- java -jar examples/target/dapr-java-sdk-examples-exec.jar \
 * io.dapr.examples.invoke.grpc.HelloWorldClient
 */
public class HelloWorldClient {

  /**
   * The main method of the client app.
   *
   * @param args Array of messages to be sent.
   */
  public static void main(String[] args) throws InterruptedException {
    DaprClient client = new DaprClientBuilder().build();

    String serviceAppId = "hellogrpc";
    String method = "say";

    int count = 0;
    while (true) {
      String message = "Message #" + (count++);
      System.out.println("Sending message: " + message);
      client.invokeService(Verb.POST, serviceAppId, method, message).block();
      System.out.println("Message sent: " + message);

      Thread.sleep(1000);
      
      // This is an example, so for simplicity we are just exiting here.  
      // Normally a dapr app would be a web service and not exit main.
      System.out.println("Done");
    }
  }
}
