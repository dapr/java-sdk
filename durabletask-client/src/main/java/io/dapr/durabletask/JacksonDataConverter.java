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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * An implementation of {@link DataConverter} that uses Jackson APIs for data serialization.
 */
public final class JacksonDataConverter implements DataConverter {
  // Static singletons are recommended by the Jackson documentation
  private static final ObjectMapper jsonObjectMapper = JsonMapper.builder()
      .findAndAddModules()
      .build();

  @Override
  public String serialize(Object value) {
    if (value == null) {
      return null;
    }

    try {
      return jsonObjectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new DataConverterException(
          String.format("Failed to serialize argument of type '%s'. Detailed error message: %s",
              value.getClass().getName(), e.getMessage()),
          e);
    }
  }

  @Override
  public <T> T deserialize(String jsonText, Class<T> targetType) {
    if (jsonText == null || jsonText.length() == 0 || targetType == Void.class) {
      return null;
    }

    try {
      return jsonObjectMapper.readValue(jsonText, targetType);
    } catch (JsonProcessingException e) {
      throw new DataConverterException(String.format("Failed to deserialize the JSON text to %s. "
         + "Detailed error message: %s", targetType.getName(), e.getMessage()), e);
    }
  }
}
