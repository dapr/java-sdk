/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.internal.opencensus;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Code originally from https://github.com/census-instrumentation/opencensus-java/blob/
 *   446e9bde9b1f6c0317e3f310644997e5d6d5eab2/impl_core/src/main/java/io/opencensus/
 *   implcore/trace/propagation/BinaryFormatImpl.java
 */
final class BinaryFormatImpl {

  private static final byte VERSION_ID = 0;

  private static final int VERSION_ID_OFFSET = 0;

  // The version_id/field_id size in bytes.
  private static final byte ID_SIZE = 1;

  private static final byte TRACE_ID_FIELD_ID = 0;

  // TODO: clarify if offsets are correct here. While the specification suggests you should stop
  // parsing when you hit an unknown field, it does not suggest that fields must be declared in
  // ID order. Rather it only groups by data type order, in this case Trace Context
  // https://github.com/census-instrumentation/opencensus-specs/blob/master/encodings/BinaryEncoding
  // .md#deserialization-rules
  private static final int TRACE_ID_FIELD_ID_OFFSET = VERSION_ID_OFFSET + ID_SIZE;

  private static final int TRACE_ID_OFFSET = TRACE_ID_FIELD_ID_OFFSET + ID_SIZE;

  private static final byte SPAN_ID_FIELD_ID = 1;

  private static final int SPAN_ID_FIELD_ID_OFFSET = TRACE_ID_OFFSET + TraceId.SIZE;

  private static final int SPAN_ID_OFFSET = SPAN_ID_FIELD_ID_OFFSET + ID_SIZE;

  private static final byte TRACE_OPTION_FIELD_ID = 2;

  private static final int TRACE_OPTION_FIELD_ID_OFFSET = SPAN_ID_OFFSET + SpanId.SIZE;

  private static final int TRACE_OPTIONS_OFFSET = TRACE_OPTION_FIELD_ID_OFFSET + ID_SIZE;

  /**
   * Version, Trace and Span IDs are required fields.
   */
  private static final int REQUIRED_FORMAT_LENGTH = 3 * ID_SIZE + TraceId.SIZE + SpanId.SIZE;

  private static final int ALL_FORMAT_LENGTH = REQUIRED_FORMAT_LENGTH + ID_SIZE + TraceOptions.SIZE;

  /**
   * Generates the byte array for a span context.
   * @param spanContext OpenCensus' span context.
   * @return byte array for span context.
   */
  byte[] toByteArray(SpanContext spanContext) {
    checkNotNull(spanContext, "spanContext");
    byte[] bytes = new byte[ALL_FORMAT_LENGTH];
    bytes[VERSION_ID_OFFSET] = VERSION_ID;
    bytes[TRACE_ID_FIELD_ID_OFFSET] = TRACE_ID_FIELD_ID;
    spanContext.getTraceId().copyBytesTo(bytes, TRACE_ID_OFFSET);
    bytes[SPAN_ID_FIELD_ID_OFFSET] = SPAN_ID_FIELD_ID;
    spanContext.getSpanId().copyBytesTo(bytes, SPAN_ID_OFFSET);
    bytes[TRACE_OPTION_FIELD_ID_OFFSET] = TRACE_OPTION_FIELD_ID;
    spanContext.getTraceOptions().copyBytesTo(bytes, TRACE_OPTIONS_OFFSET);
    return bytes;
  }

}
