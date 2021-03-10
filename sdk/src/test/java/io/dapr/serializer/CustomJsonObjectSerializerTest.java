/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.ObjectSerializer;
import io.dapr.utils.TypeRef;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Type;

import static org.junit.Assert.*;

public class CustomJsonObjectSerializerTest {

  public static class CustomJsonSerializer extends ObjectSerializer implements DaprObjectSerializer {
    CustomJsonSerializer() {
      super(new ObjectMapper());
    }

    public ObjectMapper getObjectMapper() {
      return super.getObjectMapper();
    }

    @Override
    public String getContentType() {
      return DefaultObjectSerializer.JSON_CONTENT_TYPE;
    }
  }

  CustomJsonSerializer SERIALIZER = new CustomJsonSerializer();

  @Test
  public void serializeObjectTest() {
    DefaultObjectSerializerTest.MyObjectTestToSerialize obj = new DefaultObjectSerializerTest.MyObjectTestToSerialize();
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
      DefaultObjectSerializerTest.MyObjectTestToSerialize deserializedValue = SERIALIZER.deserialize(serializedValue, DefaultObjectSerializerTest.MyObjectTestToSerialize.class);
      assertEquals(obj, deserializedValue);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }

    try {
      serializedValue = SERIALIZER.serialize(obj);
      assertNotNull(serializedValue);
      Type t = DefaultObjectSerializerTest.MyObjectTestToSerialize.class;
      TypeRef<DefaultObjectSerializerTest.MyObjectTestToSerialize> tr = TypeRef.get(t);
      DefaultObjectSerializerTest.MyObjectTestToSerialize deserializedValue = SERIALIZER.deserialize(serializedValue, tr);
      assertEquals(obj, deserializedValue);
    } catch (IOException exception) {
      fail(exception.getMessage());
    }

    assertNotNull(SERIALIZER.getObjectMapper());
  }

}
