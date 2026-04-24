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

import io.dapr.config.Properties;
import io.dapr.examples.OpenTelemetryConfig;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.sdk.OpenTelemetrySdk;

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
    OpenTelemetrySdk openTelemetrySdk = OpenTelemetryConfig.createOpenTelemetry();
    Tracer tracer = openTelemetrySdk.getTracer(InvokeClient.class.getCanonicalName());
    Span span = tracer.spanBuilder("Example's Main").setSpanKind(SpanKind.CLIENT).startSpan();

    int port = Properties.HTTP_PORT.get();
    String baseUrl = "http://localhost:" + port + "/v1.0/invoke/" + SERVICE_APP_ID + "/method/";

    HttpClient httpClient = HttpClient.newHttpClient();

    for (String message : args) {
      try (Scope scope = span.makeCurrent()) {
        // Call proxy_echo
        HttpRequest.Builder echoBuilder = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "proxy_echo"))
            .POST(HttpRequest.BodyPublishers.ofString(message));
        injectTraceContext(echoBuilder);
        addDaprApiToken(echoBuilder);
        HttpResponse<byte[]> echoResponse =
            httpClient.send(echoBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
        System.out.println(new String(echoResponse.body()));

        // Call proxy_sleep
        HttpRequest.Builder sleepBuilder = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "proxy_sleep"))
            .POST(HttpRequest.BodyPublishers.noBody());
        injectTraceContext(sleepBuilder);
        addDaprApiToken(sleepBuilder);
        httpClient.send(sleepBuilder.build(), HttpResponse.BodyHandlers.discarding());
      }
    }

    span.end();
    openTelemetrySdk.getSdkTracerProvider().shutdown();
    Validation.validate();
    System.out.println("Done");
    System.exit(0);
  }

  private static void injectTraceContext(HttpRequest.Builder builder) {
    TextMapSetter<HttpRequest.Builder> setter = HttpRequest.Builder::header;
    GlobalOpenTelemetry.getPropagators().getTextMapPropagator()
        .inject(Context.current(), builder, setter);
  }

  private static void addDaprApiToken(HttpRequest.Builder builder) {
    String token = Properties.API_TOKEN.get();
    if (token != null) {
      builder.header("dapr-api-token", token);
    }
  }
}
