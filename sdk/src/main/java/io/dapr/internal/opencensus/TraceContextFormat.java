/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.internal.opencensus;

import com.google.common.base.Splitter;
import io.grpc.Metadata;

import java.util.List;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Implementation of the TraceContext propagation protocol. See <a
 * href=https://github.com/w3c/distributed-tracing>w3c/distributed-tracing</a>.
 *
 * <p>Code originally from https://github.com/census-instrumentation/opencensus-java/blob/
 * 446e9bde9b1f6c0317e3f310644997e5d6d5eab2/impl_core/src/main/java/io/opencensus/implcore/
 * trace/propagation/TraceContextFormat.java</p>
 */
class TraceContextFormat {

  private static final Tracestate TRACESTATE_DEFAULT = Tracestate.builder().build();
  private static final String TRACEPARENT = "traceparent";
  private static final String TRACESTATE = "tracestate";

  private static final Metadata.Key<String> TRACEPARENT_KEY =
      Metadata.Key.of(TRACEPARENT, Metadata.ASCII_STRING_MARSHALLER);
  private static final Metadata.Key<String> TRACESTATE_KEY =
      Metadata.Key.of(TRACESTATE, Metadata.ASCII_STRING_MARSHALLER);

  private static final int VERSION_SIZE = 2;
  private static final char TRACEPARENT_DELIMITER = '-';
  private static final int TRACEPARENT_DELIMITER_SIZE = 1;
  private static final int TRACE_ID_HEX_SIZE = 2 * TraceId.SIZE;
  private static final int SPAN_ID_HEX_SIZE = 2 * SpanId.SIZE;
  private static final int TRACE_OPTION_HEX_SIZE = 2 * TraceOptions.SIZE;
  private static final int TRACE_ID_OFFSET = VERSION_SIZE + TRACEPARENT_DELIMITER_SIZE;
  private static final int SPAN_ID_OFFSET =
      TRACE_ID_OFFSET + TRACE_ID_HEX_SIZE + TRACEPARENT_DELIMITER_SIZE;
  private static final int TRACE_OPTION_OFFSET =
      SPAN_ID_OFFSET + SPAN_ID_HEX_SIZE + TRACEPARENT_DELIMITER_SIZE;
  private static final int TRACEPARENT_HEADER_SIZE = TRACE_OPTION_OFFSET + TRACE_OPTION_HEX_SIZE;
  private static final int TRACESTATE_MAX_MEMBERS = 32;
  private static final char TRACESTATE_KEY_VALUE_DELIMITER = '=';
  private static final char TRACESTATE_ENTRY_DELIMITER = ',';
  private static final Splitter TRACESTATE_ENTRY_DELIMITER_SPLITTER =
      Splitter.on(Pattern.compile("[ \t]*" + TRACESTATE_ENTRY_DELIMITER + "[ \t]*"));

  /**
   * Extracts span context from gRPC's metadata.
   * @param metadata gRPC's metadata.
   * @return span context.
   */
  static SpanContext extract(Metadata metadata) {
    String traceparent = metadata.get(TRACEPARENT_KEY);
    if (traceparent == null) {
      throw new RuntimeException("Traceparent not present");
    }

    checkArgument(
        traceparent.charAt(TRACE_OPTION_OFFSET - 1) == TRACEPARENT_DELIMITER
            && (traceparent.length() == TRACEPARENT_HEADER_SIZE
            || (traceparent.length() > TRACEPARENT_HEADER_SIZE
            && traceparent.charAt(TRACEPARENT_HEADER_SIZE) == TRACEPARENT_DELIMITER))
            && traceparent.charAt(SPAN_ID_OFFSET - 1) == TRACEPARENT_DELIMITER
            && traceparent.charAt(TRACE_OPTION_OFFSET - 1) == TRACEPARENT_DELIMITER,
        "Missing or malformed TRACEPARENT.");

    TraceId traceId = TraceId.fromLowerBase16(traceparent, TRACE_ID_OFFSET);
    SpanId spanId = SpanId.fromLowerBase16(traceparent, SPAN_ID_OFFSET);
    TraceOptions traceOptions = TraceOptions.fromLowerBase16(traceparent, TRACE_OPTION_OFFSET);

    String tracestate = metadata.get(TRACESTATE_KEY);
    if (tracestate == null || tracestate.isEmpty()) {
      return SpanContext.create(traceId, spanId, traceOptions, TRACESTATE_DEFAULT);
    }
    Tracestate.Builder tracestateBuilder = Tracestate.builder();
    List<String> listMembers = TRACESTATE_ENTRY_DELIMITER_SPLITTER.splitToList(tracestate);
    checkArgument(
        listMembers.size() <= TRACESTATE_MAX_MEMBERS, "Tracestate has too many elements.");
    // Iterate in reverse order because when call builder set the elements is added in the
    // front of the list.
    for (int i = listMembers.size() - 1; i >= 0; i--) {
      String listMember = listMembers.get(i);
      int index = listMember.indexOf(TRACESTATE_KEY_VALUE_DELIMITER);
      checkArgument(index != -1, "Invalid tracestate list-member format.");
      tracestateBuilder.set(
          listMember.substring(0, index), listMember.substring(index + 1, listMember.length()));
    }
    return SpanContext.create(traceId, spanId, traceOptions, tracestateBuilder.build());
  }
}
