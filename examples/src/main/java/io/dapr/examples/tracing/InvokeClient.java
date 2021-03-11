/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.examples.tracing;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.InvokeMethodRequest;
import io.dapr.client.domain.InvokeMethodRequestBuilder;
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
          InvokeMethodRequestBuilder builder = new InvokeMethodRequestBuilder(SERVICE_APP_ID, "proxy_echo");
          InvokeMethodRequest request = builder
              .withBody(message)
              .withHttpExtension(HttpExtension.POST)
              .build();
          client.invokeMethod(request, TypeRef.get(byte[].class))
              .map(r -> {
                System.out.println(new String(r));
                return r;
              })
              .flatMap(r -> {
                InvokeMethodRequest sleepRequest = new InvokeMethodRequestBuilder(SERVICE_APP_ID, "proxy_sleep")
                    .withHttpExtension(HttpExtension.POST)
                    .build();
                return client.invokeMethod(sleepRequest, TypeRef.get(Void.class));
              }).subscriberContext(getReactorContext()).block();
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
