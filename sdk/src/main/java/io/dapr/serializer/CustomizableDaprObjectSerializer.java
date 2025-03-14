package io.dapr.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.ObjectSerializer;
import io.dapr.utils.TypeRef;

import java.io.IOException;
import java.util.Objects;

public class CustomizableDaprObjectSerializer implements DaprObjectSerializer {

  private final ObjectSerializer delegate;

  public CustomizableDaprObjectSerializer(final ObjectMapper objectMapper) {
    ObjectMapper nonNullableObjectMapper = Objects.requireNonNull(objectMapper);
    this.delegate = ObjectSerializer.withObjectMapper(nonNullableObjectMapper);
  }

  @Override
  public byte[] serialize(Object o) throws IOException {
    return this.delegate.serialize(o);
  }

  @Override
  public <T> T deserialize(byte[] data, TypeRef<T> type) throws IOException {
    return deserialize(data, type);
  }

  @Override
  public String getContentType() {
    return "application/json";
  }
}
