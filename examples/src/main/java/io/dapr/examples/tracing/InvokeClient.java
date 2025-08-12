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

package io.dapr.examples.tracing;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.InvokeMethodRequest;
import io.dapr.examples.OpenTelemetryConfig;
import io.dapr.utils.TypeRef;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;

import static io.dapr.examples.OpenTelemetryConfig.getReactorContext;

/**
 * 1. Build and install jars:
 * mvn clean install
 * 2. cd [repo root]/examples
 * 3. Send messages to the server:
 * dapr run -- java -jar target/dapr-java-sdk-examples-exec.jar \
 *   io.dapr.examples.tracing.InvokeClient 'message one' 'message two'
 */
public class InvokeClient {

  /**
   * Identifier in Dapr for the service this client will invoke.
   */
  private static final String SERVICE_APP_ID = "tracingdemoproxy";

  /**
   * Starts the invoke client.
   *
   * @param args Messages to be sent as request for the invoke API.
   */
  public static void main(String[] args) throws Exception {
    final OpenTelemetry openTelemetry = OpenTelemetryConfig.createOpenTelemetry();
    final Tracer tracer = openTelemetry.getTracer(InvokeClient.class.getCanonicalName());

    Span span = tracer.spanBuilder("Example's Main").setSpanKind(Span.Kind.CLIENT).startSpan();
    try (DaprClient client = (new DaprClientBuilder()).build()) {
      for (String message : args) {
        try (Scope scope = span.makeCurrent()) {
          InvokeMethodRequest request = new InvokeMethodRequest(SERVICE_APP_ID, "proxy_echo")
              .setBody(message)
              .setHttpExtension(HttpExtension.POST);
          client.invokeMethod(request, TypeRef.get(byte[].class))
              .map(r -> {
                System.out.println(new String(r));
                return r;
              })
              .flatMap(r -> {
                InvokeMethodRequest sleepRequest = new InvokeMethodRequest(SERVICE_APP_ID, "proxy_sleep")
                    .setHttpExtension(HttpExtension.POST);
                return client.invokeMethod(sleepRequest, TypeRef.get(Void.class));
              }).contextWrite(getReactorContext()).block();
        }
      }
    }
    span.end();
    shutdown();
    System.out.println("Done");
  }

  private static void shutdown() throws Exception {
    OpenTelemetrySdk.getGlobalTracerManagement().shutdown();
    Validation.validate();
  }

}
