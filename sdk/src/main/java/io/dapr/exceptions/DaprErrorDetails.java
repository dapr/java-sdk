/*
 * Copyright 2024 The Dapr Authors
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

package io.dapr.exceptions;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.rpc.Status;
import io.dapr.utils.TypeRef;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class DaprErrorDetails {

  static final DaprErrorDetails EMPTY_INSTANCE = new DaprErrorDetails((Status) null);

  private static final Map<Class<? extends Message>, ErrorDetailType> SUPPORTED_ERROR_TYPES =
      Map.of(
          com.google.rpc.ErrorInfo.class, ErrorDetailType.ERROR_INFO,
          com.google.rpc.RetryInfo.class, ErrorDetailType.RETRY_INFO,
          com.google.rpc.DebugInfo.class, ErrorDetailType.DEBUG_INFO,
          com.google.rpc.QuotaFailure.class, ErrorDetailType.QUOTA_FAILURE,
          com.google.rpc.PreconditionFailure.class, ErrorDetailType.PRECONDITION_FAILURE,
          com.google.rpc.BadRequest.class, ErrorDetailType.BAD_REQUEST,
          com.google.rpc.RequestInfo.class, ErrorDetailType.REQUEST_INFO,
          com.google.rpc.ResourceInfo.class, ErrorDetailType.RESOURCE_INFO,
          com.google.rpc.Help.class, ErrorDetailType.HELP,
          com.google.rpc.LocalizedMessage.class, ErrorDetailType.LOCALIZED_MESSAGE
      );

  private static final Map<String, Class<? extends Message>> ERROR_TYPES_FQN_REVERSE_LOOKUP =
      SUPPORTED_ERROR_TYPES.keySet().stream().collect(Collectors.toMap(
          item -> generateErrorTypeFqn(item),
          item -> item
      ));

  /**
   * Error status details.
   */
  private final Map<ErrorDetailType, Map<String, Object>> map;

  public DaprErrorDetails(Status grpcStatus) {
    this.map = parse(grpcStatus);
  }

  public DaprErrorDetails(List<Map<String, Object>> entries) {
    this.map = parse(entries);
  }

  /**
   * Gets an attribute of an error detail.
   * @param errorDetailType Type of the error detail.
   * @param errAttribute Attribute of the error detail.
   * @param typeRef Type of the value expected to be returned.
   * @param <T> Type of the value to be returned.
   * @return Value of the attribute or null if not found.
   */
  public <T> T get(ErrorDetailType errorDetailType, String errAttribute, TypeRef<T> typeRef) {
    Map<String, Object> dictionary = map.get(errorDetailType);
    if (dictionary == null) {
      return null;
    }

    return (T) dictionary.get(errAttribute);
  }

  /**
   * Parses status details from a gRPC Status.
   *
   * @param status The gRPC Status to parse details from.
   * @return List containing parsed status details.
   */
  private static Map<ErrorDetailType, Map<String, Object>> parse(Status status) {
    if (status == null || status.getDetailsList() == null) {
      return Collections.emptyMap();
    }

    Map<ErrorDetailType, Map<String, Object>> detailsList = new HashMap<>();
    List<Any> grpcDetailsList = status.getDetailsList();
    for (Any detail : grpcDetailsList) {
      for (Map.Entry<Class<? extends Message>, ErrorDetailType>
          supportedClazzAndType : SUPPORTED_ERROR_TYPES.entrySet()) {
        Class<? extends Message> clazz = supportedClazzAndType.getKey();
        ErrorDetailType errorDetailType = supportedClazzAndType.getValue();
        if (detail.is(clazz)) {
          detailsList.put(errorDetailType, parseProtoMessage(detail, clazz));
        }
      }
    }
    return Collections.unmodifiableMap(detailsList);
  }

  private static Map<ErrorDetailType, Map<String, Object>> parse(List<Map<String, Object>> entries) {
    if ((entries == null) || entries.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<ErrorDetailType, Map<String, Object>> detailsList = new HashMap<>();
    for (Map<String, Object> entry : entries) {
      Object type = entry.getOrDefault("@type", "");
      if (type == null) {
        continue;
      }

      Class<? extends Message> clazz = ERROR_TYPES_FQN_REVERSE_LOOKUP.get(type.toString());
      if (clazz == null) {
        continue;
      }

      ErrorDetailType errorDetailType = SUPPORTED_ERROR_TYPES.get(clazz);
      if (errorDetailType == null) {
        continue;
      }

      detailsList.put(errorDetailType, entry);
    }
    return Collections.unmodifiableMap(detailsList);
  }

  private static <T extends com.google.protobuf.Message> Map<String, Object> parseProtoMessage(
      Any detail, Class<T> clazz) {
    try {
      T message = detail.unpack(clazz);
      return messageToMap(message);
    } catch (InvalidProtocolBufferException e) {
      return Collections.singletonMap(e.getClass().getSimpleName(), e.getMessage());
    }
  }

  /**
   * Converts a Protocol Buffer (proto) message to a Map.
   *
   * @param message The proto message to be converted.
   * @return A Map representing the fields of the proto message.
   */
  private static Map<String, Object> messageToMap(Message message) {
    Map<String, Object> result = new HashMap<>();
    Field[] fields = message.getClass().getDeclaredFields();

    result.put("@type", generateErrorTypeFqn(message.getClass()));

    for (Field field : fields) {
      if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
        continue;
      }

      String normalizedFieldName = field.getName().replaceAll("_$", "");
      try {
        field.setAccessible(true);
        Object value = field.get(message);
        result.put(normalizedFieldName, value);
      } catch (IllegalAccessException e) {
        // no-op, just ignore this attribute.
      }
    }

    return Collections.unmodifiableMap(result);
  }

  private static <T extends com.google.protobuf.Message> String generateErrorTypeFqn(Class<T> clazz) {
    String className = clazz.getName();

    // trim the 'com.' to match the kit error details returned to users
    return "type.googleapis.com/" + (className.startsWith("com.") ? className.substring(4) : className);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DaprErrorDetails that = (DaprErrorDetails) o;
    return Objects.equals(map, that.map);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(map);
  }

  public enum ErrorDetailType {
    ERROR_INFO,
    RETRY_INFO,
    DEBUG_INFO,
    QUOTA_FAILURE,
    PRECONDITION_FAILURE,
    BAD_REQUEST,
    REQUEST_INFO,
    RESOURCE_INFO,
    HELP,
    LOCALIZED_MESSAGE,
  }
}
