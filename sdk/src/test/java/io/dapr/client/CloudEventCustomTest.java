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

import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.utils.TypeRef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CloudEventCustomTest {

  private DaprObjectSerializer serializer = new DefaultObjectSerializer();

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
    assertEquals("hello world again", cloudEventCustom.getNewValue());
    assertEquals(432434324.43, cloudEventCustom.getNewDouble(),0);
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

    CloudEventCustom<MyClass> cloudEventCustom = serializer.deserialize(
        content.getBytes(), new TypeRef<CloudEventCustom<MyClass>>() {});
    assertEquals("application/json", cloudEventCustom.getDatacontenttype());
    assertEquals(1, cloudEventCustom.getData().id);
    assertEquals("hello world", cloudEventCustom.getData().name);
    assertEquals("hello world again", cloudEventCustom.getNewValue());
    assertEquals(432434324.43, cloudEventCustom.getNewDouble(), 0);
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
    assertNull(cloudEventCustom.getNewValue());
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

    CloudEventCustom<Integer> cloudEventCustom = serializer.deserialize(
        content.getBytes(), new TypeRef<CloudEventCustom<Integer>>() {});
    assertEquals("application/json", cloudEventCustom.getDatacontenttype());
    assertEquals(1, cloudEventCustom.getData().intValue());
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

    CloudEventCustom<String> cloudEventCustom = serializer.deserialize(
        content.getBytes(), new TypeRef<CloudEventCustom<String>>() {});
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

    CloudEventCustom<String> cloudEventCustom = serializer.deserialize(
        content.getBytes(), new TypeRef<CloudEventCustom<String>>() {});
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
    CloudEventCustom<byte[]> cloudEventCustom = serializer.deserialize(
        content.getBytes(), new TypeRef<CloudEventCustom<byte[]>>() {});
    assertEquals("application/json", cloudEventCustom.getDatacontenttype());
    assertArrayEquals(expected, cloudEventCustom.getData());
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
    CloudEventCustom<byte[]> cloudEventCustom = serializer.deserialize(
        content.getBytes(), new TypeRef<CloudEventCustom<byte[]>>() {});
    assertEquals("application/octet-stream", cloudEventCustom.getDatacontenttype());
    assertNull(cloudEventCustom.getData());
    assertArrayEquals(expected, cloudEventCustom.getBinaryData());
  }

  @Test
  public void serializeAndDeserialize() throws Exception {
    CloudEventCustom<MyClass> cloudEventCustom = new CloudEventCustom();
    cloudEventCustom.setId("idVal");
    cloudEventCustom.setSource("sourceVal");
    cloudEventCustom.setType("typeVal");
    cloudEventCustom.setSpecversion("specVal");
    cloudEventCustom.setDatacontenttype("contentVal");
    cloudEventCustom.setNewValue("specVal");
    cloudEventCustom.setNewInt(5);
    cloudEventCustom.setNewDouble(323.32323);
    cloudEventCustom.setData(new MyClass() {{
      this.id = 1;
      this.name = "hello world";
    }});
    byte[] byte_array = serializer.serialize(cloudEventCustom);
    CloudEventCustom<MyClass> cloudEventUnserial =
        serializer.deserialize(byte_array, new TypeRef<CloudEventCustom<MyClass>>() {});
    assertEquals(cloudEventCustom.getDatacontenttype(), cloudEventUnserial.getDatacontenttype());
    assertEquals(cloudEventCustom.getData().id, cloudEventUnserial.getData().id);
    assertEquals(cloudEventCustom.getData().name, cloudEventUnserial.getData().name);
    assertEquals(cloudEventCustom.getId(), cloudEventUnserial.getId());
    assertEquals(cloudEventCustom.getSource(), cloudEventUnserial.getSource());
    assertEquals(cloudEventCustom.getType(), cloudEventUnserial.getType());
    assertEquals(cloudEventCustom.getSpecversion(), cloudEventUnserial.getSpecversion());
    assertEquals(cloudEventCustom.getNewValue(), cloudEventUnserial.getNewValue());
    assertEquals(cloudEventCustom.getNewInt(), cloudEventUnserial.getNewInt());
    assertEquals(cloudEventCustom.getNewDouble(), cloudEventUnserial.getNewDouble(), 0.00001);
  }

}
