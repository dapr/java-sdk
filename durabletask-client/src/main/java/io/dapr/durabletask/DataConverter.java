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

package io.dapr.durabletask;

import com.google.protobuf.Timestamp;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Interface for serializing and deserializing data that gets passed to and from orchestrators and activities.
 *
 * <p>Implementations of this abstract class are free to use any serialization method. Currently, only strings are
 * supported as the serialized representation of data. Byte array payloads and streams are not supported by this
 * abstraction. Note that these methods all accept null values, in which case the return value should also be null.</p>
 */
public interface DataConverter {
  /**
   * Serializes the input into a text representation.
   *
   * @param value the value to be serialized
   * @return a serialized text representation of the value or <code>null</code> if the value is <code>null</code>
   */
  @Nullable
  String serialize(@Nullable Object value);

  /**
   * Deserializes the given text data into an object of the specified type.
   *
   * @param data   the text data to deserialize into an object
   * @param target the target class to deserialize the input into
   * @param <T>    the generic parameter type representing the target class to deserialize the input into
   * @return a deserialized object of type <code>T</code>
   * @throws DataConverterException if the text data cannot be deserialized
   */
  @Nullable
  <T> T deserialize(@Nullable String data, Class<T> target);

  // Data conversion errors are expected to be unrecoverable in most cases, hence an unchecked runtime exception
  class DataConverterException extends RuntimeException {
    public DataConverterException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  /**
   * Convert from Timestamp to Instant.
   *
   * @param ts timestamp to convert
   * @return instant
   */
  static Instant getInstantFromTimestamp(Timestamp ts) {
    if (ts == null) {
      return null;
    }

    // We don't include nanoseconds because of serialization round-trip issues
    return Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos()).truncatedTo(ChronoUnit.MILLIS);
  }

  /**
   * Convert from Instant to Timestamp.
   * @param instant to convert
   * @return timestamp
   */
  static Timestamp getTimestampFromInstant(Instant instant) {
    if (instant == null) {
      return null;
    }

    return Timestamp.newBuilder()
        .setSeconds(instant.getEpochSecond())
        .setNanos(instant.getNano())
        .build();
  }
}
