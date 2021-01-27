/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.examples;

import io.dapr.examples.invoke.http.InvokeClient;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.HttpTraceContext;
import io.opentelemetry.context.propagation.DefaultContextPropagators;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.Socket;

@Configuration
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class OpenTelemetryConfig {

  private static final int ZIPKIN_PORT = 9411;

  private static final String ENDPOINT_V2_SPANS = "/api/v2/spans";

  @Bean
  public Tracer initTracer() throws Exception {
    return createTracer("io.dapr.examples");
  }

  /**
   * Creates an OpenTelemetry's tracer.
   * @param instrumentationName Name of the instrumentation.
   * @return New tracer's instance.
   */
  public static Tracer createTracer(String instrumentationName) {
    OpenTelemetry.setGlobalPropagators(
        DefaultContextPropagators.builder()
            .addTextMapPropagator(HttpTraceContext.getInstance())
            .build());
    final Tracer tracer = OpenTelemetry.getGlobalTracer(instrumentationName);

    // Only exports to Zipkin if it is up. Otherwise, ignore it.
    // This is helpful to avoid exceptions for examples that do not require Zipkin.
    if (isZipkinUp()) {
      String httpUrl = String.format("http://localhost:%d", ZIPKIN_PORT);
      ZipkinSpanExporter zipkinExporter =
          ZipkinSpanExporter.builder()
              .setEndpoint(httpUrl + ENDPOINT_V2_SPANS)
              .setServiceName(InvokeClient.class.getName())
              .build();

      OpenTelemetrySdk.getGlobalTracerManagement()
          .addSpanProcessor(SimpleSpanProcessor.builder(zipkinExporter).build());
    } else {
      System.out.println("WARNING: Zipkin is not available.");
    }

    return tracer;
  }

  private static boolean isZipkinUp() {
    try (Socket ignored = new Socket("localhost", ZIPKIN_PORT)) {
      return true;
    } catch (IOException ignored) {
      return false;
    }
  }
}
