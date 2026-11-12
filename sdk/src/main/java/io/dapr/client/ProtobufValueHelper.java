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

package io.dapr.client;

import com.google.protobuf.ListValue;
import com.google.protobuf.NullValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.dapr.serializer.DaprObjectSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class to convert between Java objects and Google Protobuf Value types.
 */
public class ProtobufValueHelper {

  /**
   * Converts a Java object to a Google Protobuf Value.
   *
   * @param obj the Java object to convert
   * @return the corresponding Protobuf Value
   * @throws IOException if serialization fails
   */
  public static Value toProtobufValue(Object obj) throws IOException {
    if (obj == null) {
      return Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build();
    }

    if (obj instanceof Boolean) {
      return Value.newBuilder().setBoolValue((Boolean) obj).build();
    }

    if (obj instanceof String) {
      return Value.newBuilder().setStringValue((String) obj).build();
    }

    if (obj instanceof Number) {
      return Value.newBuilder().setNumberValue(((Number) obj).doubleValue()).build();
    }

    if (obj instanceof List) {
      ListValue.Builder listBuilder = ListValue.newBuilder();
      for (Object item : (List<?>) obj) {
        listBuilder.addValues(toProtobufValue(item));
      }
      return Value.newBuilder().setListValue(listBuilder.build()).build();
    }

    if (obj instanceof Map) {
      Struct.Builder structBuilder = Struct.newBuilder();
      for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
        String key = entry.getKey().toString();
        Value value = toProtobufValue(entry.getValue());
        structBuilder.putFields(key, value);
      }
      return Value.newBuilder().setStructValue(structBuilder.build()).build();
    }

    // Fallback: convert to string
    return Value.newBuilder().setStringValue(obj.toString()).build();
  }

  /**
   * Converts a Google Protobuf Value to a Java object: null, Boolean, Double, String, List or Map.
   *
   * @param value the Protobuf Value to convert
   * @return the corresponding Java object
   */
  static Object fromProtobufValue(Value value) {
    switch (value.getKindCase()) {
      case BOOL_VALUE:
        return value.getBoolValue();
      case NUMBER_VALUE:
        return value.getNumberValue();
      case STRING_VALUE:
        return value.getStringValue();
      case LIST_VALUE:
        List<Object> list = new ArrayList<>();
        for (Value item : value.getListValue().getValuesList()) {
          list.add(fromProtobufValue(item));
        }
        return list;
      case STRUCT_VALUE:
        return fromProtobufStruct(value.getStructValue());
      default:
        return null;
    }
  }

  /**
   * Converts a Google Protobuf Struct to a Java map.
   *
   * @param struct the Protobuf Struct to convert
   * @return a map with the same fields, converted by {@link #fromProtobufValue(Value)}
   */
  static Map<String, Object> fromProtobufStruct(Struct struct) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (Map.Entry<String, Value> field : struct.getFieldsMap().entrySet()) {
      map.put(field.getKey(), fromProtobufValue(field.getValue()));
    }
    return map;
  }
}
