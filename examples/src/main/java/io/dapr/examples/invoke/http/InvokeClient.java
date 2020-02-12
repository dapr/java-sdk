/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.examples.invoke.http;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.Verb;

/**
 * 1. Build and install jars:
 * mvn clean install
 * 2. Send messages to the server:
 * dapr run --port 3006 -- java -jar examples/target/dapr-java-sdk-examples-exec.jar \
 *   io.dapr.examples.invoke.http.InvokeClient 'message one' 'message two'
 */
public class InvokeClient {

  /**
   * Identifier in Dapr for the service this client will invoke.
   */
  private static final String SERVICE_APP_ID = "invokedemo";

  /**
   * Starts the invoke client.
   *
   * @param args Messages to be sent as request for the invoke API.
   */
  public static void main(String[] args) {
    DaprClient client = (new DaprClientBuilder()).build();
    for (String message : args) {
      byte[] response = client.invokeService(
          Verb.POST, SERVICE_APP_ID, "say", message, null, byte[].class).block();
      System.out.println(new String(response));
    }
  }
}
