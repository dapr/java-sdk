package io.dapr.utils;

import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class ObjectSerializerTest {

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
  public void serializeObjectTest() {
    MyObjectTestToSerialize obj = new MyObjectTestToSerialize();
    obj.setStringValue("A String");
    obj.setIntValue(2147483647);
    obj.setBoolValue(true);
    obj.setCharValue('a');
    obj.setByteValue((byte)65);
    obj.setShortValue((short) 32767);
    obj.setLongValue(9223372036854775807L);
    obj.setFloatValue(1.0f);
    obj.setDoubleValue(1000.0);
    String expectedResult = "{\"stringValue\":\"A String\",\"intValue\":2147483647,\"boolValue\":true,\"charValue\":\"a\",\"byteValue\":65,\"shortValue\":32767,\"longValue\":9223372036854775807,\"floatValue\":1.0,\"doubleValue\":1000.0}";

    ObjectSerializer serializer = new ObjectSerializer();
    String serializedValue;
    try {
      serializedValue = serializer.serialize(obj);
      assertEquals("FOUND:[[" + serializedValue + "]] \n but was EXPECING: [[" + expectedResult + "]]", expectedResult, serializedValue);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  @Test
  public void serializeNullTest() {
    ObjectSerializer serializer = new ObjectSerializer();
    String serializedValue;
    try {
      serializedValue = serializer.serialize(null);
      assertNull("The expected result is null", serializedValue);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  @Test
  public void serializeStringTest() {
    String valueToSerialize = "A String";
    ObjectSerializer serializer = new ObjectSerializer();
    String serializedValue;
    try {
      serializedValue = serializer.serialize(valueToSerialize);
      assertEquals(valueToSerialize, serializedValue);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  @Test
  public void serializeIntTest() {
    Integer valueToSerialize = 1;
    String expectedResult = valueToSerialize.toString();
    ObjectSerializer serializer = new ObjectSerializer();
    String serializedValue;
    try {
      serializedValue = serializer.serialize(valueToSerialize.intValue());
      assertEquals(expectedResult, serializedValue);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  @Test
  public void deserializeObjectTest() {
    String jsonToDeserialize = "{\"stringValue\":\"A String\",\"intValue\":2147483647,\"boolValue\":true,\"charValue\":\"a\",\"byteValue\":65,\"shortValue\":32767,\"longValue\":9223372036854775807,\"floatValue\":1.0,\"doubleValue\":1000.0}";
    MyObjectTestToSerialize expectedResult = new MyObjectTestToSerialize();
    expectedResult.setStringValue("A String");
    expectedResult.setIntValue(2147483647);
    expectedResult.setBoolValue(true);
    expectedResult.setCharValue('a');
    expectedResult.setByteValue((byte)65);
    expectedResult.setShortValue((short) 32767);
    expectedResult.setLongValue(9223372036854775807L);
    expectedResult.setFloatValue(1.0f);
    expectedResult.setDoubleValue(1000.0);
    MyObjectTestToSerialize result;
    ObjectSerializer serializer = new ObjectSerializer();
    try {
      result = serializer.deserialize(jsonToDeserialize, MyObjectTestToSerialize.class);
      assertEquals("The expected value is different than the actual result", expectedResult, result);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  public void deserializeNullObjectOrPrimitiveTest() {
    ObjectSerializer serializer = new ObjectSerializer();
    try {
      MyObjectTestToSerialize expectedObj = null;
     MyObjectTestToSerialize objResult = serializer.deserialize(null, MyObjectTestToSerialize.class);
     assertEquals(expectedObj, objResult);
     boolean expectedBoolResutl = false;
     boolean boolResult = serializer.deserialize(null, boolean.class);
     assertEquals(expectedBoolResutl, boolResult);
     byte expectedByteResult = Byte.valueOf((byte) 0);
     byte byteResult = serializer.deserialize(null, byte.class);
     assertEquals(expectedByteResult, byteResult);
     short expectedShortResult = (short) 0;
     short shortResult = serializer.deserialize(null, short.class);
     assertEquals(expectedShortResult, shortResult);
     int expectedIntResult = 0;
     int intResult = serializer.deserialize(null, int.class);
     assertEquals(expectedIntResult, intResult);
     long expectedLongResult = 0L;
     long longResult = serializer.deserialize(null, long.class);
     assertEquals(expectedLongResult, longResult);
     float expectedFloatResult = 0f;
     float floatResult = serializer.deserialize(null, float.class);
     assertEquals(expectedFloatResult, floatResult);
     double expectedDoubleResult = (double) 0;
     double doubleResult = serializer.deserialize(null, double.class);
     assertEquals(expectedDoubleResult, doubleResult);
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
    expectedResult.setByteValue((byte)65);
    expectedResult.setShortValue((short) 32767);
    expectedResult.setLongValue(9223372036854775807L);
    expectedResult.setFloatValue(1.0f);
    expectedResult.setDoubleValue(1000.0);
    MyObjectTestToSerialize result;
    ObjectSerializer serializer = new ObjectSerializer();
    try {
      result = serializer.deserialize(jsonToDeserialize, MyObjectTestToSerialize.class);
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
    expectedResult.setByteValue((byte)65);
    expectedResult.setShortValue((short) 32767);
    expectedResult.setLongValue(9223372036854775807L);
    expectedResult.setFloatValue(1.0f);
    expectedResult.setDoubleValue(1000.0);
    MyObjectTestToSerialize result;
    ObjectSerializer serializer = new ObjectSerializer();
    try {
      result = serializer.deserialize(jsonToDeserialize, MyObjectTestToSerialize.class);
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
    expectedResult.setByteValue((byte)65);
    expectedResult.setShortValue((short) 32767);
    expectedResult.setLongValue(9223372036854775807L);
    expectedResult.setFloatValue(1.0f);
    expectedResult.setDoubleValue(1000.0);
    MyObjectTestToSerialize result;
    ObjectSerializer serializer = new ObjectSerializer();
    try {
      result = serializer.deserialize(jsonToDeserialize, MyObjectTestToSerialize.class);
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
    expectedResult.setByteValue((byte)65);
    expectedResult.setShortValue((short) 32767);
    expectedResult.setLongValue(9223372036854775807L);
    expectedResult.setFloatValue(1.0f);
    expectedResult.setDoubleValue(1000.0);
    MyObjectTestToSerialize result;
    ObjectSerializer serializer = new ObjectSerializer();
    try {
      result = serializer.deserialize(jsonToDeserialize, MyObjectTestToSerialize.class);
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
    ObjectSerializer serializer = new ObjectSerializer();
    try {
      result = serializer.deserialize(jsonToDeserialize, MyObjectTestToSerialize.class);
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
    expectedResult.setByteValue((byte)65);
    expectedResult.setLongValue(9223372036854775807L);
    expectedResult.setFloatValue(1.0f);
    expectedResult.setDoubleValue(1000.0);
    MyObjectTestToSerialize result;
    ObjectSerializer serializer = new ObjectSerializer();
    try {
      result = serializer.deserialize(jsonToDeserialize, MyObjectTestToSerialize.class);
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
    expectedResult.setByteValue((byte)65);
    expectedResult.setShortValue((short) 32767);
    expectedResult.setFloatValue(1.0f);
    expectedResult.setDoubleValue(1000.0);
    MyObjectTestToSerialize result;
    ObjectSerializer serializer = new ObjectSerializer();
    try {
      result = serializer.deserialize(jsonToDeserialize, MyObjectTestToSerialize.class);
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
    expectedResult.setByteValue((byte)65);
    expectedResult.setShortValue((short) 32767);
    expectedResult.setLongValue(9223372036854775807L);
    expectedResult.setDoubleValue(1000.0);
    MyObjectTestToSerialize result;
    ObjectSerializer serializer = new ObjectSerializer();
    try {
      result = serializer.deserialize(jsonToDeserialize, MyObjectTestToSerialize.class);
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
    expectedResult.setByteValue((byte)65);
    expectedResult.setShortValue((short) 32767);
    expectedResult.setLongValue(9223372036854775807L);
    expectedResult.setFloatValue(1.0f);
    MyObjectTestToSerialize result;
    ObjectSerializer serializer = new ObjectSerializer();
    try {
      result = serializer.deserialize(jsonToDeserialize, MyObjectTestToSerialize.class);
      assertEquals("FOUND:[[" + result + "]] \n but was EXPECING: [[" + expectedResult + "]]", expectedResult, result);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }
  }

  @Test(expected = IOException.class)
  public void deserializeObjectIntExceedMaximunValueTest() throws Exception {
    String jsonToDeserialize = "{\"stringValue\":\"A String\",\"intValue\":2147483648,\"boolValue\":true,\"charValue\":\"a\",\"byteValue\":65,\"shortValue\":32767,\"longValue\":9223372036854775807,\"floatValue\":1.0,\"doubleValue\":1000.0}";
    ObjectSerializer serializer = new ObjectSerializer();
    serializer.deserialize(jsonToDeserialize, MyObjectTestToSerialize.class);
  }

  @Test(expected = IOException.class)
  public void deserializeObjectNotACharTest() throws Exception {
    String jsonToDeserialize = "{\"stringValue\":\"A String\",\"intValue\":2147483647,\"boolValue\":true,\"charValue\":\"Not A Char\",\"byteValue\":65,\"shortValue\":32767,\"longValue\":9223372036854775807,\"floatValue\":1.0,\"doubleValue\":1000.0}";
    ObjectSerializer serializer = new ObjectSerializer();
    try {
      serializer.deserialize(jsonToDeserialize, MyObjectTestToSerialize.class);
    } catch (IOException ioEx) {
      throw ioEx;
    } catch (Exception ex) {
      fail("Wrong exception thrown: [" + ex.getClass() + "] Message:[" + ex.getMessage() + "]");
    }
  }

  @Test(expected = IOException.class)
  public void deserializeObjectShortExceededMaximunValueTest() throws Exception {
    String jsonToDeserialize = "{\"stringValue\":\"A String\",\"intValue\":2147483647,\"boolValue\":true,\"charValue\":\"a\",\"byteValue\":65,\"shortValue\":32768,\"longValue\":9223372036854775807,\"floatValue\":1.0,\"doubleValue\":1000.0}";
    ObjectSerializer serializer = new ObjectSerializer();
    serializer.deserialize(jsonToDeserialize, MyObjectTestToSerialize.class);
  }

  @Test(expected = IOException.class)
  public void deserializeObjectLongExceededMaximumValueTest() throws Exception {
    String jsonToDeserialize = "{\"stringValue\":\"A String\",\"intValue\":2147483647,\"boolValue\":true,\"charValue\":\"a\",\"byteValue\":65,\"shortValue\":32767,\"longValue\":9223372036854775808,\"floatValue\":1.0,\"doubleValue\":1000.0}";
    ObjectSerializer serializer = new ObjectSerializer();
    MyObjectTestToSerialize result = serializer.deserialize(jsonToDeserialize, MyObjectTestToSerialize.class);
  }
}
