/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.serializer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import io.dapr.client.domain.CloudEvent;
import io.dapr.utils.TypeRef;
import io.dapr.v1.CommonProtos;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DefaultObjectSerializerTest {

  private static final DefaultObjectSerializer SERIALIZER = new DefaultObjectSerializer();

  public static class MyObjectTestToSerialize implements Serializable {
    private String stringValue;
    private int intValue;
    private boolean boolValue;
    private char charValue;
    private byte byteValue;
    private short shortValue;
    private long longValue;
    private float floatValue;
    private double doubleValue;

    public String getStringValue() {
      return stringValue;
    }

    public void setStringValue(String stringValue) {
      this.stringValue = stringValue;
    }

    public int getIntValue() {
      return intValue;
    }

    public void setIntValue(int intValue) {
      this.intValue = intValue;
    }

    public boolean isBoolValue() {
      return boolValue;
    }

    public void setBoolValue(boolean boolValue) {
      this.boolValue = boolValue;
    }

    public char getCharValue() {
      return charValue;
    }

    public void setCharValue(char charValue) {
      this.charValue = charValue;
    }

    public byte getByteValue() {
      return byteValue;
    }

    public void setByteValue(byte byteValue) {
      this.byteValue = byteValue;
    }

    public short getShortValue() {
      return shortValue;
    }

    public void setShortValue(short shortValue) {
      this.shortValue = shortValue;
    }

    public long getLongValue() {
      return longValue;
    }

    public void setLongValue(long longValue) {
      this.longValue = longValue;
    }

    public float getFloatValue() {
      return floatValue;
    }

    public void setFloatValue(float floatValue) {
      this.floatValue = floatValue;
    }

    public double getDoubleValue() {
      return doubleValue;
    }

    public void setDoubleValue(double doubleValue) {
      this.doubleValue = doubleValue;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof MyObjectTestToSerialize)) {
        return false;
      }

      MyObjectTestToSerialize that = (MyObjectTestToSerialize) o;

      if (getIntValue() != that.getIntValue()) {
        return false;
      }
      if (isBoolValue() != that.isBoolValue()) {
        return false;
      }
      if (getCharValue() != that.getCharValue()) {
        return false;
      }
      if (getByteValue() != that.getByteValue()) {
        return false;
      }
      if (getShortValue() != that.getShortValue()) {
        return false;
      }
      if (getLongValue() != that.getLongValue()) {
        return false;
      }
      if (Float.compare(that.getFloatValue(), getFloatValue()) != 0) {
        return false;
      }
      if (Double.compare(that.getDoubleValue(), getDoubleValue()) != 0) {
        return false;
      }
      if (getStringValue() != null ? !getStringValue().equals(that.getStringValue()) : that.getStringValue() != null) {
        return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int result;
      long temp;
      result = getStringValue() != null ? getStringValue().hashCode() : 0;
      result = 31 * result + getIntValue();
      result = 31 * result + (isBoolValue() ? 1 : 0);
      result = 31 * result + (int) getCharValue();
      result = 31 * result + (int) getByteValue();
      result = 31 * result + (int) getShortValue();
      result = 31 * result + (int) (getLongValue() ^ (getLongValue() >>> 32));
      result = 31 * result + (getFloatValue() != +0.0f ? Float.floatToIntBits(getFloatValue()) : 0);
      temp = Double.doubleToLongBits(getDoubleValue());
      result = 31 * result + (int) (temp ^ (temp >>> 32));
      return result;
    }

    @Override
    public String toString() {
      return "MyObjectTestToSerialize{" +
          "stringValue='" + stringValue + '\'' +
          ", intValue=" + intValue +
          ", boolValue=" + boolValue +
          ", charValue=" + charValue +
          ", byteValue=" + byteValue +
          ", shortValue=" + shortValue +
          ", longValue=" + longValue +
          ", floatValue=" + floatValue +
          ", doubleValue=" + doubleValue +
          '}';
    }
  }

  @Test
  public void serializeStringObjectTest() {
    MyObjectTestToSerialize obj = new MyObjectTestToSerialize();
    obj.setStringValue("A String");
    obj.setIntValue(2147483647);
    obj.setBoolValue(true);
    obj.setCharValue('a');
    obj.setByteValue((byte) 65);
    obj.setShortValue((short) 32767);
    obj.setLongValue(9223372036854775807L);
    obj.setFloatValue(1.0f);
    obj.setDoubleValue(1000.0);
    String expectedResult = "{\"stringValue\":\"A String\",\"intValue\":2147483647,\"boolValue\":true,\"charValue\":\"a\",\"byteValue\":65,\"shortValue\":32767,\"longValue\":9223372036854775807,\"floatValue\":1.0,\"doubleValue\":1000.0}";

    
    String serializedValue;
    try {
      serializedValue = new String(SERIALIZER.serialize(obj));
      assertEquals("FOUND:[[" + serializedValue + "]] \n but was EXPECTING: [[" + expectedResult + "]]", expectedResult, serializedValue);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  @Test
  public void serializeObjectTest() {
    MyObjectTestToSerialize obj = new MyObjectTestToSerialize();
    obj.setStringValue("A String");
    obj.setIntValue(2147483647);
    obj.setBoolValue(true);
    obj.setCharValue('a');
    obj.setByteValue((byte) 65);
    obj.setShortValue((short) 32767);
    obj.setLongValue(9223372036854775807L);
    obj.setFloatValue(1.0f);
    obj.setDoubleValue(1000.0);
    //String expectedResult = "{\"stringValue\":\"A String\",\"intValue\":2147483647,\"boolValue\":true,\"charValue\":\"a\",\"byteValue\":65,\"shortValue\":32767,\"longValue\":9223372036854775807,\"floatValue\":1.0,\"doubleValue\":1000.0}";

    byte[] serializedValue;
    try {
      serializedValue = SERIALIZER.serialize(obj);
      assertNotNull(serializedValue);
      MyObjectTestToSerialize deserializedValue = SERIALIZER.deserialize(serializedValue, MyObjectTestToSerialize.class);
      assertEquals(obj, deserializedValue);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }

    try {
      serializedValue = SERIALIZER.serialize(obj);
      assertNotNull(serializedValue);
      Type t = MyObjectTestToSerialize.class;
      TypeRef<MyObjectTestToSerialize> tr = TypeRef.get(t);
      MyObjectTestToSerialize deserializedValue = SERIALIZER.deserialize(serializedValue, tr);
      assertEquals(obj, deserializedValue);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  @Test
  public void serializeNullTest() {
    
    byte[] byteSerializedValue;
    try {
      byteSerializedValue = SERIALIZER.serialize(null);
      assertNull(byteSerializedValue);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  @Test
  public void serializeStringTest() {
    String valueToSerialize = "A String";
    String expectedSerializedValue = "\"A String\"";
    
    String serializedValue;
    byte [] byteValue;
    try {
      serializedValue = new String(SERIALIZER.serialize(valueToSerialize));
      assertEquals(expectedSerializedValue, serializedValue);
      byteValue = SERIALIZER.serialize(valueToSerialize);
      assertNotNull(byteValue);
      String deserializedValue = SERIALIZER.deserialize(byteValue, String.class);
      assertEquals(valueToSerialize, deserializedValue);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  @Test
  public void serializeIntTest() {
    Integer valueToSerialize = 1;
    String expectedResult = valueToSerialize.toString();
    
    String serializedValue;
    byte [] byteValue;
    try {
      serializedValue = new String(SERIALIZER.serialize(valueToSerialize.intValue()));
      assertEquals(expectedResult, serializedValue);
      byteValue = SERIALIZER.serialize(valueToSerialize);
      assertNotNull(byteValue);
      Integer deserializedValue = SERIALIZER.deserialize(byteValue, Integer.class);
      assertEquals(valueToSerialize, deserializedValue);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  @Test
  public void serializeShortTest() {
    Short valueToSerialize = 1;
    String expectedResult = valueToSerialize.toString();
    
    String serializedValue;
    byte [] byteValue;
    try {
      serializedValue = new String(SERIALIZER.serialize(valueToSerialize.shortValue()));
      assertEquals(expectedResult, serializedValue);
      byteValue = SERIALIZER.serialize(valueToSerialize);
      assertNotNull(byteValue);
      Short deserializedValue = SERIALIZER.deserialize(byteValue, Short.class);
      assertEquals(valueToSerialize, deserializedValue);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  @Test
  public void serializeLongTest() {
    Long valueToSerialize = Long.MAX_VALUE;
    String expectedResult = valueToSerialize.toString();
    
    String serializedValue;
    byte [] byteValue;
    try {
      serializedValue = new String(SERIALIZER.serialize(valueToSerialize.longValue()));
      assertEquals(expectedResult, serializedValue);
      byteValue = SERIALIZER.serialize(valueToSerialize);
      assertNotNull(byteValue);
      Long deserializedValue = SERIALIZER.deserialize(byteValue, Long.class);
      assertEquals(valueToSerialize, deserializedValue);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  @Test
  public void serializeFloatTest() {
    Float valueToSerialize = -1.23456f;
    String expectedResult = valueToSerialize.toString();
    
    String serializedValue;
    byte [] byteValue;
    try {
      serializedValue = new String(SERIALIZER.serialize(valueToSerialize.floatValue()));
      assertEquals(expectedResult, serializedValue);
      byteValue = SERIALIZER.serialize(valueToSerialize);
      assertNotNull(byteValue);
      Float deserializedValue = SERIALIZER.deserialize(byteValue, Float.class);
      assertEquals(valueToSerialize, deserializedValue, 0.00000000001);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  @Test
  public void serializeDoubleTest() {
    Double valueToSerialize = 1.0;
    String expectedResult = valueToSerialize.toString();
    
    String serializedValue;
    byte [] byteValue;
    try {
      serializedValue = new String(SERIALIZER.serialize(valueToSerialize.doubleValue()));
      assertEquals(expectedResult, serializedValue);
      byteValue = SERIALIZER.serialize(valueToSerialize);
      assertNotNull(byteValue);
      Double deserializedValue = SERIALIZER.deserialize(byteValue, Double.class);
      assertEquals(valueToSerialize, deserializedValue);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  @Test
  public void serializeBooleanTest() {
    Boolean valueToSerialize = true;
    String expectedResult = valueToSerialize.toString();
    
    String serializedValue;
    byte [] byteValue;
    try {
      serializedValue = new String(SERIALIZER.serialize(valueToSerialize.booleanValue()));
      assertEquals(expectedResult, serializedValue);
      byteValue = SERIALIZER.serialize(valueToSerialize);
      assertNotNull(byteValue);
      Boolean deserializedValue = SERIALIZER.deserialize(byteValue, Boolean.class);
      assertEquals(valueToSerialize, deserializedValue);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  @Test
  public void serializeProtoTest() throws Exception {
    CommonProtos.Etag valueToSerialize = CommonProtos.Etag.newBuilder().setValue("myValue").build();
    String expectedSerializedBase64Value = "CgdteVZhbHVl";

    byte[] serializedValue = SERIALIZER.serialize(valueToSerialize);
    assertEquals(expectedSerializedBase64Value, Base64.getEncoder().encodeToString(serializedValue));
    assertNotNull(serializedValue);
    CommonProtos.Etag deserializedValue = SERIALIZER.deserialize(serializedValue, CommonProtos.Etag.class);
    assertEquals(valueToSerialize.getValue(), deserializedValue.getValue());
    assertEquals(valueToSerialize, deserializedValue);
  }

  @Test
  public void serializeFakeProtoTest() throws Exception {
    FakeProtoClass valueToSerialize = new FakeProtoClass();
    String expectedSerializedBase64Value = "AQ==";

    byte[] serializedValue = SERIALIZER.serialize(valueToSerialize);
    assertEquals(expectedSerializedBase64Value, Base64.getEncoder().encodeToString(serializedValue));
    assertNotNull(serializedValue);

    // Tries to parse as JSON since FakeProtoClass does not have `parseFrom()` static method.
    assertThrows(JsonParseException.class, () -> SERIALIZER.deserialize(serializedValue, FakeProtoClass.class));
  }

  @Test
  public void deserializeObjectTest() {
    String jsonToDeserialize = "{\"stringValue\":\"A String\",\"intValue\":2147483647,\"boolValue\":true,\"charValue\":\"a\",\"byteValue\":65,\"shortValue\":32767,\"longValue\":9223372036854775807,\"floatValue\":1.0,\"doubleValue\":1000.0}";
    MyObjectTestToSerialize expectedResult = new MyObjectTestToSerialize();
    expectedResult.setStringValue("A String");
    expectedResult.setIntValue(2147483647);
    expectedResult.setBoolValue(true);
    expectedResult.setCharValue('a');
    expectedResult.setByteValue((byte) 65);
    expectedResult.setShortValue((short) 32767);
    expectedResult.setLongValue(9223372036854775807L);
    expectedResult.setFloatValue(1.0f);
    expectedResult.setDoubleValue(1000.0);
    MyObjectTestToSerialize result;

    try {
      result = SERIALIZER.deserialize(jsonToDeserialize.getBytes(), TypeRef.get(MyObjectTestToSerialize.class));
      assertEquals("The expected value is different than the actual result", expectedResult, result);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  @Test
  public void deserializeArrayObjectTest() {
    String jsonToDeserialize = "[{\"stringValue\":\"A String\",\"intValue\":2147483647,\"boolValue\":true,\"charValue\":\"a\",\"byteValue\":65,\"shortValue\":32767,\"longValue\":9223372036854775807,\"floatValue\":1.0,\"doubleValue\":1000.0}]";
    MyObjectTestToSerialize expectedResult = new MyObjectTestToSerialize();
    expectedResult.setStringValue("A String");
    expectedResult.setIntValue(2147483647);
    expectedResult.setBoolValue(true);
    expectedResult.setCharValue('a');
    expectedResult.setByteValue((byte) 65);
    expectedResult.setShortValue((short) 32767);
    expectedResult.setLongValue(9223372036854775807L);
    expectedResult.setFloatValue(1.0f);
    expectedResult.setDoubleValue(1000.0);
    List<MyObjectTestToSerialize> result;

    try {
      result = SERIALIZER.deserialize(jsonToDeserialize.getBytes(), new TypeRef<List<MyObjectTestToSerialize>>(){});
      assertEquals("The expected value is different than the actual result", expectedResult, result.get(0));
    } catch (IOException exception) {
      fail(exception.getMessage());
    }

    try {
      TypeRef<List<MyObjectTestToSerialize>> tr1 = new TypeRef<List<MyObjectTestToSerialize>>(){};
      Type t = tr1.getType();
      TypeRef<?> tr = TypeRef.get(t);
      result = (List<MyObjectTestToSerialize>) SERIALIZER.deserialize(jsonToDeserialize.getBytes(), tr);
      assertEquals("The expected value is different than the actual result", expectedResult, result.get(0));
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  @Test
  public void deserializeBytesTest() {
    
    try {
      byte[] result = SERIALIZER.deserialize("String".getBytes(), byte[].class);
      assertNotNull(result);
      assertEquals("String", new String(result));
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  @Test
  public void deserializeNullObjectOrPrimitiveTest() {
    
    try {
      MyObjectTestToSerialize objResult = SERIALIZER.deserialize(null, MyObjectTestToSerialize.class);
      assertNull(objResult);
      boolean boolResult = SERIALIZER.deserialize(null, boolean.class);
      assertFalse(boolResult);
      byte expectedByteResult = (byte) 0;
      byte byteResult = SERIALIZER.deserialize(null, byte.class);
      assertEquals(expectedByteResult, byteResult);
      short expectedShortResult = (short) 0;
      short shortResult = SERIALIZER.deserialize(null, short.class);
      assertEquals(expectedShortResult, shortResult);
      int expectedIntResult = 0;
      int intResult = SERIALIZER.deserialize(null, int.class);
      assertEquals(expectedIntResult, intResult);
      long expectedLongResult = 0L;
      long longResult = SERIALIZER.deserialize(null, long.class);
      assertEquals(expectedLongResult, longResult);
      float expectedFloatResult = 0f;
      float floatResult = SERIALIZER.deserialize(null, float.class);
      assertEquals(expectedFloatResult, floatResult, 0.0f);
      double expectedDoubleResult = 0;
      double doubleResult = SERIALIZER.deserialize(null, double.class);
      assertEquals(expectedDoubleResult, doubleResult, 0.0);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  @Test
  public void deserializeObjectMissingStringPropertyTest() {
    String jsonToDeserialize = "{\"intValue\":2147483647,\"boolValue\":true,\"charValue\":\"a\",\"byteValue\":65,\"shortValue\":32767,\"longValue\":9223372036854775807,\"floatValue\":1.0,\"doubleValue\":1000.0}";
    MyObjectTestToSerialize expectedResult = new MyObjectTestToSerialize();
    expectedResult.setIntValue(2147483647);
    expectedResult.setBoolValue(true);
    expectedResult.setCharValue('a');
    expectedResult.setByteValue((byte) 65);
    expectedResult.setShortValue((short) 32767);
    expectedResult.setLongValue(9223372036854775807L);
    expectedResult.setFloatValue(1.0f);
    expectedResult.setDoubleValue(1000.0);
    MyObjectTestToSerialize result;
    
    try {
      result = SERIALIZER.deserialize(jsonToDeserialize.getBytes(), MyObjectTestToSerialize.class);
      assertEquals("FOUND:[[" + result + "]] \n but was EXPECING: [[" + expectedResult + "]]", expectedResult, result);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  @Test
  public void deserializeObjectMissingIntTest() {
    String jsonToDeserialize = "{\"stringValue\":\"A String\",\"boolValue\":true,\"charValue\":\"a\",\"byteValue\":65,\"shortValue\":32767,\"longValue\":9223372036854775807,\"floatValue\":1.0,\"doubleValue\":1000.0}";
    MyObjectTestToSerialize expectedResult = new MyObjectTestToSerialize();
    expectedResult.setStringValue("A String");
    expectedResult.setBoolValue(true);
    expectedResult.setCharValue('a');
    expectedResult.setByteValue((byte) 65);
    expectedResult.setShortValue((short) 32767);
    expectedResult.setLongValue(9223372036854775807L);
    expectedResult.setFloatValue(1.0f);
    expectedResult.setDoubleValue(1000.0);
    MyObjectTestToSerialize result;
    
    try {
      result = SERIALIZER.deserialize(jsonToDeserialize.getBytes(), MyObjectTestToSerialize.class);
      assertEquals("FOUND:[[" + result + "]] \n but was EXPECING: [[" + expectedResult + "]]", expectedResult, result);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  @Test
  public void deserializeObjectMissingBooleanTest() {
    String jsonToDeserialize = "{\"stringValue\":\"A String\",\"intValue\":2147483647,\"charValue\":\"a\",\"byteValue\":65,\"shortValue\":32767,\"longValue\":9223372036854775807,\"floatValue\":1.0,\"doubleValue\":1000.0}";
    MyObjectTestToSerialize expectedResult = new MyObjectTestToSerialize();
    expectedResult.setStringValue("A String");
    expectedResult.setIntValue(2147483647);
    expectedResult.setCharValue('a');
    expectedResult.setByteValue((byte) 65);
    expectedResult.setShortValue((short) 32767);
    expectedResult.setLongValue(9223372036854775807L);
    expectedResult.setFloatValue(1.0f);
    expectedResult.setDoubleValue(1000.0);
    MyObjectTestToSerialize result;
    
    try {
      result = SERIALIZER.deserialize(jsonToDeserialize.getBytes(), MyObjectTestToSerialize.class);
      assertEquals("FOUND:[[" + result + "]] \n but was EXPECING: [[" + expectedResult + "]]", expectedResult, result);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  @Test
  public void deserializeObjectMissingCharTest() {
    String jsonToDeserialize = "{\"stringValue\":\"A String\",\"intValue\":2147483647,\"boolValue\":true,\"byteValue\":65,\"shortValue\":32767,\"longValue\":9223372036854775807,\"floatValue\":1.0,\"doubleValue\":1000.0}";
    MyObjectTestToSerialize expectedResult = new MyObjectTestToSerialize();
    expectedResult.setStringValue("A String");
    expectedResult.setIntValue(2147483647);
    expectedResult.setBoolValue(true);
    expectedResult.setByteValue((byte) 65);
    expectedResult.setShortValue((short) 32767);
    expectedResult.setLongValue(9223372036854775807L);
    expectedResult.setFloatValue(1.0f);
    expectedResult.setDoubleValue(1000.0);
    MyObjectTestToSerialize result;
    
    try {
      result = SERIALIZER.deserialize(jsonToDeserialize.getBytes(), MyObjectTestToSerialize.class);
      assertEquals("FOUND:[[" + result + "]] \n but was EXPECING: [[" + expectedResult + "]]", expectedResult, result);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  @Test
  public void deserializeObjectMissingByteTest() {
    String jsonToDeserialize = "{\"stringValue\":\"A String\",\"intValue\":2147483647,\"boolValue\":true,\"charValue\":\"a\",\"shortValue\":32767,\"longValue\":9223372036854775807,\"floatValue\":1.0,\"doubleValue\":1000.0}";
    MyObjectTestToSerialize expectedResult = new MyObjectTestToSerialize();
    expectedResult.setStringValue("A String");
    expectedResult.setIntValue(2147483647);
    expectedResult.setBoolValue(true);
    expectedResult.setCharValue('a');
    expectedResult.setShortValue((short) 32767);
    expectedResult.setLongValue(9223372036854775807L);
    expectedResult.setFloatValue(1.0f);
    expectedResult.setDoubleValue(1000.0);
    MyObjectTestToSerialize result;
    
    try {
      result = SERIALIZER.deserialize(jsonToDeserialize.getBytes(), MyObjectTestToSerialize.class);
      assertEquals("FOUND:[[" + result + "]] \n but was EXPECING: [[" + expectedResult + "]]", expectedResult, result);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  @Test
  public void deserializeObjectMissingShortTest() {
    String jsonToDeserialize = "{\"stringValue\":\"A String\",\"intValue\":2147483647,\"boolValue\":true,\"charValue\":\"a\",\"byteValue\":65,\"longValue\":9223372036854775807,\"floatValue\":1.0,\"doubleValue\":1000.0}";
    MyObjectTestToSerialize expectedResult = new MyObjectTestToSerialize();
    expectedResult.setStringValue("A String");
    expectedResult.setIntValue(2147483647);
    expectedResult.setBoolValue(true);
    expectedResult.setCharValue('a');
    expectedResult.setByteValue((byte) 65);
    expectedResult.setLongValue(9223372036854775807L);
    expectedResult.setFloatValue(1.0f);
    expectedResult.setDoubleValue(1000.0);
    MyObjectTestToSerialize result;
    
    try {
      result = SERIALIZER.deserialize(jsonToDeserialize.getBytes(), MyObjectTestToSerialize.class);
      assertEquals("FOUND:[[" + result + "]] \n but was EXPECING: [[" + expectedResult + "]]", expectedResult, result);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  @Test
  public void deserializeObjectMissingLongTest() {
    String jsonToDeserialize = "{\"stringValue\":\"A String\",\"intValue\":2147483647,\"boolValue\":true,\"charValue\":\"a\",\"byteValue\":65,\"shortValue\":32767,\"floatValue\":1.0,\"doubleValue\":1000.0}";
    MyObjectTestToSerialize expectedResult = new MyObjectTestToSerialize();
    expectedResult.setStringValue("A String");
    expectedResult.setIntValue(2147483647);
    expectedResult.setBoolValue(true);
    expectedResult.setCharValue('a');
    expectedResult.setByteValue((byte) 65);
    expectedResult.setShortValue((short) 32767);
    expectedResult.setFloatValue(1.0f);
    expectedResult.setDoubleValue(1000.0);
    MyObjectTestToSerialize result;
    
    try {
      result = SERIALIZER.deserialize(jsonToDeserialize.getBytes(), MyObjectTestToSerialize.class);
      assertEquals("FOUND:[[" + result + "]] \n but was EXPECING: [[" + expectedResult + "]]", expectedResult, result);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  @Test
  public void deserializeObjectMissingFloatTest() {
    String jsonToDeserialize = "{\"stringValue\":\"A String\",\"intValue\":2147483647,\"boolValue\":true,\"charValue\":\"a\",\"byteValue\":65,\"shortValue\":32767,\"longValue\":9223372036854775807,\"doubleValue\":1000.0}";
    MyObjectTestToSerialize expectedResult = new MyObjectTestToSerialize();
    expectedResult.setStringValue("A String");
    expectedResult.setIntValue(2147483647);
    expectedResult.setBoolValue(true);
    expectedResult.setCharValue('a');
    expectedResult.setByteValue((byte) 65);
    expectedResult.setShortValue((short) 32767);
    expectedResult.setLongValue(9223372036854775807L);
    expectedResult.setDoubleValue(1000.0);
    MyObjectTestToSerialize result;
    
    try {
      result = SERIALIZER.deserialize(jsonToDeserialize.getBytes(), MyObjectTestToSerialize.class);
      assertEquals("FOUND:[[" + result + "]] \n but was EXPECING: [[" + expectedResult + "]]", expectedResult, result);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  @Test
  public void deserializeObjectMissingDoubleTest() {
    String jsonToDeserialize = "{\"stringValue\":\"A String\",\"intValue\":2147483647,\"boolValue\":true,\"charValue\":\"a\",\"byteValue\":65,\"shortValue\":32767,\"longValue\":9223372036854775807,\"floatValue\":1.0}";
    MyObjectTestToSerialize expectedResult = new MyObjectTestToSerialize();
    expectedResult.setStringValue("A String");
    expectedResult.setIntValue(2147483647);
    expectedResult.setBoolValue(true);
    expectedResult.setCharValue('a');
    expectedResult.setByteValue((byte) 65);
    expectedResult.setShortValue((short) 32767);
    expectedResult.setLongValue(9223372036854775807L);
    expectedResult.setFloatValue(1.0f);
    MyObjectTestToSerialize result;
    
    try {
      result = SERIALIZER.deserialize(jsonToDeserialize.getBytes(), MyObjectTestToSerialize.class);
      assertEquals("FOUND:[[" + result + "]] \n but was EXPECING: [[" + expectedResult + "]]", expectedResult, result);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  @Test(expected = IOException.class)
  public void deserializeObjectIntExceedMaximunValueTest() throws Exception {
    String jsonToDeserialize = "{\"stringValue\":\"A String\",\"intValue\":2147483648,\"boolValue\":true,\"charValue\":\"a\",\"byteValue\":65,\"shortValue\":32767,\"longValue\":9223372036854775807,\"floatValue\":1.0,\"doubleValue\":1000.0}";
    
    SERIALIZER.deserialize(jsonToDeserialize.getBytes(), MyObjectTestToSerialize.class);
  }

  @Test(expected = IOException.class)
  public void deserializeObjectNotACharTest() throws Exception {
    String jsonToDeserialize = "{\"stringValue\":\"A String\",\"intValue\":2147483647,\"boolValue\":true,\"charValue\":\"Not A Char\",\"byteValue\":65,\"shortValue\":32767,\"longValue\":9223372036854775807,\"floatValue\":1.0,\"doubleValue\":1000.0}";
    
    try {
      SERIALIZER.deserialize(jsonToDeserialize.getBytes(), MyObjectTestToSerialize.class);
    } catch (IOException ioEx) {
      throw ioEx;
    } catch (Exception ex) {
      fail("Wrong exception thrown: [" + ex.getClass() + "] Message:[" + ex.getMessage() + "]");
    }
  }

  @Test(expected = IOException.class)
  public void deserializeObjectShortExceededMaximunValueTest() throws Exception {
    String jsonToDeserialize = "{\"stringValue\":\"A String\",\"intValue\":2147483647,\"boolValue\":true,\"charValue\":\"a\",\"byteValue\":65,\"shortValue\":32768,\"longValue\":9223372036854775807,\"floatValue\":1.0,\"doubleValue\":1000.0}";
    
    SERIALIZER.deserialize(jsonToDeserialize.getBytes(), MyObjectTestToSerialize.class);
  }

  @Test(expected = IOException.class)
  public void deserializeObjectLongExceededMaximumValueTest() throws Exception {
    String jsonToDeserialize = "{\"stringValue\":\"A String\",\"intValue\":2147483647,\"boolValue\":true,\"charValue\":\"a\",\"byteValue\":65,\"shortValue\":32767,\"longValue\":9223372036854775808,\"floatValue\":1.0,\"doubleValue\":1000.0}";
    
    MyObjectTestToSerialize result = SERIALIZER.deserialize(jsonToDeserialize.getBytes(), MyObjectTestToSerialize.class);
  }

  @Test
  public void deserializeNullToPrimitives() throws Exception {
    
    assertEquals(0, (char)SERIALIZER.deserialize(null, char.class));
    assertEquals(0, (int)SERIALIZER.deserialize(null, int.class));
    assertEquals(0, (long)SERIALIZER.deserialize(null, long.class));
    assertEquals(0, (byte)SERIALIZER.deserialize(null, byte.class));
    assertEquals(0, SERIALIZER.deserialize(null, double.class), 0);
    assertEquals(0, SERIALIZER.deserialize(null, float.class), 0);
    assertEquals(false, SERIALIZER.deserialize(null, boolean.class));

    assertNull(SERIALIZER.deserialize(null, Character.class));
    assertNull(SERIALIZER.deserialize(null, Integer.class));
    assertNull(SERIALIZER.deserialize(null, Long.class));
    assertNull(SERIALIZER.deserialize(null, Byte.class));
    assertNull(SERIALIZER.deserialize(null, Double.class));
    assertNull(SERIALIZER.deserialize(null, Float.class));
    assertNull(SERIALIZER.deserialize(null, Boolean.class));
  }

  @Test
  public void deserializeEmptyByteArrayToPrimitives() throws Exception {
    
    assertEquals(0, (char)SERIALIZER.deserialize(new byte[0], char.class));
    assertEquals(0, (int)SERIALIZER.deserialize(new byte[0], int.class));
    assertEquals(0, (long)SERIALIZER.deserialize(new byte[0], long.class));
    assertEquals(0, (byte)SERIALIZER.deserialize(new byte[0], byte.class));
    assertEquals(0, SERIALIZER.deserialize(new byte[0], double.class), 0);
    assertEquals(0, SERIALIZER.deserialize(new byte[0], float.class), 0);
    assertEquals(false, SERIALIZER.deserialize(new byte[0], boolean.class));

    assertNull(SERIALIZER.deserialize(new byte[0], Character.class));
    assertNull(SERIALIZER.deserialize(new byte[0], Integer.class));
    assertNull(SERIALIZER.deserialize(new byte[0], Long.class));
    assertNull(SERIALIZER.deserialize(new byte[0], Byte.class));
    assertNull(SERIALIZER.deserialize(new byte[0], Double.class));
    assertNull(SERIALIZER.deserialize(new byte[0], Float.class));
    assertNull(SERIALIZER.deserialize(new byte[0], Boolean.class));
  }

  @Test
  public void serializeDeserializeCloudEventEnvelope() throws Exception {
    

    Function<CloudEvent, Boolean> check = (e -> {
      try {
        if (e == null) {
          return CloudEvent.deserialize(SERIALIZER.serialize(e)) == null;
        }

        return e.equals(CloudEvent.deserialize(SERIALIZER.serialize(e)));
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    });

    assertTrue(check.apply(null));
    assertTrue(check.apply(
      new CloudEvent(
        "1",
        "mysource",
        "text",
        "v2",
        "XML",
        "<root></root>")));
    assertTrue(check.apply(
      new CloudEvent(
        "1234-65432",
        "myother",
        "image",
        "v2",
        "byte",
        Base64.getEncoder().encodeToString(new byte[] {0, 2, 99}))));
  }

  @Test
  public void deserializeCloudEventEnvelopeData() throws Exception {
    

    Function<String, Object> deserializeData = (jsonData -> {
      try {
        String payload = String.format("{\"data\": %s}", jsonData);
        return CloudEvent.deserialize(payload.getBytes()).getData();
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    });

    assertEquals(123,
        deserializeData.apply("123"));
    assertEquals(true,
        deserializeData.apply("true"));
    assertEquals(123.45,
        deserializeData.apply("123.45"));
    assertEquals("AAEI",
        deserializeData.apply(quote(Base64.getEncoder().encodeToString(new byte[]{0, 1, 8}))));
    assertEquals("hello world",
        deserializeData.apply(quote("hello world")));
    assertEquals("\"hello world\"",
        deserializeData.apply(quote("\\\"hello world\\\"")));
    assertEquals("\"hello world\"",
        deserializeData.apply(new ObjectMapper().writeValueAsString("\"hello world\"")));
    assertEquals("hello world",
        deserializeData.apply(new ObjectMapper().writeValueAsString("hello world")));
    assertEquals(new TreeMap<String, String>() {{
      put("id", "123");
      put("name", "Jon Doe");
    }}, deserializeData.apply("{\"id\": \"123\", \"name\": \"Jon Doe\"}"));
    assertEquals("{\"id\": \"123\", \"name\": \"Jon Doe\"}",
        deserializeData.apply(new ObjectMapper().writeValueAsString("{\"id\": \"123\", \"name\": \"Jon Doe\"}")));
  }

  @Test
  public void deserializeListOfString() throws IOException {
    List<String> r = SERIALIZER.deserialize("[\"1\", \"2\", \"3\"]".getBytes(), ArrayList.class);

    assertNotNull(r);
    assertEquals(3, r.size());
    assertEquals("1", r.get(0));
    assertEquals("2", r.get(1));
    assertEquals("3", r.get(2));
  }

  private static String quote(String content) {
    if (content == null) {
      return null;
    }

    return "\"" + content + "\"";
  }

  /**
   * Class that simulates a proto class implementing MessageLite but does not have `parseFrom()` static method.
   */
  public static final class FakeProtoClass implements MessageLite {
    @Override
    public void writeTo(CodedOutputStream codedOutputStream) throws IOException {
    }

    @Override
    public int getSerializedSize() {
      return 0;
    }

    @Override
    public Parser<? extends MessageLite> getParserForType() {
      return null;
    }

    @Override
    public ByteString toByteString() {
      return null;
    }

    @Override
    public byte[] toByteArray() {
      return new byte[]{0x1};
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {

    }

    @Override
    public void writeDelimitedTo(OutputStream outputStream) throws IOException {

    }

    @Override
    public Builder newBuilderForType() {
      return null;
    }

    @Override
    public Builder toBuilder() {
      return null;
    }

    @Override
    public MessageLite getDefaultInstanceForType() {
      return null;
    }

    @Override
    public boolean isInitialized() {
      return false;
    }
  }
}
