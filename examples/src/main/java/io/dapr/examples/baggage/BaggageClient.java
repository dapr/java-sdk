/*
 * Copyright 2024 The Dapr Authors
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

package io.dapr.examples.baggage;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.Headers;
import io.dapr.client.domain.HttpExtension;
import io.dapr.utils.TypeRef;
import reactor.util.context.Context;

/**
 * Example demonstrating W3C Baggage propagation with the Dapr Java SDK.
 *
 * <p>Baggage allows propagating key-value pairs across service boundaries alongside
 * distributed traces. This is useful for passing contextual information (e.g., user IDs,
 * tenant IDs, feature flags) without modifying request payloads.
 *
 * <p>The Dapr runtime supports baggage propagation as defined by the
 * <a href="https://www.w3.org/TR/baggage/">W3C Baggage specification</a>.
 *
 * <h2>Usage</h2>
 * <ol>
 *   <li>Build and install jars: {@code mvn clean install}</li>
 *   <li>{@code cd [repo root]/examples}</li>
 *   <li>Start the target service:
 *     {@code dapr run --app-id target-service --app-port 3000 -- java -jar target/dapr-java-sdk-examples-exec.jar
 *     io.dapr.examples.invoke.http.DemoService -p 3000}</li>
 *   <li>Run the client:
 *     {@code dapr run -- java -jar target/dapr-java-sdk-examples-exec.jar
 *     io.dapr.examples.baggage.BaggageClient}</li>
 * </ol>
 */
public class BaggageClient {

  /**
   * The main method to run the baggage example.
   *
   * @param args command line arguments (unused).
   * @throws Exception on any error.
   */
  public static void main(String[] args) throws Exception {
    try (DaprClient client = new DaprClientBuilder().build()) {

      // Build the W3C Baggage header value.
      // Format: key1=value1,key2=value2
      // See https://www.w3.org/TR/baggage/#header-content
      String baggageValue = "userId=alice,tenantId=acme-corp,featureFlag=new-ui";

      System.out.println("Invoking service with baggage: " + baggageValue);

      // Propagate baggage via Reactor context.
      // The SDK automatically injects the "baggage" header into outgoing gRPC
      // and HTTP requests when present in the Reactor context.
      byte[] response = client.invokeMethod(
              "target-service",
              "say",
              "hello with baggage",
              HttpExtension.POST,
              null,
              byte[].class)
          .contextWrite(Context.of(Headers.BAGGAGE, baggageValue))
          .block();

      if (response != null) {
        System.out.println("Response: " + new String(response));
      }

      // You can also combine baggage with tracing context.
      System.out.println("\nInvoking service with baggage and tracing context...");
      response = client.invokeMethod(
              "target-service",
              "say",
              "hello with baggage and tracing",
              HttpExtension.POST,
              null,
              byte[].class)
          .contextWrite(Context.of(Headers.BAGGAGE, baggageValue)
              .put("traceparent", "00-0af7651916cd43dd8448eb211c80319c-b9c7c989f97918e1-01"))
          .block();

      if (response != null) {
        System.out.println("Response: " + new String(response));
      }

      System.out.println("Done.");
    }
  }
}
