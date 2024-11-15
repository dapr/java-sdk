package io.dapr.testcontainers.converter;

public interface YamlConverter<T> {
  String convert(T value);
}
