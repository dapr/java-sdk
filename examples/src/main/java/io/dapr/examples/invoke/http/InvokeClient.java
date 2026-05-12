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
 * <p>This example demonstrates calling another Dapr-enabled application using a
 * native HTTP client. Two equivalent URL forms are supported by the Dapr sidecar:
 * <ol>
 *   <li>Sending the request to the sidecar's base URL with a {@code dapr-app-id}
 *       header that identifies the target app.</li>
 *   <li>Sending the request to the sidecar's {@code /v1.0/invoke/&lt;app-id&gt;/method/&lt;method&gt;}
 *       path, with no extra header.</li>
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
    int port = Properties.HTTP_PORT.get();
    String sidecarBase = "http://localhost:" + port;

    HttpClient httpClient = HttpClient.newHttpClient();

    for (String message : args) {
      // Form 1: dapr-app-id header against the sidecar's base URL.
      HttpRequest headerRequest = HttpRequest.newBuilder()
          .uri(URI.create(sidecarBase + "/" + METHOD))
          .header("Content-Type", "application/json")
          .header("dapr-app-id", SERVICE_APP_ID)
          .POST(HttpRequest.BodyPublishers.ofString(message))
          .build();
      HttpResponse<byte[]> headerResponse =
          httpClient.send(headerRequest, HttpResponse.BodyHandlers.ofByteArray());
      System.out.println(new String(headerResponse.body()));

      // Form 2: sidecar invoke path.
      HttpRequest pathRequest = HttpRequest.newBuilder()
          .uri(URI.create(sidecarBase + "/v1.0/invoke/" + SERVICE_APP_ID + "/method/" + METHOD))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(message))
          .build();
      HttpResponse<byte[]> pathResponse =
          httpClient.send(pathRequest, HttpResponse.BodyHandlers.ofByteArray());
      System.out.println(new String(pathResponse.body()));
    }

    // This is an example, so for simplicity we are just exiting here.
    // Normally a dapr app would be a web service and not exit main.
    System.out.println("Done");
  }
}
