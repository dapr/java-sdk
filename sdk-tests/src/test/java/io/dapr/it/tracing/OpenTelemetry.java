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

package io.dapr.it.tracing;

import io.dapr.utils.NetworkUtils;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

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
  public static io.opentelemetry.api.OpenTelemetry createOpenTelemetry(String serviceName) throws InterruptedException {
    waitForZipkin();
    String httpUrl = String.format("http://localhost:%d", ZIPKIN_PORT);
    ZipkinSpanExporter zipkinExporter = ZipkinSpanExporter.builder().setEndpoint(httpUrl + ENDPOINT_V2_SPANS).build();

    SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(SimpleSpanProcessor.create(zipkinExporter))
        .build();

    return OpenTelemetrySdk.builder()
        .setTracerProvider(sdkTracerProvider)
        .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
        .build();
  }

  /**
   * Converts current OpenTelemetry's context into Reactor's context.
   * @return Reactor's context.
   */
  public static reactor.util.context.Context getReactorContext(io.opentelemetry.api.OpenTelemetry openTelemetry) {
    return getReactorContext(openTelemetry, Context.current());
  }

  /**
   * Converts given OpenTelemetry's context into Reactor's context.
   * @param context OpenTelemetry's context.
   * @return Reactor's context.
   */
  public static reactor.util.context.Context getReactorContext(io.opentelemetry.api.OpenTelemetry openTelemetry,
      Context context) {
    Map<String, String> map = new HashMap<>();
    TextMapSetter<Map<String, String>> setter = (carrier, key, value) -> map.put(key, value);

    openTelemetry.getPropagators().getTextMapPropagator().inject(context, map, setter);
    reactor.util.context.Context reactorContext = reactor.util.context.Context.empty();
    for (Map.Entry<String, String> entry : map.entrySet()) {
      reactorContext = reactorContext.put(entry.getKey(), entry.getValue());
    }
    return reactorContext;
  }

  private static void waitForZipkin() throws InterruptedException {
    NetworkUtils.waitForSocket("127.0.0.1", ZIPKIN_PORT, 10000);
  }
}
