/*
 * Copyright 2026 The Dapr Authors
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

package io.dapr.client;

import com.google.protobuf.ListValue;
import com.google.protobuf.NullValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.rpc.Status;
import io.dapr.client.domain.FailedItem;
import io.dapr.client.domain.IndexAck;
import io.dapr.client.domain.IndexingOptions;
import io.dapr.client.domain.IndexingWaitTimeoutAction;
import io.dapr.exceptions.DaprErrorDetails;
import io.dapr.exceptions.DaprException;
import io.dapr.v1.DaprSearchProtos;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Validation and conversions shared by the search and vector building blocks (alpha).
 */
final class SearchVectorHelper {

  private SearchVectorHelper() {
  }

  static void requireRequest(Object request) {
    if (request == null) {
      throw new IllegalArgumentException("Request cannot be null.");
    }
  }

  static void validateStoreName(String storeName) {
    if (isBlank(storeName)) {
      throw new IllegalArgumentException("Store name cannot be null or empty.");
    }
  }

  static void validateIds(List<String> ids) {
    if (ids == null) {
      throw new IllegalArgumentException("IDs cannot be null.");
    }
    for (String id : ids) {
      if (isBlank(id)) {
        throw new IllegalArgumentException("IDs cannot contain a null or empty ID.");
      }
    }
  }

  static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  static void putMetadata(Consumer<Map<String, String>> target, Map<String, String> metadata) {
    if (metadata != null) {
      target.accept(metadata);
    }
  }

  static DaprSearchProtos.IndexingOptionsAlpha1 toIndexingOptions(IndexingOptions options) {
    if (!options.isWaitForCompletion()) {
      return DaprSearchProtos.IndexingOptionsAlpha1.newBuilder()
          .setMode(DaprSearchProtos.IndexingMode.INDEXING_MODE_RETURN_ON_ACCEPTANCE)
          .build();
    }

    Duration waitTimeout = options.getWaitTimeout();
    if (waitTimeout == null || waitTimeout.isZero() || waitTimeout.isNegative()) {
      throw new IllegalArgumentException("Waiting for completion needs a positive wait timeout.");
    }
    if (options.getOnWaitTimeout() == null) {
      throw new IllegalArgumentException("Waiting for completion needs an action for when the wait times out.");
    }
    return DaprSearchProtos.IndexingOptionsAlpha1.newBuilder()
        .setMode(DaprSearchProtos.IndexingMode.INDEXING_MODE_WAIT_FOR_COMPLETION)
        .setWaitTimeout(com.google.protobuf.Duration.newBuilder()
            .setSeconds(waitTimeout.getSeconds())
            .setNanos(waitTimeout.getNano()))
        .setOnWaitTimeout(toWaitTimeoutAction(options.getOnWaitTimeout()))
        .build();
  }

  private static DaprSearchProtos.IndexingWaitTimeoutAction toWaitTimeoutAction(IndexingWaitTimeoutAction action) {
    switch (action) {
      case CONTINUE_ASYNC:
        return DaprSearchProtos.IndexingWaitTimeoutAction.INDEXING_WAIT_TIMEOUT_ACTION_CONTINUE_ASYNC;
      case FAIL_REQUEST:
        return DaprSearchProtos.IndexingWaitTimeoutAction.INDEXING_WAIT_TIMEOUT_ACTION_FAIL_REQUEST;
      default:
        throw new IllegalArgumentException("Unknown wait timeout action: " + action);
    }
  }

  static IndexAck toIndexAck(DaprSearchProtos.IndexAck ack) {
    switch (ack) {
      case INDEX_ACK_QUEUED:
        return IndexAck.QUEUED;
      case INDEX_ACK_COMPLETED:
        return IndexAck.COMPLETED;
      default:
        throw invalidResponse("The store returned an invalid write acknowledgement: " + ack + ".");
    }
  }

  static List<FailedItem> toFailedItems(List<DaprSearchProtos.FailedItem> failedItems) {
    List<FailedItem> items = new ArrayList<>();
    for (DaprSearchProtos.FailedItem failedItem : failedItems) {
      items.add(new FailedItem(failedItem.getId(), toDaprException(failedItem.getError())));
    }
    return items;
  }

  static DaprException toDaprException(Status status) {
    io.grpc.Status.Code code = io.grpc.Status.fromCodeValue(status.getCode()).getCode();
    if (code == io.grpc.Status.Code.OK) {
      throw invalidResponse("The store returned an error with an OK status code.");
    }
    return new DaprException(code.toString(), status.getMessage(), new DaprErrorDetails(status), status.toByteArray());
  }

  static DaprException invalidResponse(String message) {
    return new DaprException(io.grpc.Status.Code.INTERNAL.toString(), message, (Throwable) null);
  }

  // Unlike ProtobufValueHelper.toProtobufValue, this rejects unsupported types instead of sending their toString(),
  // which a filter would silently fail to match.
  static Struct toStruct(Map<?, ?> map) {
    Struct.Builder builder = Struct.newBuilder();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      if (!(entry.getKey() instanceof String)) {
        throw new IllegalArgumentException("Query, filter and metadata keys must be strings.");
      }
      builder.putFields((String) entry.getKey(), toValue(entry.getValue()));
    }
    return builder.build();
  }

  private static Value toValue(Object value) {
    Value.Builder builder = Value.newBuilder();
    if (value == null) {
      return builder.setNullValue(NullValue.NULL_VALUE).build();
    }
    if (value instanceof Boolean) {
      return builder.setBoolValue((Boolean) value).build();
    }
    if (value instanceof Number) {
      return builder.setNumberValue(toDouble((Number) value)).build();
    }
    if (value instanceof String) {
      return builder.setStringValue((String) value).build();
    }
    if (value instanceof Collection) {
      ListValue.Builder list = ListValue.newBuilder();
      for (Object item : (Collection<?>) value) {
        list.addValues(toValue(item));
      }
      return builder.setListValue(list).build();
    }
    if (value instanceof Map) {
      return builder.setStructValue(toStruct((Map<?, ?>) value)).build();
    }
    throw new IllegalArgumentException("Query, filter and metadata values must be null, a Boolean, a Number, a String, "
        + "a Collection or a Map, not " + value.getClass().getName() + ".");
  }

  // A Struct keeps every number as a double. A number that would change on the way is rejected, because a filter
  // holding it would match the wrong documents.
  private static double toDouble(Number number) {
    if (number instanceof Double) {
      return number.doubleValue();
    }
    if (number instanceof Float) {
      // Through its decimal form, so that 1.1f stays 1.1 rather than becoming 1.100000023841858.
      return Double.parseDouble(number.toString());
    }
    double value = number.doubleValue();
    if (Double.isInfinite(value) || new BigDecimal(number.toString()).compareTo(BigDecimal.valueOf(value)) != 0) {
      throw new IllegalArgumentException("The number " + number + " cannot be represented exactly as a double.");
    }
    return value;
  }
}
