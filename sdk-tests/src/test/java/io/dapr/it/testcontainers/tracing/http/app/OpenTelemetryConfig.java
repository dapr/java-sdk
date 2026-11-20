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
package io.dapr.it.testcontainers.tracing.http.app;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Telemetry beans for the HTTP tracing fixture. */
@Configuration
public class OpenTelemetryConfig {
  public static final String TRACER_NAME = "integration testing tracer";

  @Bean(destroyMethod = "close")
  public SdkTracerProvider tracerProvider() {
    return SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(
        ZipkinSpanExporter.builder().setEndpoint("http://zipkin:9411/api/v2/spans").build())).build();
  }

  @Bean(destroyMethod = "")
  public OpenTelemetry initOpenTelemetry(SdkTracerProvider provider) {
    return OpenTelemetrySdk.builder().setTracerProvider(provider)
        .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance())).build();
  }

  @Bean
  public Tracer initTracer(OpenTelemetry openTelemetry) {
    return openTelemetry.getTracer(TRACER_NAME);
  }
}
