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
import io.dapr.client.domain.HttpExtension;

/**
 * 1. Build and install jars:
 * mvn clean install
 * 2. cd [repo root]/examples
 * 3. Send messages to the server:
 * dapr run -- java -jar target/dapr-java-sdk-examples-exec.jar \
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
  public static void main(String[] args) throws Exception {
    try (DaprClient client = (new DaprClientBuilder()).build()) {
      for (String message : args) {
        byte[] response = client.invokeMethod(SERVICE_APP_ID, "say", message, HttpExtension.POST, null,
            byte[].class).block();
        System.out.println(new String(response));
      }

      // This is an example, so for simplicity we are just exiting here.
      // Normally a dapr app would be a web service and not exit main.
      System.out.println("Done");
    }
  }
}
