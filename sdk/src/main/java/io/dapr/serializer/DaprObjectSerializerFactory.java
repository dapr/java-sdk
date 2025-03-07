package io.dapr.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface DaprObjectSerializerFactory {

  static DaprObjectSerializer createDefaultSerializer() {
    return new DefaultObjectSerializer();
  }

  static DaprObjectSerializer createJacksonSerializer(final ObjectMapper objectMapper) {
    return new CustomizableDaprObjectSerializer(objectMapper);
  }
}
