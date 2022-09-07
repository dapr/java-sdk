/*
 * Copyright 2022 The Dapr Authors
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import io.dapr.client.domain.CloudEvent;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.utils.TypeRef;

public class CloudEventCustomTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  DaprObjectSerializer serializer = new DefaultObjectSerializer();

  public static class MyClass {
    public int id;
    public String name;
  }


  @Test
  public void deserializeNewAttributes() throws Exception {
    String content = "{\n" +
        "    \"specversion\" : \"1.0\",\n" +
        "    \"newValue\" : \"hello world again\",\n" +
        "    \"newDouble\" : 432434324.43,\n" +
        "    \"newInt\" : 435,\n" +
        "    \"type\" : \"com.github.pull_request.opened\",\n" +
        "    \"source\" : \"https://github.com/cloudevents/spec/pull\",\n" +
        "    \"subject\" : \"123\",\n" +
        "    \"id\" : \"A234-1234-1234\",\n" +
        "    \"time\" : \"2018-04-05T17:31:00Z\",\n" +
        "    \"comexampleextension1\" : \"value\",\n" +
        "    \"comexampleothervalue\" : 5,\n" +
        "    \"datacontenttype\" : \"application/json\"\n" +
        "}";

    CloudEventCustom cloudEventCustom = serializer.deserialize(content.getBytes(), TypeRef.get(CloudEventCustom.class));
    assertEquals("application/json", cloudEventCustom.getDatacontenttype());
    assertEquals("hello world again", cloudEventCustom.newValue);
    assertEquals(432434324.43, cloudEventCustom.newDouble,0);
    assertEquals(435, cloudEventCustom.getNewInt());
  }

  @Test
  public void deserializeObjectClass() throws Exception {
    String content = "{\n" +
        "    \"specversion\" : \"1.0\",\n" +
        "    \"newValue\" : \"hello world again\",\n" +
        "    \"newDouble\" : 432434324.43,\n" +
        "    \"newInt\" : 435,\n" +
        "    \"type\" : \"com.github.pull_request.opened\",\n" +
        "    \"source\" : \"https://github.com/cloudevents/spec/pull\",\n" +
        "    \"subject\" : \"123\",\n" +
        "    \"id\" : \"A234-1234-1234\",\n" +
        "    \"time\" : \"2018-04-05T17:31:00Z\",\n" +
        "    \"comexampleextension1\" : \"value\",\n" +
        "    \"comexampleothervalue\" : 5,\n" +
        "    \"datacontenttype\" : \"application/json\",\n" +
        "    \"data\" : {\"id\": 1, \"name\": \"hello world\"}\n" +
        "}";

    MyClass expected = new MyClass() {{
      this.id = 1;
      this.name = "hello world";
    }};

    CloudEventCustom cloudEventCustom = serializer.deserialize(content.getBytes(), TypeRef.get(CloudEventCustom.class));
    assertEquals("application/json", cloudEventCustom.getDatacontenttype());
    MyClass myObject = OBJECT_MAPPER.convertValue(cloudEventCustom.getData(), MyClass.class);
    assertEquals(expected.id, myObject.id);
    assertEquals(expected.name, myObject.name);
    assertEquals("hello world again", cloudEventCustom.newValue);
    assertEquals(432434324.43, cloudEventCustom.newDouble, 0);
    assertEquals(435, cloudEventCustom.getNewInt());

  }

  @Test
  public void deserializeNullData() throws Exception {
    String content = "{\n" +
        "    \"specversion\" : \"1.0\",\n" +
        "    \"type\" : \"com.github.pull_request.opened\",\n" +
        "    \"source\" : \"https://github.com/cloudevents/spec/pull\",\n" +
        "    \"subject\" : \"123\",\n" +
        "    \"id\" : \"A234-1234-1234\",\n" +
        "    \"time\" : \"2018-04-05T17:31:00Z\",\n" +
        "    \"comexampleextension1\" : \"value\",\n" +
        "    \"comexampleothervalue\" : 5,\n" +
        "    \"datacontenttype\" : \"application/json\"\n" +
        "}";

    CloudEventCustom cloudEventCustom = serializer.deserialize(content.getBytes(), TypeRef.get(CloudEventCustom.class));
    assertEquals("application/json", cloudEventCustom.getDatacontenttype());
    assertNull(cloudEventCustom.getData());
    assertNull(cloudEventCustom.newValue);
    assertEquals(0, cloudEventCustom.getNewInt());
  }

  @Test
  public void deserializeInteger() throws Exception {
    String content = "{\n" +
        "    \"specversion\" : 7,\n" +
        "    \"newInt\" : 7,\n" +
        "    \"type\" : \"com.github.pull_request.opened\",\n" +
        "    \"source\" : \"https://github.com/cloudevents/spec/pull\",\n" +
        "    \"subject\" : \"123\",\n" +
        "    \"id\" : \"A234-1234-1234\",\n" +
        "    \"time\" : \"2018-04-05T17:31:00Z\",\n" +
        "    \"comexampleextension1\" : \"value\",\n" +
        "    \"comexampleothervalue\" : 5,\n" +
        "    \"datacontenttype\" : \"application/json\",\n" +
        "    \"data\" : 1\n" +
        "}";

    CloudEventCustom cloudEventCustom = serializer.deserialize(content.getBytes(), TypeRef.get(CloudEventCustom.class));
    assertEquals("application/json", cloudEventCustom.getDatacontenttype());
    assertEquals(1, cloudEventCustom.getData());
    assertEquals(7, cloudEventCustom.getNewInt());
  }

  @Test
  public void deserializeString() throws Exception {
    String content = "{\n" +
        "    \"specversion\" : \"1.0\",\n" +
        "    \"type\" : \"com.github.pull_request.opened\",\n" +
        "    \"source\" : \"https://github.com/cloudevents/spec/pull\",\n" +
        "    \"subject\" : \"123\",\n" +
        "    \"id\" : \"A234-1234-1234\",\n" +
        "    \"time\" : \"2018-04-05T17:31:00Z\",\n" +
        "    \"comexampleextension1\" : \"value\",\n" +
        "    \"comexampleothervalue\" : 5,\n" +
        "    \"datacontenttype\" : \"text/plain\",\n" +
        "    \"data\" : \"hello world\"\n" +
        "}";

    CloudEventCustom cloudEventCustom = serializer.deserialize(content.getBytes(), TypeRef.get(CloudEventCustom.class));
    assertEquals("text/plain", cloudEventCustom.getDatacontenttype());
    assertEquals("hello world", cloudEventCustom.getData());
  }

  @Test
  public void deserializeXML() throws Exception {
    String content = "{\n" +
        "    \"specversion\" : \"1.0\",\n" +
        "    \"type\" : \"com.github.pull_request.opened\",\n" +
        "    \"source\" : \"https://github.com/cloudevents/spec/pull\",\n" +
        "    \"subject\" : \"123\",\n" +
        "    \"id\" : \"A234-1234-1234\",\n" +
        "    \"time\" : \"2018-04-05T17:31:00Z\",\n" +
        "    \"comexampleextension1\" : \"value\",\n" +
        "    \"comexampleothervalue\" : 5,\n" +
        "    \"datacontenttype\" : \"text/xml\",\n" +
        "    \"data\" : \"<root/>\"\n" +
        "}";

    CloudEventCustom cloudEventCustom = serializer.deserialize(content.getBytes(), TypeRef.get(CloudEventCustom.class));
    assertEquals("text/xml", cloudEventCustom.getDatacontenttype());
    assertEquals("<root/>", cloudEventCustom.getData());
  }

  @Test
  public void deserializeBytes() throws Exception {
    String content = "{\n" +
        "    \"specversion\" : \"1.0\",\n" +
        "    \"type\" : \"com.github.pull_request.opened\",\n" +
        "    \"source\" : \"https://github.com/cloudevents/spec/pull\",\n" +
        "    \"subject\" : \"123\",\n" +
        "    \"id\" : \"A234-1234-1234\",\n" +
        "    \"time\" : \"2018-04-05T17:31:00Z\",\n" +
        "    \"comexampleextension1\" : \"value\",\n" +
        "    \"comexampleothervalue\" : 5,\n" +
        "    \"datacontenttype\" : \"application/json\",\n" +
        "    \"data\" : \"AQI=\"\n" +
        "}";

    byte[] expected = new byte[]{ 0x1, 0x2 };
    CloudEventCustom cloudEventCustom = serializer.deserialize(content.getBytes(), TypeRef.get(CloudEventCustom.class));
    assertEquals("application/json", cloudEventCustom.getDatacontenttype());
    assertEquals("AQI=", cloudEventCustom.getData());
    assertArrayEquals(expected, OBJECT_MAPPER.convertValue(cloudEventCustom.getData(), byte[].class));
  }

  @Test
  public void deserializeBinaryData() throws Exception {
    String content = "{\n" +
        "    \"specversion\" : \"1.0\",\n" +
        "    \"type\" : \"com.github.pull_request.opened\",\n" +
        "    \"source\" : \"https://github.com/cloudevents/spec/pull\",\n" +
        "    \"subject\" : \"123\",\n" +
        "    \"id\" : \"A234-1234-1234\",\n" +
        "    \"time\" : \"2018-04-05T17:31:00Z\",\n" +
        "    \"comexampleextension1\" : \"value\",\n" +
        "    \"comexampleothervalue\" : 5,\n" +
        "    \"datacontenttype\" : \"application/octet-stream\",\n" +
        "    \"data_base64\" : \"AQI=\"\n" +
        "}";

    byte[] expected = new byte[]{ 0x1, 0x2 };
    CloudEventCustom cloudEventCustom = serializer.deserialize(content.getBytes(), TypeRef.get(CloudEventCustom.class));
    assertEquals("application/octet-stream", cloudEventCustom.getDatacontenttype());
    assertNull(cloudEventCustom.getData());
    assertArrayEquals(expected, cloudEventCustom.getBinaryData());
  }

  @Test
  public void serializeAndDeserialize() throws Exception {
    MyClass expected = new MyClass() {{
      this.id = 1;
      this.name = "hello world";
    }};

    CloudEventCustom<MyClass> cloudEventCustom = new CloudEventCustom("idVal", "sourceVal", "typeVal", "specVal", "contentVal", expected, "newString", 5, 323.32323);
    byte[] byte_array = serializer.serialize(cloudEventCustom);
    CloudEventCustom cloudEventUnserial = serializer.deserialize(byte_array, TypeRef.get(CloudEventCustom.class));
    assertEquals("contentVal", cloudEventUnserial.getDatacontenttype());
    MyClass myObject = OBJECT_MAPPER.convertValue(cloudEventUnserial.getData(), MyClass.class);
    assertEquals(expected.id, myObject.id);
    assertEquals(expected.name, myObject.name);
    assertEquals("idVal", cloudEventUnserial.getId());
    assertEquals("sourceVal", cloudEventUnserial.getSource());
    assertEquals("typeVal", cloudEventUnserial.getType());
    assertEquals("specVal", cloudEventUnserial.getSpecversion());
    assertEquals("newString", cloudEventUnserial.newValue);
    assertEquals(5, cloudEventUnserial.getNewInt());
    assertEquals(323.32323, cloudEventUnserial.newDouble, 0);
  }

}
