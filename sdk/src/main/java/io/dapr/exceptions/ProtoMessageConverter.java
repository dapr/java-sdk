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

import com.google.protobuf.Message;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for converting Protocol Buffer (proto) messages to a Map.
 */
public class ProtoMessageConverter {
  /**
   * Converts a Protocol Buffer (proto) message to a Map.
   *
   * @param message The proto message to be converted.
   * @return A Map representing the fields of the proto message.
   */
  public static Map<String, Object> messageToMap(Message message) {
    Map<String, Object> result = new HashMap<>();
    Field[] fields = message.getClass().getDeclaredFields();

    // trim the 'com.' to match the kit error details returned to users
    String className = message.getClass().getName();
    result.put("@type", "type.googleapis.com/" + (className.startsWith("com.") ? className.substring(4) : className));

    for (Field field : fields) {
      // The error detail fields we care about end in '_'
      if (!(isSupportedField(field.getName()))) {
        continue;
      }
      try {
        field.setAccessible(true);
        Object value = field.get(message);
        String fieldNameMinusUnderscore = removeTrailingUnderscore(field.getName());
        result.put(fieldNameMinusUnderscore, value);
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }

    return result;
  }

  /**
   * Remove the trailing underscore from a string.
   *
   * @param input The input string.
   * @return The input string without the trailing underscore.
   */
  private static String removeTrailingUnderscore(String input) {
    if (input.endsWith("_")) {
      return input.substring(0, input.length() - 1);
    }
    return input;
  }

  /**
   * Check if the field name ends with an underscore ('_'). Those are the error detail
   * fields we care about.
   *
   * @param fieldName The field name.
   * @return True if the field name ends with an underscore, false otherwise.
   */
  private static boolean isSupportedField(String fieldName) {
    return fieldName.endsWith("_");
  }
}
