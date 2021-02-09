/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
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
