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

package io.dapr.spring.boot4.autoconfigure.client;

import io.dapr.utils.TypeRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Jackson3ObjectSerializerTest {

  private Jackson3ObjectSerializer serializer;

  @BeforeEach
  void setUp() {
    serializer = new Jackson3ObjectSerializer(JsonMapper.builder().build());
  }

  @Test
  @DisplayName("getContentType should return application/json")
  void getContentType() {
    assertThat(serializer.getContentType()).isEqualTo("application/json");
  }

  @Test
  @DisplayName("serialize null should return null")
  void serializeNull() throws IOException {
    assertThat(serializer.serialize(null)).isNull();
  }

  @Test
  @DisplayName("serialize Void should return null")
  void serializeVoid() throws IOException {
    // Void.class instance can't be created normally, test the class check
    assertThat(serializer.serialize(null)).isNull();
  }

  @Test
  @DisplayName("serialize byte[] should return same array")
  void serializeByteArray() throws IOException {
    byte[] input = new byte[]{1, 2, 3};
    assertThat(serializer.serialize(input)).isSameAs(input);
  }

  @Test
  @DisplayName("serialize string should return JSON bytes")
  void serializeString() throws IOException {
    byte[] result = serializer.serialize("hello");
    assertThat(new String(result)).isEqualTo("\"hello\"");
  }

  @Test
  @DisplayName("serialize object should return JSON bytes")
  void serializeObject() throws IOException {
    TestData data = new TestData("test", 42);
    byte[] result = serializer.serialize(data);
    String json = new String(result);
    assertThat(json).contains("\"name\"");
    assertThat(json).contains("\"test\"");
    assertThat(json).contains("\"value\"");
    assertThat(json).contains("42");
  }

  @Test
  @DisplayName("deserialize Void type should return null")
  void deserializeVoid() throws IOException {
    assertThat(serializer.deserialize(new byte[]{}, TypeRef.get(Void.class))).isNull();
  }

  @Test
  @DisplayName("deserialize null data for non-primitive should return null")
  void deserializeNullData() throws IOException {
    assertThat(serializer.deserialize(null, TypeRef.STRING)).isNull();
  }

  @Test
  @DisplayName("deserialize byte[] type should return same array")
  void deserializeByteArray() throws IOException {
    byte[] input = new byte[]{1, 2, 3};
    byte[] result = serializer.deserialize(input, TypeRef.get(byte[].class));
    assertThat(result).isSameAs(input);
  }

  @Test
  @DisplayName("deserialize empty data should return null")
  void deserializeEmptyData() throws IOException {
    assertThat(serializer.deserialize(new byte[]{}, TypeRef.STRING)).isNull();
  }

  @Test
  @DisplayName("deserialize string should work")
  void deserializeString() throws IOException {
    byte[] data = "\"hello\"".getBytes();
    String result = serializer.deserialize(data, TypeRef.STRING);
    assertThat(result).isEqualTo("hello");
  }

  @Test
  @DisplayName("deserialize object should work")
  void deserializeObject() throws IOException {
    byte[] data = "{\"name\":\"test\",\"value\":42}".getBytes();
    TestData result = serializer.deserialize(data, TypeRef.get(TestData.class));
    assertThat(result.getName()).isEqualTo("test");
    assertThat(result.getValue()).isEqualTo(42);
  }

  @Test
  @DisplayName("deserialize generic type should work")
  void deserializeGenericType() throws IOException {
    byte[] data = "[\"a\",\"b\"]".getBytes();
    List<String> result = serializer.deserialize(data, new TypeRef<List<String>>() {});
    assertThat(result).containsExactly("a", "b");
  }

  @Test
  @DisplayName("deserialize int primitive with null data should return 0")
  void deserializeIntPrimitiveNull() throws IOException {
    int result = serializer.deserialize(null, TypeRef.INT);
    assertThat(result).isEqualTo(0);
  }

  @Test
  @DisplayName("deserialize int primitive with empty data should return 0")
  void deserializeIntPrimitiveEmpty() throws IOException {
    int result = serializer.deserialize(new byte[]{}, TypeRef.INT);
    assertThat(result).isEqualTo(0);
  }

  @Test
  @DisplayName("deserialize boolean primitive with null data should return false")
  void deserializeBooleanPrimitiveNull() throws IOException {
    boolean result = serializer.deserialize(null, TypeRef.BOOLEAN);
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("deserialize int primitive with actual data should parse correctly")
  void deserializeIntPrimitiveWithData() throws IOException {
    byte[] data = "42".getBytes();
    int result = serializer.deserialize(data, TypeRef.INT);
    assertThat(result).isEqualTo(42);
  }

  @Test
  @DisplayName("round-trip serialize and deserialize should preserve data")
  void roundTrip() throws IOException {
    TestData original = new TestData("round-trip", 99);
    byte[] serialized = serializer.serialize(original);
    TestData deserialized = serializer.deserialize(serialized, TypeRef.get(TestData.class));
    assertThat(deserialized.getName()).isEqualTo(original.getName());
    assertThat(deserialized.getValue()).isEqualTo(original.getValue());
  }

  @Test
  @DisplayName("deserialize map type should work")
  void deserializeMap() throws IOException {
    byte[] data = "{\"key\":\"value\"}".getBytes();
    Map<String, String> result = serializer.deserialize(data, new TypeRef<Map<String, String>>() {});
    assertThat(result).containsEntry("key", "value");
  }

  // --- Primitive default values for null data ---

  @Test
  @DisplayName("deserialize boolean primitive with empty data should return false")
  void deserializeBooleanPrimitiveEmpty() throws IOException {
    boolean result = serializer.deserialize(new byte[]{}, TypeRef.BOOLEAN);
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("deserialize byte primitive with null data should return 0")
  void deserializeBytePrimitiveNull() throws IOException {
    byte result = serializer.deserialize(null, TypeRef.BYTE);
    assertThat(result).isEqualTo((byte) 0);
  }

  @Test
  @DisplayName("deserialize byte primitive with empty data should return 0")
  void deserializeBytePrimitiveEmpty() throws IOException {
    byte result = serializer.deserialize(new byte[]{}, TypeRef.BYTE);
    assertThat(result).isEqualTo((byte) 0);
  }

  @Test
  @DisplayName("deserialize short primitive with null data should return 0")
  void deserializeShortPrimitiveNull() throws IOException {
    short result = serializer.deserialize(null, TypeRef.SHORT);
    assertThat(result).isEqualTo((short) 0);
  }

  @Test
  @DisplayName("deserialize short primitive with empty data should return 0")
  void deserializeShortPrimitiveEmpty() throws IOException {
    short result = serializer.deserialize(new byte[]{}, TypeRef.SHORT);
    assertThat(result).isEqualTo((short) 0);
  }

  @Test
  @DisplayName("deserialize long primitive with null data should return 0")
  void deserializeLongPrimitiveNull() throws IOException {
    long result = serializer.deserialize(null, TypeRef.LONG);
    assertThat(result).isEqualTo(0L);
  }

  @Test
  @DisplayName("deserialize long primitive with empty data should return 0")
  void deserializeLongPrimitiveEmpty() throws IOException {
    long result = serializer.deserialize(new byte[]{}, TypeRef.LONG);
    assertThat(result).isEqualTo(0L);
  }

  @Test
  @DisplayName("deserialize float primitive with null data should return 0")
  void deserializeFloatPrimitiveNull() throws IOException {
    float result = serializer.deserialize(null, TypeRef.FLOAT);
    assertThat(result).isEqualTo(0f);
  }

  @Test
  @DisplayName("deserialize float primitive with empty data should return 0")
  void deserializeFloatPrimitiveEmpty() throws IOException {
    float result = serializer.deserialize(new byte[]{}, TypeRef.FLOAT);
    assertThat(result).isEqualTo(0f);
  }

  @Test
  @DisplayName("deserialize double primitive with null data should return 0")
  void deserializeDoublePrimitiveNull() throws IOException {
    double result = serializer.deserialize(null, TypeRef.DOUBLE);
    assertThat(result).isEqualTo(0d);
  }

  @Test
  @DisplayName("deserialize double primitive with empty data should return 0")
  void deserializeDoublePrimitiveEmpty() throws IOException {
    double result = serializer.deserialize(new byte[]{}, TypeRef.DOUBLE);
    assertThat(result).isEqualTo(0d);
  }

  @Test
  @DisplayName("deserialize char primitive with null data should return MIN_VALUE")
  void deserializeCharPrimitiveNull() throws IOException {
    char result = serializer.deserialize(null, TypeRef.CHAR);
    assertThat(result).isEqualTo(Character.MIN_VALUE);
  }

  @Test
  @DisplayName("deserialize char primitive with empty data should return MIN_VALUE")
  void deserializeCharPrimitiveEmpty() throws IOException {
    char result = serializer.deserialize(new byte[]{}, TypeRef.CHAR);
    assertThat(result).isEqualTo(Character.MIN_VALUE);
  }

  // --- Primitive deserialization with actual data ---

  @Test
  @DisplayName("deserialize boolean primitive with actual data should parse correctly")
  void deserializeBooleanPrimitiveWithData() throws IOException {
    byte[] data = "true".getBytes();
    boolean result = serializer.deserialize(data, TypeRef.BOOLEAN);
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("deserialize long primitive with actual data should parse correctly")
  void deserializeLongPrimitiveWithData() throws IOException {
    byte[] data = "123456789".getBytes();
    long result = serializer.deserialize(data, TypeRef.LONG);
    assertThat(result).isEqualTo(123456789L);
  }

  @Test
  @DisplayName("deserialize float primitive with actual data should parse correctly")
  void deserializeFloatPrimitiveWithData() throws IOException {
    byte[] data = "3.14".getBytes();
    float result = serializer.deserialize(data, TypeRef.FLOAT);
    assertThat(result).isEqualTo(3.14f);
  }

  @Test
  @DisplayName("deserialize double primitive with actual data should parse correctly")
  void deserializeDoublePrimitiveWithData() throws IOException {
    byte[] data = "3.14159".getBytes();
    double result = serializer.deserialize(data, TypeRef.DOUBLE);
    assertThat(result).isEqualTo(3.14159d);
  }

  @Test
  @DisplayName("deserialize short primitive with actual data should parse correctly")
  void deserializeShortPrimitiveWithData() throws IOException {
    byte[] data = "123".getBytes();
    short result = serializer.deserialize(data, TypeRef.SHORT);
    assertThat(result).isEqualTo((short) 123);
  }

  // --- Void primitive type ---

  @Test
  @DisplayName("deserialize void primitive type with null data should return null")
  void deserializeVoidPrimitiveNull() throws IOException {
    assertThat(serializer.deserialize(null, TypeRef.VOID)).isNull();
  }

  @Test
  @DisplayName("deserialize void primitive type with empty data should return null")
  void deserializeVoidPrimitiveEmpty() throws IOException {
    assertThat(serializer.deserialize(new byte[]{}, TypeRef.VOID)).isNull();
  }

  // --- IOException wrapping ---

  @Test
  @DisplayName("serialize invalid object should throw IOException")
  void serializeInvalidObjectThrowsIOException() {
    Object unserializable = new Object() {
      // Jackson cannot serialize this because of the self-reference
      public Object getSelf() {
        return this;
      }
    };
    assertThatThrownBy(() -> serializer.serialize(unserializable))
        .isInstanceOf(IOException.class);
  }

  @Test
  @DisplayName("deserialize invalid JSON should throw IOException")
  void deserializeInvalidJsonThrowsIOException() {
    byte[] invalidJson = "{not valid json".getBytes();
    assertThatThrownBy(() -> serializer.deserialize(invalidJson, TypeRef.get(TestData.class)))
        .isInstanceOf(IOException.class);
  }

  @Test
  @DisplayName("deserialize invalid JSON for primitive should throw IOException")
  void deserializeInvalidJsonForPrimitiveThrowsIOException() {
    byte[] invalidJson = "not_a_number".getBytes();
    assertThatThrownBy(() -> serializer.deserialize(invalidJson, TypeRef.INT))
        .isInstanceOf(IOException.class);
  }

  // --- Additional edge cases ---

  @Test
  @DisplayName("deserialize null data for byte[] type should return null")
  void deserializeNullDataForByteArray() throws IOException {
    byte[] result = serializer.deserialize(null, TypeRef.BYTE_ARRAY);
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("serialize integer should return JSON bytes")
  void serializeInteger() throws IOException {
    byte[] result = serializer.serialize(42);
    assertThat(new String(result)).isEqualTo("42");
  }

  @Test
  @DisplayName("serialize boolean should return JSON bytes")
  void serializeBoolean() throws IOException {
    byte[] result = serializer.serialize(true);
    assertThat(new String(result)).isEqualTo("true");
  }

  @Test
  @DisplayName("serialize list should return JSON array bytes")
  void serializeList() throws IOException {
    byte[] result = serializer.serialize(List.of("a", "b", "c"));
    assertThat(new String(result)).isEqualTo("[\"a\",\"b\",\"c\"]");
  }

  @Test
  @DisplayName("round-trip serialize and deserialize list should preserve data")
  void roundTripList() throws IOException {
    List<String> original = List.of("x", "y", "z");
    byte[] serialized = serializer.serialize(original);
    List<String> deserialized = serializer.deserialize(serialized, new TypeRef<List<String>>() {});
    assertThat(deserialized).containsExactlyElementsOf(original);
  }

  public static class TestData {
    private String name;
    private int value;

    public TestData() {
    }

    public TestData(String name, int value) {
      this.name = name;
      this.value = value;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public int getValue() {
      return value;
    }

    public void setValue(int value) {
      this.value = value;
    }
  }
}
