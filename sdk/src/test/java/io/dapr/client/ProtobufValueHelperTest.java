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
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProtobufValueHelperTest {

  @Test
  public void testToProtobufValue_Null() throws IOException {
    Value result = ProtobufValueHelper.toProtobufValue(null);

    assertNotNull(result);
    assertTrue(result.hasNullValue());
    assertEquals(NullValue.NULL_VALUE, result.getNullValue());
  }

  @Test
  public void testToProtobufValue_Boolean_True() throws IOException {
    Value result = ProtobufValueHelper.toProtobufValue(true);

    assertNotNull(result);
    assertTrue(result.hasBoolValue());
    assertEquals(true, result.getBoolValue());
  }

  @Test
  public void testToProtobufValue_Boolean_False() throws IOException {
    Value result = ProtobufValueHelper.toProtobufValue(false);

    assertNotNull(result);
    assertTrue(result.hasBoolValue());
    assertEquals(false, result.getBoolValue());
  }

  @Test
  public void testToProtobufValue_String() throws IOException {
    String testString = "Hello, World!";
    Value result = ProtobufValueHelper.toProtobufValue(testString);

    assertNotNull(result);
    assertTrue(result.hasStringValue());
    assertEquals(testString, result.getStringValue());
  }

  @Test
  public void testToProtobufValue_String_Empty() throws IOException {
    String emptyString = "";
    Value result = ProtobufValueHelper.toProtobufValue(emptyString);

    assertNotNull(result);
    assertTrue(result.hasStringValue());
    assertEquals(emptyString, result.getStringValue());
  }

  @Test
  public void testToProtobufValue_Integer() throws IOException {
    Integer testInt = 42;
    Value result = ProtobufValueHelper.toProtobufValue(testInt);

    assertNotNull(result);
    assertTrue(result.hasNumberValue());
    assertEquals(42.0, result.getNumberValue(), 0.001);
  }

  @Test
  public void testToProtobufValue_Long() throws IOException {
    Long testLong = 9876543210L;
    Value result = ProtobufValueHelper.toProtobufValue(testLong);

    assertNotNull(result);
    assertTrue(result.hasNumberValue());
    assertEquals(9876543210.0, result.getNumberValue(), 0.001);
  }

  @Test
  public void testToProtobufValue_Double() throws IOException {
    Double testDouble = 3.14159;
    Value result = ProtobufValueHelper.toProtobufValue(testDouble);

    assertNotNull(result);
    assertTrue(result.hasNumberValue());
    assertEquals(testDouble, result.getNumberValue(), 0.00001);
  }

  @Test
  public void testToProtobufValue_Float() throws IOException {
    Float testFloat = 2.718f;
    Value result = ProtobufValueHelper.toProtobufValue(testFloat);

    assertNotNull(result);
    assertTrue(result.hasNumberValue());
    assertEquals(2.718, result.getNumberValue(), 0.001);
  }

  @Test
  public void testToProtobufValue_BigInteger() throws IOException {
    BigInteger testBigInt = new BigInteger("123456789012345678901234567890");
    Value result = ProtobufValueHelper.toProtobufValue(testBigInt);

    assertNotNull(result);
    assertTrue(result.hasNumberValue());
    assertEquals(1.2345678901234568E29, result.getNumberValue(), 1E20);
  }

  @Test
  public void testToProtobufValue_BigDecimal() throws IOException {
    BigDecimal testBigDecimal = new BigDecimal("123.456789");
    Value result = ProtobufValueHelper.toProtobufValue(testBigDecimal);

    assertNotNull(result);
    assertTrue(result.hasNumberValue());
    assertEquals(123.456789, result.getNumberValue(), 0.000001);
  }

  @Test
  public void testToProtobufValue_EmptyList() throws IOException {
    List<Object> emptyList = new ArrayList<>();
    Value result = ProtobufValueHelper.toProtobufValue(emptyList);

    assertNotNull(result);
    assertTrue(result.hasListValue());
    ListValue listValue = result.getListValue();
    assertEquals(0, listValue.getValuesCount());
  }

  @Test
  public void testToProtobufValue_SimpleList() throws IOException {
    List<Object> testList = Arrays.asList("hello", 42, true, null);
    Value result = ProtobufValueHelper.toProtobufValue(testList);

    assertNotNull(result);
    assertTrue(result.hasListValue());
    ListValue listValue = result.getListValue();
    assertEquals(4, listValue.getValuesCount());

    // Verify each element
    assertEquals("hello", listValue.getValues(0).getStringValue());
    assertEquals(42.0, listValue.getValues(1).getNumberValue(), 0.001);
    assertEquals(true, listValue.getValues(2).getBoolValue());
    assertEquals(NullValue.NULL_VALUE, listValue.getValues(3).getNullValue());
  }

  @Test
  public void testToProtobufValue_NestedList() throws IOException {
    List<Object> innerList = Arrays.asList(1, 2, 3);
    List<Object> outerList = Arrays.asList("outer", innerList, "end");
    Value result = ProtobufValueHelper.toProtobufValue(outerList);

    assertNotNull(result);
    assertTrue(result.hasListValue());
    ListValue listValue = result.getListValue();
    assertEquals(3, listValue.getValuesCount());

    // Verify nested list
    assertEquals("outer", listValue.getValues(0).getStringValue());
    assertTrue(listValue.getValues(1).hasListValue());
    ListValue nestedList = listValue.getValues(1).getListValue();
    assertEquals(3, nestedList.getValuesCount());
    assertEquals(1.0, nestedList.getValues(0).getNumberValue(), 0.001);
    assertEquals(2.0, nestedList.getValues(1).getNumberValue(), 0.001);
    assertEquals(3.0, nestedList.getValues(2).getNumberValue(), 0.001);
    assertEquals("end", listValue.getValues(2).getStringValue());
  }

  @Test
  public void testToProtobufValue_EmptyMap() throws IOException {
    Map<String, Object> emptyMap = new HashMap<>();
    Value result = ProtobufValueHelper.toProtobufValue(emptyMap);

    assertNotNull(result);
    assertTrue(result.hasStructValue());
    Struct struct = result.getStructValue();
    assertEquals(0, struct.getFieldsCount());
  }

  @Test
  public void testToProtobufValue_SimpleMap() throws IOException {
    Map<String, Object> testMap = new LinkedHashMap<>();
    testMap.put("name", "John Doe");
    testMap.put("age", 30);
    testMap.put("active", true);
    testMap.put("description", null);

    Value result = ProtobufValueHelper.toProtobufValue(testMap);

    assertNotNull(result);
    assertTrue(result.hasStructValue());
    Struct struct = result.getStructValue();
    assertEquals(4, struct.getFieldsCount());

    // Verify each field
    assertEquals("John Doe", struct.getFieldsMap().get("name").getStringValue());
    assertEquals(30.0, struct.getFieldsMap().get("age").getNumberValue(), 0.001);
    assertEquals(true, struct.getFieldsMap().get("active").getBoolValue());
    assertEquals(NullValue.NULL_VALUE, struct.getFieldsMap().get("description").getNullValue());
  }

  @Test
  public void testToProtobufValue_NestedMap() throws IOException {
    Map<String, Object> innerMap = new HashMap<>();
    innerMap.put("city", "New York");
    innerMap.put("zipcode", 10001);

    Map<String, Object> outerMap = new HashMap<>();
    outerMap.put("name", "John");
    outerMap.put("address", innerMap);
    outerMap.put("hobbies", Arrays.asList("reading", "coding"));

    Value result = ProtobufValueHelper.toProtobufValue(outerMap);

    assertNotNull(result);
    assertTrue(result.hasStructValue());
    Struct struct = result.getStructValue();
    assertEquals(3, struct.getFieldsCount());

    // Verify nested structure
    assertEquals("John", struct.getFieldsMap().get("name").getStringValue());

    // Verify nested map
    assertTrue(struct.getFieldsMap().get("address").hasStructValue());
    Struct nestedStruct = struct.getFieldsMap().get("address").getStructValue();
    assertEquals("New York", nestedStruct.getFieldsMap().get("city").getStringValue());
    assertEquals(10001.0, nestedStruct.getFieldsMap().get("zipcode").getNumberValue(), 0.001);

    // Verify nested list
    assertTrue(struct.getFieldsMap().get("hobbies").hasListValue());
    ListValue hobbiesList = struct.getFieldsMap().get("hobbies").getListValue();
    assertEquals(2, hobbiesList.getValuesCount());
    assertEquals("reading", hobbiesList.getValues(0).getStringValue());
    assertEquals("coding", hobbiesList.getValues(1).getStringValue());
  }

  @Test
  public void testToProtobufValue_MapWithNonStringKeys() throws IOException {
    Map<Integer, String> intKeyMap = new HashMap<>();
    intKeyMap.put(1, "one");
    intKeyMap.put(2, "two");

    Value result = ProtobufValueHelper.toProtobufValue(intKeyMap);

    assertNotNull(result);
    assertTrue(result.hasStructValue());
    Struct struct = result.getStructValue();
    assertEquals(2, struct.getFieldsCount());

    // Keys should be converted to strings
    assertTrue(struct.getFieldsMap().containsKey("1"));
    assertTrue(struct.getFieldsMap().containsKey("2"));
    assertEquals("one", struct.getFieldsMap().get("1").getStringValue());
    assertEquals("two", struct.getFieldsMap().get("2").getStringValue());
  }

  @Test
  public void testToProtobufValue_CustomObject() throws IOException {
    // Test with a custom object that will fall back to toString()
    TestCustomObject customObj = new TestCustomObject("test", 123);
    Value result = ProtobufValueHelper.toProtobufValue(customObj);

    assertNotNull(result);
    assertTrue(result.hasStringValue());
    assertEquals("TestCustomObject{name='test', value=123}", result.getStringValue());
  }

  @Test
  public void testToProtobufValue_ComplexNestedStructure() throws IOException {
    // Create a complex nested structure
    Map<String, Object> config = new HashMap<>();
    config.put("timeout", 30);
    config.put("retries", 3);

    Map<String, Object> server = new HashMap<>();
    server.put("host", "localhost");
    server.put("port", 8080);
    server.put("ssl", true);
    server.put("config", config);

    List<String> tags = Arrays.asList("prod", "critical", "monitoring");

    Map<String, Object> application = new HashMap<>();
    application.put("name", "my-app");
    application.put("version", "1.0.0");
    application.put("server", server);
    application.put("tags", tags);
    application.put("metadata", null);

    Value result = ProtobufValueHelper.toProtobufValue(application);

    assertNotNull(result);
    assertTrue(result.hasStructValue());
    Struct appStruct = result.getStructValue();

    // Verify top-level fields
    assertEquals("my-app", appStruct.getFieldsMap().get("name").getStringValue());
    assertEquals("1.0.0", appStruct.getFieldsMap().get("version").getStringValue());
    assertEquals(NullValue.NULL_VALUE, appStruct.getFieldsMap().get("metadata").getNullValue());

    // Verify server object
    assertTrue(appStruct.getFieldsMap().get("server").hasStructValue());
    Struct serverStruct = appStruct.getFieldsMap().get("server").getStructValue();
    assertEquals("localhost", serverStruct.getFieldsMap().get("host").getStringValue());
    assertEquals(8080.0, serverStruct.getFieldsMap().get("port").getNumberValue(), 0.001);
    assertEquals(true, serverStruct.getFieldsMap().get("ssl").getBoolValue());

    // Verify nested config
    assertTrue(serverStruct.getFieldsMap().get("config").hasStructValue());
    Struct configStruct = serverStruct.getFieldsMap().get("config").getStructValue();
    assertEquals(30.0, configStruct.getFieldsMap().get("timeout").getNumberValue(), 0.001);
    assertEquals(3.0, configStruct.getFieldsMap().get("retries").getNumberValue(), 0.001);

    // Verify tags list
    assertTrue(appStruct.getFieldsMap().get("tags").hasListValue());
    ListValue tagsList = appStruct.getFieldsMap().get("tags").getListValue();
    assertEquals(3, tagsList.getValuesCount());
    assertEquals("prod", tagsList.getValues(0).getStringValue());
    assertEquals("critical", tagsList.getValues(1).getStringValue());
    assertEquals("monitoring", tagsList.getValues(2).getStringValue());
  }

  @Test
  public void testToProtobufValue_OpenAPIFunctionSchema() throws IOException {
    // Test with the exact schema structure provided by the user
    Map<String, Object> functionSchema = new LinkedHashMap<>();
    functionSchema.put("type", "function");
    functionSchema.put("name", "get_horoscope");
    functionSchema.put("description", "Get today's horoscope for an astrological sign.");

    Map<String, Object> parameters = new LinkedHashMap<>();
    parameters.put("type", "object");

    Map<String, Object> properties = new LinkedHashMap<>();
    Map<String, Object> signProperty = new LinkedHashMap<>();
    signProperty.put("type", "string");
    signProperty.put("description", "An astrological sign like Taurus or Aquarius");
    properties.put("sign", signProperty);

    parameters.put("properties", properties);
    parameters.put("required", Arrays.asList("sign"));

    functionSchema.put("parameters", parameters);

    Value result = ProtobufValueHelper.toProtobufValue(functionSchema);

    assertNotNull(result);
    assertTrue(result.hasStructValue());
    Struct rootStruct = result.getStructValue();

    // Verify root level fields
    assertEquals("function", rootStruct.getFieldsMap().get("type").getStringValue());
    assertEquals("get_horoscope", rootStruct.getFieldsMap().get("name").getStringValue());
    assertEquals("Get today's horoscope for an astrological sign.",
                rootStruct.getFieldsMap().get("description").getStringValue());

    // Verify parameters object
    assertTrue(rootStruct.getFieldsMap().get("parameters").hasStructValue());
    Struct parametersStruct = rootStruct.getFieldsMap().get("parameters").getStructValue();
    assertEquals("object", parametersStruct.getFieldsMap().get("type").getStringValue());

    // Verify properties object
    assertTrue(parametersStruct.getFieldsMap().get("properties").hasStructValue());
    Struct propertiesStruct = parametersStruct.getFieldsMap().get("properties").getStructValue();

    // Verify sign property
    assertTrue(propertiesStruct.getFieldsMap().get("sign").hasStructValue());
    Struct signStruct = propertiesStruct.getFieldsMap().get("sign").getStructValue();
    assertEquals("string", signStruct.getFieldsMap().get("type").getStringValue());
    assertEquals("An astrological sign like Taurus or Aquarius",
                signStruct.getFieldsMap().get("description").getStringValue());

    // Verify required array
    assertTrue(parametersStruct.getFieldsMap().get("required").hasListValue());
    ListValue requiredList = parametersStruct.getFieldsMap().get("required").getListValue();
    assertEquals(1, requiredList.getValuesCount());
    assertEquals("sign", requiredList.getValues(0).getStringValue());
  }

  /**
   * Helper class for testing custom object conversion
   */
  private static class TestCustomObject {
    private final String name;
    private final int value;

    public TestCustomObject(String name, int value) {
      this.name = name;
      this.value = value;
    }

    @Override
    public String toString() {
      return "TestCustomObject{name='" + name + "', value=" + value + "}";
    }
  }
}
