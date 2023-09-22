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

package io.dapr.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.domain.CloudEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class DefaultContentTypeConverterTest {

  // same as default serializer config
  private static final ObjectMapper MAPPER = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .setSerializationInclusion(JsonInclude.Include.NON_NULL);

  @Test
  public void testToBytesHttpStringEventCorrectContentType() throws IOException {
    String event = "string event";
    byte[] res = DefaultContentTypeConverter.convertEventToBytesForHttp(event, "text/plain");
    Assertions.assertNotNull(res, "expected correct byte array response");
    byte[] expected = event.getBytes(StandardCharsets.UTF_8);
    Assertions.assertArrayEquals(expected, res, "expected response to be matched with expectation");
  }

  @Test
  public void testToBytesHttpNumberEventCorrectContentType() throws IOException {
    Number event = 123;
    byte[] res = DefaultContentTypeConverter.convertEventToBytesForHttp(event, "text/plain");
    Assertions.assertNotNull( res, "expected correct byte array response");
    byte[] expected = "123".getBytes(StandardCharsets.UTF_8);
    Assertions.assertArrayEquals( expected, res, "expected response to be matched with expectation");
  }

  @Test
  public void testToBytesHttpBinEventCorrectContentType() throws IOException {
    String event = "string event";
    byte[] data = event.getBytes(StandardCharsets.UTF_8);
    byte[] res = DefaultContentTypeConverter.convertEventToBytesForHttp(data, "application/octet-stream");
    Assertions.assertNotNull(res, "expected correct byte array response");
    byte[] expected = Base64.getEncoder().encode(data);
    Assertions.assertArrayEquals(expected, res, "expected response to be matched with expectation");
  }

  @Test
  public void testToBytesHttpBinEventInCorrectContentType() throws IOException {
    String event = "string event";
    assertThrows(IllegalArgumentException.class, () ->
    DefaultContentTypeConverter.convertEventToBytesForHttp(event, "application/octet-stream"));
  }

  @Test
  public void testToBytesHttpJsonEventCorrectContentType() throws IOException {
    Map<String, String> event = new HashMap<String, String>() {{
      put("test1", "val1");
      put("test2", "val2");
    }};
    byte[] res = DefaultContentTypeConverter.convertEventToBytesForHttp(event, "application/json");
    Assertions.assertNotNull(res, "expected correct byte array response");
    byte[] expected = MAPPER.writeValueAsBytes(event);
    Assertions.assertArrayEquals(expected, res, "expected response to be matched with expectation");
  }

  @Test
  public void testToBytesHttpJsonEventInCorrectContentType() throws IOException {
    Map<String, String> event = new HashMap<String, String>() {{
      put("test1", "val1");
      put("test2", "val2");
    }};

    assertThrows(IllegalArgumentException.class, () -> DefaultContentTypeConverter.convertEventToBytesForHttp(event, "application/xml"));
  }

  @Test
  public void testToBytesHttpCloudEventCorrectContentType() throws IOException {
    // Make sure that the MAPPER is configured same as the DefaultObjectSerializer config
    CloudEvent<String> event = new CloudEvent<>();
    event.setType("test");
    event.setId("id 1");
    event.setSpecversion("v1");
    event.setData("test data");
    event.setDatacontenttype("text/plain");
    event.setSource("dapr test");
    byte[] res = DefaultContentTypeConverter.convertEventToBytesForHttp(event, "application/cloudevents+json");
    Assertions.assertNotNull(res, "expected correct byte array response");
    byte[] expected = MAPPER.writeValueAsBytes(event);
    Assertions.assertArrayEquals(expected, res, "expected response to be matched with expectation");
  }

  @Test
  public void testToBytesHttpCloudEventInCorrectContentType() throws IOException {
    // Make sure that the MAPPER is configured same as the DefaultObjectSerializer config
    CloudEvent<String> event = new CloudEvent<>();
    event.setType("test");
    event.setId("id 1");
    event.setSpecversion("v1");
    event.setData("test data");
    event.setDatacontenttype("text/plain");
    event.setSource("dapr test");
    assertThrows(IllegalArgumentException.class, () ->     DefaultContentTypeConverter.convertEventToBytesForHttp(event, "image/png"));
  }

  @Test
  public void testToBytesGrpcBinEventCorrectContentType() throws IOException {
    byte[] event = "test event".getBytes(StandardCharsets.UTF_8);
    byte[] res = DefaultContentTypeConverter.convertEventToBytesForGrpc(event, "application/octet-stream");
    Assertions.assertNotNull(res, "expected correct byte array response");
    Assertions.assertArrayEquals( event, res, "expected response to be matched with expectation");
  }

  @Test
  public void testToBytesGrpcBinEventInCorrectContentType() throws IOException {
    byte[] event = "test event".getBytes(StandardCharsets.UTF_8);
    assertThrows(IllegalArgumentException.class, () -> DefaultContentTypeConverter.convertEventToBytesForGrpc(event, "application/xml"));
  }

  @Test
  public void testToBytesGrpcStringEventCorrectContentType() throws IOException {
    String event = "string event";
    byte[] res = DefaultContentTypeConverter.convertEventToBytesForGrpc(event, "text/plain");
    Assertions.assertNotNull(res, "expected correct byte array response");
    byte[] expected = event.getBytes(StandardCharsets.UTF_8);
    Assertions.assertArrayEquals(expected, res, "expected response to be matched with expectation");
  }

  @Test
  public void testToEventHttpStringDataCorrectContentType() throws IOException {
    byte[] event = "string event".getBytes(StandardCharsets.UTF_8);
    String res = DefaultContentTypeConverter.convertBytesToEventFromHttp(event,
        "text/plain", TypeRef.STRING);
    Assertions.assertNotNull( res, "expected not null response");
    Assertions.assertEquals("string event", res, "expected res to match expectation");
  }

  @Test
  public void testToEventHttpBinDataCorrectContentType() throws IOException {
    byte[] expected = "string event".getBytes(StandardCharsets.UTF_8);
    byte[] event = Base64.getEncoder().encode(expected);
    byte[] res = DefaultContentTypeConverter.convertBytesToEventFromHttp(event,
        "application/octet-stream", TypeRef.BYTE_ARRAY);
    Assertions.assertNotNull(res, "expected not null response");
    Assertions.assertArrayEquals(expected, res, "expected res to match expectation");
  }

  @Test
  public void testToEventGrpcBinDataCorrectContentType() throws IOException {
    byte[] expected = "string event".getBytes(StandardCharsets.UTF_8);
    byte[] res = DefaultContentTypeConverter.convertBytesToEventFromGrpc(expected,
        "application/octet-stream", TypeRef.BYTE_ARRAY);
    Assertions.assertNotNull(res, "expected not null response");
    Assertions.assertArrayEquals(expected, res, "expected res to match expectation");
  }

  @Test
  public void testToBytesGrpcBinDataInCorrectContentType() throws IOException {
    String event = "string event";
    assertThrows(IllegalArgumentException.class, () -> DefaultContentTypeConverter.convertEventToBytesForGrpc(event,
        "application/octet-stream"));
  }

  @Test
  public void testToEventGrpcStringDataCorrectContentType() throws IOException {
    byte[] expected = "string event".getBytes(StandardCharsets.UTF_8);
    String res = DefaultContentTypeConverter.convertBytesToEventFromGrpc(expected,
        "text/plain", TypeRef.STRING);
    Assertions.assertNotNull( res, "expected not null response");
    Assertions.assertEquals("string event", res, "expected res to match expectation");
  }


  @Test
  public void testToEventHttpPrimitiveDataCorrectContentType() throws IOException {
    Number expected = 123;
    byte[] data = DefaultContentTypeConverter.convertEventToBytesForHttp(expected, "text/plain");
    Integer res = DefaultContentTypeConverter.convertBytesToEventFromHttp(data,
        "text/plain", TypeRef.INT);
    Assertions.assertNotNull(res, "expected not null response");
    Assertions.assertEquals(expected, res, "expected res to match expectation");
  }

  @Test
  public void testToEventHttpCEDataCorrectContentType() throws IOException {
    CloudEvent<String> event = new CloudEvent<>();
    event.setType("test");
    event.setId("id 1");
    event.setSpecversion("v1");
    event.setData("test data");
    event.setDatacontenttype("text/plain");
    event.setSource("dapr test");
    byte[] data = DefaultContentTypeConverter.convertEventToBytesForHttp(event, "application/cloudevents+json");
    CloudEvent<String> res = DefaultContentTypeConverter.convertBytesToEventFromHttp(data,
        "application/cloudevents+json", new TypeRef<CloudEvent<String>>() {
        });
    Assertions.assertNotNull(res,"expected not null response");
    Assertions.assertEquals(event, res, "expected res to match expectation");
  }

  @Test
  public void testToEventHttpBinDataInCorrectContentType() throws IOException {
    byte[] data = "string event".getBytes(StandardCharsets.UTF_8);
    byte[] event = Base64.getEncoder().encode(data);
    assertThrows(IllegalArgumentException.class, () -> DefaultContentTypeConverter.convertBytesToEventFromHttp(event,
        "text/plain", TypeRef.BYTE_ARRAY));
  }

  @Test
  public void testToEventHttpBinDataNullCorrectContentType() throws IOException {
    byte[] data = "string event".getBytes(StandardCharsets.UTF_8);
    byte[] event = Base64.getEncoder().encode(data);
    assertThrows(IllegalArgumentException.class, () -> DefaultContentTypeConverter.convertBytesToEventFromHttp(event,
        null, TypeRef.BYTE_ARRAY));
  }

  @Test
  public void testToEventHttpBinDataCharsetInCorrectContentType() throws IOException {
    byte[] data = "string event".getBytes(StandardCharsets.UTF_8);
    byte[] event = Base64.getEncoder().encode(data);
    assertThrows(IllegalArgumentException.class, () -> DefaultContentTypeConverter.convertBytesToEventFromHttp(event,
        "text/plain;charset=utf-8", TypeRef.BYTE_ARRAY));
  }
}
