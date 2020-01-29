/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.examples.invoke.http;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.client.domain.Verb;

/**
 * 1. Build and install jars:
 * mvn clean install
 * 2. Send messages to the server:
 * dapr run --port 3006 -- mvn exec:java -pl=examples -Dexec.mainClass=io.dapr.examples.invoke.http.InvokeClient -Dexec.args="'message one' 'message two'"
 */
public class InvokeClient {

  /**
   * Identifier in Dapr for the service this client will invoke.
   */
  private static final String SERVICE_APP_ID = "invokedemo";

  /**
   * Starts the invoke client.
   * @param args Messages to be sent as request for the invoke API.
   */
  public static void main(String[] args) {
    DaprClient client = (new DaprClientBuilder()).build();
    for (String message : args) {
      client.invokeService(Verb.POST, SERVICE_APP_ID, "say", message, null, String.class).block();
    }
  }
}
