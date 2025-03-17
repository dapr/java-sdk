package io.dapr.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface ObjectSerializerFactory {

  static DaprObjectSerializer createJacksonSerializer(final ObjectMapper objectMapper) {
    return new CustomizableObjectSerializer(objectMapper);
  }
}
