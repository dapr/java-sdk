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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.domain.CloudEvent;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CloudEventTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static class MyClass {
    public int id;
    public String name;
  }

  @Test
  public void deserializeObjectClass() throws Exception {
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
        "    \"data\" : {\"id\": 1, \"name\": \"hello world\"}\n" +
        "}";

    MyClass expected = new MyClass() {{
      this.id = 1;
      this.name = "hello world";
    }};

    CloudEvent cloudEvent = CloudEvent.deserialize(content.getBytes());
    assertEquals("application/json", cloudEvent.getDatacontenttype());
    MyClass myObject = OBJECT_MAPPER.convertValue(cloudEvent.getData(), MyClass.class);
    assertEquals(expected.id, myObject.id);
    assertEquals(expected.name, myObject.name);
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

    CloudEvent cloudEvent = CloudEvent.deserialize(content.getBytes());
    assertEquals("application/json", cloudEvent.getDatacontenttype());
    assertNull(cloudEvent.getData());
  }

  @Test
  public void deserializeInteger() throws Exception {
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
        "    \"data\" : 1\n" +
        "}";

    CloudEvent cloudEvent = CloudEvent.deserialize(content.getBytes());
    assertEquals("application/json", cloudEvent.getDatacontenttype());
    assertEquals(1, cloudEvent.getData());
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

    CloudEvent cloudEvent = CloudEvent.deserialize(content.getBytes());
    assertEquals("text/plain", cloudEvent.getDatacontenttype());
    assertEquals("hello world", cloudEvent.getData());
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

    CloudEvent cloudEvent = CloudEvent.deserialize(content.getBytes());
    assertEquals("text/xml", cloudEvent.getDatacontenttype());
    assertEquals("<root/>", cloudEvent.getData());
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
    CloudEvent cloudEvent = CloudEvent.deserialize(content.getBytes());
    assertEquals("application/json", cloudEvent.getDatacontenttype());
    assertEquals("AQI=", cloudEvent.getData());
    assertArrayEquals(expected, OBJECT_MAPPER.convertValue(cloudEvent.getData(), byte[].class));
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
    CloudEvent cloudEvent = CloudEvent.deserialize(content.getBytes());
    assertEquals("application/octet-stream", cloudEvent.getDatacontenttype());
    assertNull(cloudEvent.getData());
    assertArrayEquals(expected, cloudEvent.getBinaryData());
  }
}
