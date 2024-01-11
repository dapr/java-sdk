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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.domain.CloudEvent;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CloudEventTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static class MyClass {
    public int id;
    public String name;

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      MyClass myClass = (MyClass) o;

      if (id != myClass.id) return false;
      return Objects.equals(name, myClass.name);
    }

    @Override
    public int hashCode() {
      int result = id;
      result = 31 * result + (name != null ? name.hashCode() : 0);
      return result;
    }
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
        "    \"data\" : {\"id\": 1, \"name\": \"hello world\"},\n" +
        "    \"pubsubname\" : \"mypubsubname\",\n" +
        "    \"topic\" : \"mytopic\",\n" +
        "    \"traceid\" : \"Z987-0987-0987\",\n" +
        "    \"traceparent\" : \"Z987-0987-0987\",\n" +
        "    \"tracestate\" : \"\"\n" +
        "}";

    MyClass expected = new MyClass() {{
      this.id = 1;
      this.name = "hello world";
    }};

    CloudEvent cloudEvent = CloudEvent.deserialize(content.getBytes());
    assertEquals("1.0", cloudEvent.getSpecversion());
    assertEquals("com.github.pull_request.opened", cloudEvent.getType());
    assertEquals("https://github.com/cloudevents/spec/pull", cloudEvent.getSource());
    assertEquals("A234-1234-1234", cloudEvent.getId());
    assertEquals(OffsetDateTime.parse("2018-04-05T17:31:00Z"), cloudEvent.getTime());
    assertEquals("application/json", cloudEvent.getDatacontenttype());
    assertEquals("mypubsubname", cloudEvent.getPubsubName());
    assertEquals("mytopic", cloudEvent.getTopic());
    assertEquals("Z987-0987-0987", cloudEvent.getTraceId());
    assertEquals("Z987-0987-0987", cloudEvent.getTraceParent());
    assertEquals("", cloudEvent.getTraceState());
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

  @Test
  public void serializeObjectClass() throws Exception {
    CloudEvent<MyClass> cloudEvent = new CloudEvent<>();
    MyClass myClass = new MyClass();
    myClass.id = 1;
    myClass.name = "Hello World";
    cloudEvent.setData(myClass);
    OffsetDateTime now = OffsetDateTime.now();
    cloudEvent.setTime(now);

    String cloudEventAsString = OBJECT_MAPPER.writeValueAsString(cloudEvent);
    CloudEvent<MyClass> cloudEventDeserialized = OBJECT_MAPPER.readValue(cloudEventAsString,
        new TypeReference<CloudEvent<MyClass>>() {});
    assertEquals(cloudEvent, cloudEventDeserialized);
    assertEquals(now, cloudEventDeserialized.getTime());
    MyClass myClassDeserialized = cloudEventDeserialized.getData();
    assertEquals(myClass.id, myClassDeserialized.id);
    assertEquals(myClass.name, myClassDeserialized.name);
  }

  @Test
  public void equalsCodecovTest() {
    CloudEvent<?> cloudEvent = new CloudEvent<>();
    assertFalse(cloudEvent.equals(null));
    assertFalse(cloudEvent.equals(""));

    CloudEvent<?> cloudEventCopy = cloudEvent;
    assertEquals(cloudEvent, cloudEventCopy);

    CloudEvent<String> cloudEventDifferent = new CloudEvent<>();
    cloudEventDifferent.setId("id");
    assertNotEquals(cloudEventCopy, cloudEventDifferent);

    cloudEventDifferent = new CloudEvent<>();
    cloudEventDifferent.setSource("source");
    assertNotEquals(cloudEventCopy, cloudEventDifferent);

    cloudEventDifferent = new CloudEvent<>();
    cloudEventDifferent.setType("type");
    assertNotEquals(cloudEventCopy, cloudEventDifferent);

    cloudEventDifferent = new CloudEvent<>();
    cloudEventDifferent.setSpecversion("specversion");
    assertNotEquals(cloudEventCopy, cloudEventDifferent);

    cloudEventDifferent = new CloudEvent<>();
    cloudEventDifferent.setDatacontenttype("datacontenttype");
    assertNotEquals(cloudEventCopy, cloudEventDifferent);

    cloudEventDifferent = new CloudEvent<>();
    cloudEventDifferent.setData("data");
    assertNotEquals(cloudEventCopy, cloudEventDifferent);

    cloudEventDifferent = new CloudEvent<>();
    cloudEventDifferent.setBinaryData("binaryData".getBytes());
    assertNotEquals(cloudEventCopy, cloudEventDifferent);

    cloudEventDifferent = new CloudEvent<>();
    cloudEventDifferent.setPubsubName("pubsubName");
    assertNotEquals(cloudEventCopy, cloudEventDifferent);

    cloudEventDifferent = new CloudEvent<>();
    cloudEventDifferent.setTopic("topic");
    assertNotEquals(cloudEventCopy, cloudEventDifferent);

    OffsetDateTime now = OffsetDateTime.now();
    cloudEventDifferent = new CloudEvent<>();
    // cloudEvent null time, cloudEventDifferent now time
    cloudEventDifferent.setTime(now);
    assertNotEquals(cloudEventCopy, cloudEventDifferent);
    // cloudEvent now time, cloudEventDifferent now time
    cloudEvent.setTime(now);
    assertEquals(cloudEventCopy, cloudEventDifferent);
    // cloudEvent now time, cloudEventDifferent now time + 1 nano
    cloudEventDifferent.setTime(now.plusNanos(1L));
    assertNotEquals(cloudEventCopy, cloudEventDifferent);
    // reset cloudEvent time
    cloudEvent.setTime(null);

    cloudEventDifferent = new CloudEvent<>();
    cloudEventDifferent.setTraceId("traceId");
    assertNotEquals(cloudEventCopy, cloudEventDifferent);

    cloudEventDifferent = new CloudEvent<>();
    cloudEventDifferent.setTraceParent("traceParent");
    assertNotEquals(cloudEventCopy, cloudEventDifferent);

    cloudEventDifferent = new CloudEvent<>();
    cloudEventDifferent.setTraceState("traceState");
    assertNotEquals(cloudEventCopy, cloudEventDifferent);
  }

  @Test
  public void hashCodeCodecovTest() {
    CloudEvent<?> cloudEvent = new CloudEvent<>();
    final int EXPECTED_EMPTY_HASH_CODE = -505558625;
    assertEquals(EXPECTED_EMPTY_HASH_CODE, cloudEvent.hashCode());
  }
}
