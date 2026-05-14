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

package io.dapr.examples.invoke.http;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprInvokeHttpClient;
import io.dapr.config.Properties;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * 1. Build and install jars:
 * mvn clean install
 * 2. cd [repo root]/examples
 * 3. Send messages to the server:
 * dapr run -- java -jar target/dapr-java-sdk-examples-exec.jar \
 *   io.dapr.examples.invoke.http.InvokeClient 'message one' 'message two'
 *
 * <p>This example demonstrates calling another Dapr-enabled application over HTTP.
 * Two equivalent approaches are shown:
 * <ol>
 *   <li>{@link DaprClient#invokeHttpClient(String)} — an SDK-provided {@link java.net.http.HttpClient}
 *       wrapper pre-bound to the sidecar's {@code /v1.0/invoke/&lt;app-id&gt;/method/} prefix,
 *       with the {@code dapr-api-token} header attached when configured.</li>
 *   <li>A raw {@link java.net.http.HttpClient} sending the request to the sidecar's base URL
 *       with a {@code dapr-app-id} header identifying the target app — no SDK helper required.</li>
 * </ol>
 */
public class InvokeClient {

  /**
   * Identifier in Dapr for the service this client will invoke.
   */
  private static final String SERVICE_APP_ID = "invokedemo";

  /**
   * Method on the target service to invoke.
   */
  private static final String METHOD = "say";

  /**
   * Starts the invoke client.
   *
   * @param args Messages to be sent as request for the invoke API.
   */
  public static void main(String[] args) throws Exception {
    try (DaprClient daprClient = new DaprClientBuilder().build()) {
      DaprInvokeHttpClient invoker = daprClient.invokeHttpClient(SERVICE_APP_ID);

      int port = Properties.HTTP_PORT.get();
      String sidecarBase = "http://localhost:" + port;
      HttpClient rawHttpClient = HttpClient.newHttpClient();

      for (String message : args) {
        // Form 1: SDK helper — paths resolve against /v1.0/invoke/<app-id>/method/.
        HttpRequest sdkRequest = invoker.newRequestBuilder(METHOD)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(message))
            .build();
        HttpResponse<byte[]> sdkResponse =
            invoker.send(sdkRequest, HttpResponse.BodyHandlers.ofByteArray());
        System.out.println(new String(sdkResponse.body()));

        // Form 2: raw HttpClient + dapr-app-id header against the sidecar's base URL.
        HttpRequest headerRequest = HttpRequest.newBuilder()
            .uri(URI.create(sidecarBase + "/" + METHOD))
            .header("Content-Type", "application/json")
            .header("dapr-app-id", SERVICE_APP_ID)
            .POST(HttpRequest.BodyPublishers.ofString(message))
            .build();
        HttpResponse<byte[]> headerResponse =
            rawHttpClient.send(headerRequest, HttpResponse.BodyHandlers.ofByteArray());
        System.out.println(new String(headerResponse.body()));
      }
    }

    // This is an example, so for simplicity we are just exiting here.
    // Normally a dapr app would be a web service and not exit main.
    System.out.println("Done");
  }
}
