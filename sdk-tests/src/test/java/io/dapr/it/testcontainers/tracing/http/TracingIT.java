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
 * limitations under the License.
 */
package io.dapr.it.testcontainers.tracing.http;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import io.dapr.it.testcontainers.support.DaprTestEnvironment;
import io.dapr.it.testcontainers.support.Endpoints;
import io.dapr.it.testcontainers.tracing.http.app.OpenTelemetryConfig;
import io.dapr.it.testcontainers.tracing.http.app.Service;
import io.dapr.testcontainers.Configuration;
import io.dapr.testcontainers.TracingConfigurationSettings;
import io.dapr.testcontainers.ZipkinTracingConfigurationSettings;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.dapr.it.testcontainers.tracing.TraceAssertions.awaitTrace;
import static io.dapr.it.tracing.OpenTelemetry.getReactorContext;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exercises trace propagation through a real HTTP invocation. */
@Testcontainers
@Tag("testcontainers")
public class TracingIT {
  private static final String APP_NAME = "tracing-http-it";
  private static final int ZIPKIN_PORT = 9411;
  private static final String CHILD_NAME = "calllocal/" + APP_NAME + "/sleep";
  private static final GenericContainer<?> ZIPKIN = new GenericContainer<>("openzipkin/zipkin:3.4")
      .withExposedPorts(ZIPKIN_PORT)
      .waitingFor(Wait.forHttp("/health").forPort(ZIPKIN_PORT).forStatusCode(200)
          .withStartupTimeout(Duration.ofMinutes(2)));

  @Container
  private static final DaprTestEnvironment ENVIRONMENT = new DaprTestEnvironment(APP_NAME, Service.class)
      .withPrerequisite(ZIPKIN, "zipkin", List.of())
      .withConfiguration(new Configuration("tracing", new TracingConfigurationSettings("1", true, null,
          new ZipkinTracingConfigurationSettings("http://zipkin:9411/api/v2/spans")), null));

  @Test
  public void testInvoke() throws Exception {
    URI zipkin = Endpoints.http(ZIPKIN.getHost(), ZIPKIN.getMappedPort(ZIPKIN_PORT));
    String spanName = UUID.randomUUID().toString();
    try (SdkTracerProvider provider = SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(
        ZipkinSpanExporter.builder().setEndpoint(zipkin.resolve("/api/v2/spans").toString()).build())).build()) {
      OpenTelemetrySdk telemetry = OpenTelemetrySdk.builder().setTracerProvider(provider)
          .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance())).build();
      Span span = telemetry.getTracer(OpenTelemetryConfig.TRACER_NAME).spanBuilder(spanName)
          .setSpanKind(SpanKind.CLIENT).startSpan();
      try {
        try (DaprClient client = ENVIRONMENT.clientBuilder().build(); Scope scope = span.makeCurrent()) {
          client.invokeMethod(APP_NAME, "sleep", 1, HttpExtension.POST)
              .contextWrite(getReactorContext(telemetry)).block(Duration.ofSeconds(30));
        }
      } finally {
        span.end();
      }

      CompletableResultCode flushed = provider.forceFlush().join(10, TimeUnit.SECONDS);
      assertTrue(flushed.isSuccess(), "Client span export did not flush within 10 seconds");
      awaitTrace(zipkin, span.getSpanContext().getTraceId(), span.getSpanContext().getSpanId(), spanName, CHILD_NAME);
    }
  }
}
