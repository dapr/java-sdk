/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.internal.opencensus;

import javax.annotation.concurrent.Immutable;
import java.util.Arrays;

/**
 * A class that represents a span context. A span context contains the state that must propagate to
 * child Spans and across process boundaries. It contains the identifiers (a {@link TraceId
 * trace_id} and {@link SpanId span_id}) associated with the and a set of {@link
 * TraceOptions options}.
 *
 * <p>Code originally from https://github.com/census-instrumentation/opencensus-java/blob/
 * 446e9bde9b1f6c0317e3f310644997e5d6d5eab2/api/src/main/java/io/opencensus/trace/SpanContext.java</p>
 * @since 0.5
 */
@Immutable
final class SpanContext {

  private final TraceId traceId;

  private final SpanId spanId;

  private final TraceOptions traceOptions;

  private final Tracestate tracestate;

  /**
   * Creates a new {@code SpanContext} with the given identifiers and options.
   *
   * @param traceId      the trace identifier of the span context.
   * @param spanId       the span identifier of the span context.
   * @param traceOptions the trace options for the span context.
   * @param tracestate   the trace state for the span context.
   * @return a new {@code SpanContext} with the given identifiers and options.
   * @since 0.16
   */
  static SpanContext create(
      TraceId traceId, SpanId spanId, TraceOptions traceOptions, Tracestate tracestate) {
    return new SpanContext(traceId, spanId, traceOptions, tracestate);
  }

  /**
   * Returns the trace identifier associated with this {@code SpanContext}.
   *
   * @return the trace identifier associated with this {@code SpanContext}.
   * @since 0.5
   */
  TraceId getTraceId() {
    return traceId;
  }

  /**
   * Returns the span identifier associated with this {@code SpanContext}.
   *
   * @return the span identifier associated with this {@code SpanContext}.
   * @since 0.5
   */
  SpanId getSpanId() {
    return spanId;
  }

  /**
   * Returns the {@code TraceOptions} associated with this {@code SpanContext}.
   *
   * @return the {@code TraceOptions} associated with this {@code SpanContext}.
   * @since 0.5
   */
  TraceOptions getTraceOptions() {
    return traceOptions;
  }

  /**
   * Returns the {@code Tracestate} associated with this {@code SpanContext}.
   *
   * @return the {@code Tracestate} associated with this {@code SpanContext}.
   * @since 0.5
   */
  Tracestate getTracestate() {
    return tracestate;
  }

  /**
   * Returns true if this {@code SpanContext} is valid.
   *
   * @return true if this {@code SpanContext} is valid.
   * @since 0.5
   */
  boolean isValid() {
    return traceId.isValid() && spanId.isValid();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }

    if (obj == this) {
      return true;
    }

    if (!(obj instanceof SpanContext)) {
      return false;
    }

    SpanContext that = (SpanContext) obj;
    return traceId.equals(that.traceId)
        && spanId.equals(that.spanId)
        && traceOptions.equals(that.traceOptions);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(new Object[]{traceId, spanId, traceOptions});
  }

  private SpanContext(
      TraceId traceId, SpanId spanId, TraceOptions traceOptions, Tracestate tracestate) {
    this.traceId = traceId;
    this.spanId = spanId;
    this.traceOptions = traceOptions;
    this.tracestate = tracestate;
  }
}