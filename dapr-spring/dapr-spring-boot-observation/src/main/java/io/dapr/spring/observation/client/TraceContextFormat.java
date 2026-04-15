/*
 * Copyright 2025 The Dapr Authors
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

package io.dapr.spring.observation.client;

import io.opentelemetry.api.trace.SpanContext;

/**
 * Formats an OpenTelemetry {@link SpanContext} as the W3C Trace Context header values
 * ({@code traceparent} and {@code tracestate}).
 *
 * <p>Package-private: shared between {@link ObservationDaprClient} (Reactor context for async
 * gRPC calls on {@code DaprClient}) and {@link ObservationDaprWorkflowClient} (gRPC
 * {@code ClientInterceptor} for synchronous workflow calls). This keeps a single source of truth
 * for the on-wire trace format.
 */
final class TraceContextFormat {

  private TraceContextFormat() {
  }

  /**
   * Format a {@link SpanContext} as the value of the W3C {@code traceparent} header (version 00).
   */
  static String formatW3cTraceparent(SpanContext spanCtx) {
    return "00-" + spanCtx.getTraceId() + "-" + spanCtx.getSpanId()
        + "-" + spanCtx.getTraceFlags().asHex();
  }

  /**
   * Format a {@link SpanContext}'s trace state as the value of the W3C {@code tracestate} header.
   * Returns an empty string if the trace state is empty.
   */
  static String formatTraceState(SpanContext spanCtx) {
    if (spanCtx.getTraceState().isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    spanCtx.getTraceState().forEach((k, v) -> {
      if (sb.length() > 0) {
        sb.append(',');
      }
      sb.append(k).append('=').append(v);
    });
    return sb.toString();
  }
}
