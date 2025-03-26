package io.dapr.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.ObjectSerializer;

public class CustomizableObjectSerializer extends ObjectSerializer implements DaprObjectSerializer {

  private final ObjectMapper objectMapper;

  public CustomizableObjectSerializer(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  @Override
  public String getContentType() {
    return "application/json";
  }
}
