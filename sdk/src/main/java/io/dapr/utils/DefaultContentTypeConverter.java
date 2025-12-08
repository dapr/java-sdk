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

package io.dapr.utils;

import io.dapr.serializer.DefaultObjectSerializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * A utility class for converting event to bytes based on content type or given serializer.
 * When an application/json or application/cloudevents+json is given as content type, the object serializer is used
 * to serialize the data into bytes
 */
public class DefaultContentTypeConverter {

  private static final DefaultObjectSerializer OBJECT_SERIALIZER = new DefaultObjectSerializer();

  /**
   * Function to convert a given event to bytes for HTTP calls.
   *
   * @param <T>         The type of the event
   * @param event       The input event
   * @param contentType The content type of the event
   * @return the event as bytes
   * @throws IllegalArgumentException on mismatch between contentType and event types
   * @throws IOException              on serialization
   */
  public static <T> byte[] convertEventToBytesForHttp(T event, String contentType)
      throws IllegalArgumentException, IOException {
    if (isBinaryContentType(contentType)) {
      if (event instanceof byte[]) {
        return Base64.getEncoder().encode((byte[]) event);
      } else {
        throw new IllegalArgumentException("mismatch between 'application/octect-stream' contentType and event. "
            + "expected binary data as bytes array");
      }
    } else if (isStringContentType(contentType)) {
      if (event instanceof String) {
        return ((String) event).getBytes(StandardCharsets.UTF_8);
      } else if (event instanceof Boolean || event instanceof Number) {
        return String.valueOf(event).getBytes(StandardCharsets.UTF_8);
      } else {
        throw new IllegalArgumentException("mismatch between string contentType and event. "
            + "expected event to be convertible into a string");
      }
    } else if (isJsonContentType(contentType) || isCloudEventContentType(contentType)) {
      return OBJECT_SERIALIZER.serialize(event);
    }
    throw new IllegalArgumentException("mismatch between contentType and event");
  }

  /**
   * Function to convert a given event to bytes for gRPC calls.
   *
   * @param <T>         The type of the event
   * @param event       The input event
   * @param contentType The content type of the event
   * @return the event as bytes
   * @throws IllegalArgumentException on mismatch between contentType and event types
   * @throws IOException              on serialization
   */
  public static <T> byte[] convertEventToBytesForGrpc(T event, String contentType)
      throws IllegalArgumentException, IOException {
    if (isBinaryContentType(contentType)) {
      if (event instanceof byte[]) {
        // Return the bytes of the event directly for gRPC
        return (byte[]) event;
      } else {
        throw new IllegalArgumentException("mismatch between 'application/octect-stream' contentType and event. "
            + "expected binary data as bytes array");
      }
    }
    // The rest of the conversion is same as HTTP
    return convertEventToBytesForHttp(event, contentType);
  }

  /**
   * Function to convert a bytes array from HTTP input into event based on given object deserializer.
   *
   * @param <T>         The type of the event
   * @param event       The input event
   * @param contentType The content type of the event
   * @param typeRef     The type to convert the event to
   * @return the event as bytes
   * @throws IllegalArgumentException on mismatch between contentType and event types
   * @throws IOException              on serialization
   */
  public static <T> T convertBytesToEventFromHttp(byte[] event, String contentType, TypeRef<T> typeRef)
      throws IllegalArgumentException, IOException {
    if (isBinaryContentType(contentType)) {
      byte[] decoded = Base64.getDecoder().decode(new String(event, StandardCharsets.UTF_8));
      return OBJECT_SERIALIZER.deserialize(decoded, typeRef);
    } else if (isStringContentType(contentType)) {
      if (TypeRef.STRING.equals(typeRef)) {
        // This is a string data, required as string
        return (T) new String(event, StandardCharsets.UTF_8);
      } else if (TypeRef.isPrimitive(typeRef)) {
        // This is primitive data
        return OBJECT_SERIALIZER.deserialize(event, typeRef);
      }
      // There is mismatch between content type and required type cast
    } else if (isJsonContentType(contentType) || isCloudEventContentType(contentType)) {
      // This is normal JSON deserialization of the event
      return OBJECT_SERIALIZER.deserialize(event, typeRef);
    }
    throw new IllegalArgumentException("mismatch between contentType and requested type cast in return");
  }

  /**
   * Function to convert a bytes array from gRPC input into event based on given object deserializer.
   *
   * @param <T>         The type of the event
   * @param event       The input event
   * @param contentType The content type of the event
   * @param typeRef     The type to convert the event to
   * @return the event as bytes
   * @throws IllegalArgumentException on mismatch between contentType and event types
   * @throws IOException              on serialization
   */
  public static <T> T convertBytesToEventFromGrpc(byte[] event, String contentType, TypeRef<T> typeRef)
      throws IllegalArgumentException, IOException {
    if (isBinaryContentType(contentType)) {
      // The byte array is directly deserialized
      return OBJECT_SERIALIZER.deserialize(event, typeRef);
    }
    // rest of the conversion is similar to the HTTP method
    return convertBytesToEventFromHttp(event, contentType, typeRef);
  }

  public static boolean isCloudEventContentType(String contentType) {
    return isContentType(contentType, "application/cloudevents+json");
  }

  public static boolean isJsonContentType(String contentType) {
    return isContentType(contentType, "application/json");
  }

  public static boolean isStringContentType(String contentType) {
    return contentType != null && (contentType.toLowerCase().startsWith("text/")
        || isContentType(contentType, "application/xml"));
  }

  public static boolean isBinaryContentType(String contentType) {
    return isContentType(contentType, "application/octet-stream");
  }

  private static boolean isContentType(String contentType, String expected) {
    if (contentType == null) {
      return false;
    }
    if (contentType.equalsIgnoreCase(expected)) {
      return true;
    }
    int semiColonPos = contentType.indexOf(";");
    if (semiColonPos > 0) {
      return contentType.substring(0, semiColonPos).equalsIgnoreCase(expected);
    }
    return false;
  }
}

