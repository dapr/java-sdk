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

package io.dapr.examples;

import io.dapr.examples.invoke.http.InvokeClient;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
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

@Configuration
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class OpenTelemetryConfig {

  private static final int ZIPKIN_PORT = 9411;

  private static final String ENDPOINT_V2_SPANS = "/api/v2/spans";

  @Bean
  public OpenTelemetry initOpenTelemetry() {
    return createOpenTelemetry();
  }

  @Bean
  public Tracer initTracer(@Autowired OpenTelemetry openTelemetry) {
    return openTelemetry.getTracer(io.dapr.examples.tracing.InvokeClient.class.getCanonicalName());
  }

  /**
   * Creates an opentelemetry instance.
   * @return OpenTelemetry.
   */
  public static OpenTelemetry createOpenTelemetry() {
    // Only exports to Zipkin if it is up. Otherwise, ignore it.
    // This is helpful to avoid exceptions for examples that do not require Zipkin.
    if (isZipkinUp()) {
      String httpUrl = String.format("http://localhost:%d", ZIPKIN_PORT);
      ZipkinSpanExporter zipkinExporter =
          ZipkinSpanExporter.builder()
              .setEndpoint(httpUrl + ENDPOINT_V2_SPANS)
              .setServiceName(InvokeClient.class.getName())
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
  public static reactor.util.context.ContextView getReactorContext() {
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
