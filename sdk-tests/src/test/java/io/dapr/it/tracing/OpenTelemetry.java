/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.it.tracing;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class OpenTelemetry {

  private static final int ZIPKIN_PORT = 9411;

  private static final String ENDPOINT_V2_SPANS = "/api/v2/spans";

  /**
   * Creates an opentelemetry instance.
   * @param serviceName Name of the service in Zipkin
   * @return OpenTelemetry.
   */
  public static io.opentelemetry.api.OpenTelemetry createOpenTelemetry(String serviceName) {
    // Only exports to Zipkin if it is up. Otherwise, ignore it.
    // This is helpful to avoid exceptions for examples that do not require Zipkin.
    if (isZipkinUp()) {
      String httpUrl = String.format("http://localhost:%d", ZIPKIN_PORT);
      ZipkinSpanExporter zipkinExporter =
          ZipkinSpanExporter.builder()
              .setEndpoint(httpUrl + ENDPOINT_V2_SPANS)
              .setServiceName(serviceName)
              .build();

      SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
          .addSpanProcessor(SimpleSpanProcessor.create(zipkinExporter))
          .build();

      return OpenTelemetrySdk.builder()
          .setTracerProvider(sdkTracerProvider)
          .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
          .buildAndRegisterGlobal();
    } else {
      System.out.println("WARNING: Zipkin is not available.");
    }

    return null;
  }

  /**
   * Converts current OpenTelemetry's context into Reactor's context.
   * @return Reactor's context.
   */
  public static reactor.util.context.Context getReactorContext() {
    return getReactorContext(Context.current());
  }

  /**
   * Converts given OpenTelemetry's context into Reactor's context.
   * @param context OpenTelemetry's context.
   * @return Reactor's context.
   */
  public static reactor.util.context.Context getReactorContext(Context context) {
    Map<String, String> map = new HashMap<>();
    TextMapPropagator.Setter<Map<String, String>> setter =
        (carrier, key, value) -> map.put(key, value);

    GlobalOpenTelemetry.getPropagators().getTextMapPropagator().inject(context, map, setter);
    reactor.util.context.Context reactorContext = reactor.util.context.Context.empty();
    for (Map.Entry<String, String> entry : map.entrySet()) {
      reactorContext = reactorContext.put(entry.getKey(), entry.getValue());
    }
    return reactorContext;
  }

  private static boolean isZipkinUp() {
    try (Socket ignored = new Socket("localhost", ZIPKIN_PORT)) {
      return true;
    } catch (IOException ignored) {
      return false;
    }
  }
}
