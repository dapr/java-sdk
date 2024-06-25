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

package io.dapr.runtime;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientTestBuilder;
import io.dapr.client.DaprHttp;
import io.dapr.client.DaprHttpStub;
import io.dapr.client.domain.CloudEvent;
import io.dapr.client.domain.HttpExtension;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DaprRuntimeTest {

  protected static final JsonFactory JSON_FACTORY = new JsonFactory();

  private static final String TYPE_PLAIN_TEXT = "plain/text";

  private static final String PUBSUB_NAME = "mypubsubname";

  private static final String TOPIC_NAME = "mytopic";

  private static final String APP_ID = "myappid";

  private static final String METHOD_NAME = "mymethod";

  private static final String INVOKE_PATH = DaprHttp.API_VERSION + "/invoke";

  private static final String PUBLISH_PATH = DaprHttp.API_VERSION + "/publish";

  private final DaprRuntime daprRuntime = Dapr.getInstance();

  @BeforeEach
  public void setup() throws Exception {
    // Only for unit tests to simulate a new start of the app.
    Field field = this.daprRuntime.getClass().getDeclaredField("instance");
    field.setAccessible(true);
    field.set(null, null);
  }

  @Test
  public void pubSubHappyCase() throws Exception {
    Assertions.assertNotNull(this.daprRuntime.getSubscribedTopics());
    Assertions.assertTrue(this.daprRuntime.getSubscribedTopics().isEmpty());

    TopicListener listener = mock(TopicListener.class);
    when(listener.process(any(), any())).thenReturn(Mono.empty());

    this.daprRuntime.subscribeToTopic(TOPIC_NAME, listener);

    verify(listener, never()).process(any(), any());

    Message[] messages = new Message[]{
        new Message(
            generateMessageId(),
            TYPE_PLAIN_TEXT,
            generatePayload(),
            generateSingleMetadata()),
        new Message(
            generateMessageId(),
            TYPE_PLAIN_TEXT,
            "",
            generateSingleMetadata()),
        new Message(
            generateMessageId(),
            TYPE_PLAIN_TEXT,
            null,
            generateSingleMetadata()),
        new Message(
            generateMessageId(),
            TYPE_PLAIN_TEXT,
            generatePayload(),
            null),
        new Message(
            "",
            TYPE_PLAIN_TEXT,
            generatePayload(),
            generateSingleMetadata()),
        new Message(
            null,
            TYPE_PLAIN_TEXT,
            generatePayload(),
            generateSingleMetadata()),
        new Message(
            generateMessageId(),
            "",
            generatePayload(),
            generateSingleMetadata()),
        new Message(
            generateMessageId(),
            null,
            generatePayload(),
            generateSingleMetadata())
    };

    for (Message message : messages) {
      this.daprRuntime.handleInvocation(TOPIC_NAME, this.serialize(message), message.metadata);

      CloudEvent envelope = new CloudEvent(
        message.id,
        null,
        null,
        null,
        message.datacontenttype,
        message.data
      );
      verify(listener, times(1)).process(eq(envelope), eq(message.metadata));
    }

    verify(listener, times(messages.length)).process(any(), any());
  }

  @Test
  public void invokeHappyCase() throws Exception {
    MethodListener listener = mock(MethodListener.class);

    this.daprRuntime.registerServiceMethod(METHOD_NAME, listener);

    verify(listener, never()).process(any(), any());

    Message[] messages = new Message[]{
        new Message(
            generateMessageId(),
            TYPE_PLAIN_TEXT,
            generatePayload(),
            generateSingleMetadata()),
        new Message(
            generateMessageId(),
            TYPE_PLAIN_TEXT,
            "",
            generateSingleMetadata()),
        new Message(
            generateMessageId(),
            TYPE_PLAIN_TEXT,
            null,
            generateSingleMetadata()),
        new Message(
            generateMessageId(),
            TYPE_PLAIN_TEXT,
            generatePayload(),
            null),
        new Message(
            "",
            TYPE_PLAIN_TEXT,
            generatePayload(),
            generateSingleMetadata()),
        new Message(
            null,
            TYPE_PLAIN_TEXT,
            generatePayload(),
            generateSingleMetadata()),
        new Message(
            generateMessageId(),
            "",
            generatePayload(),
            generateSingleMetadata()),
        new Message(
            generateMessageId(),
            null,
            generatePayload(),
            generateSingleMetadata())
    };

    DaprHttpStub daprHttp = mock(DaprHttpStub.class);
    DaprClient client = DaprClientTestBuilder.buildClientForHttpOnly(daprHttp);

    DaprObjectSerializer serializer = new DefaultObjectSerializer();
    for (Message message : messages) {
      byte[] expectedResponse = serializer.serialize(message.id);
      when(listener.process(eq(serializer.serialize(message.data)), eq(message.metadata)))
          .then(x -> expectedResponse == null ? Mono.empty() : Mono.just(expectedResponse));

      when(daprHttp.invokeApi(
          eq("POST"),
          eq((INVOKE_PATH + "/" + APP_ID + "/method/" + METHOD_NAME).split("/")),
          any(),
          eq(serializer.serialize(message.data)),
          any(),
          any()))
          .thenAnswer(x ->
              this.daprRuntime.handleInvocation(
              METHOD_NAME,
              serializer.serialize(message.data),
              message.metadata)
          .map(r -> new DaprHttpStub.ResponseStub(r, null, 200)));
      Mono<byte[]> response = client.invokeMethod(APP_ID, METHOD_NAME, message.data, HttpExtension.POST,
          message.metadata, byte[].class);
      Assertions.assertArrayEquals(expectedResponse, response.block());

      verify(listener, times(1))
          .process(eq(serializer.serialize(message.data)), eq(message.metadata));
    }

    verify(listener, times(messages.length)).process(any(), any());
  }

  @Test
  public void subscribeCallbackException() throws Exception {
    Assertions.assertNotNull(this.daprRuntime.getSubscribedTopics());
    Assertions.assertTrue(this.daprRuntime.getSubscribedTopics().isEmpty());

    TopicListener listener = mock(TopicListener.class);
    when(listener.process(any(), any()))
            .thenReturn(Mono.error(new RuntimeException()));

    this.daprRuntime.subscribeToTopic(TOPIC_NAME, listener);

    Message message = new Message(
            generateMessageId(),
            TYPE_PLAIN_TEXT,
            generatePayload(),
            generateSingleMetadata());

    Mono<byte[]> result = this.daprRuntime
            .handleInvocation(TOPIC_NAME, this.serialize(message), message.metadata);

    CloudEvent envelope = new CloudEvent(
      message.id,
      null,
      null,
      null,
      message.datacontenttype,
      message.data
    );
    verify(listener, times(1)).process(eq(envelope), eq(message.metadata));

    assertThrows(RuntimeException.class, () -> result.block());
  }

  @Test
  public void subscribeUnknownTopic() throws Exception {
    Assertions.assertNotNull(this.daprRuntime.getSubscribedTopics());
    Assertions.assertTrue(this.daprRuntime.getSubscribedTopics().isEmpty());

    TopicListener listener = mock(TopicListener.class);

    this.daprRuntime.subscribeToTopic(TOPIC_NAME, listener);

    Message message = new Message(
            generateMessageId(),
            TYPE_PLAIN_TEXT,
            generatePayload(),
            generateSingleMetadata());

    Mono<byte[]> result = this.daprRuntime
            .handleInvocation("UNKNOWN", serialize(message), message.metadata);

    verify(listener, never()).process(any(), any());

    assertThrows(IllegalArgumentException.class, () -> result.block());
  }

  private static String generateMessageId() {
    return UUID.randomUUID().toString();
  }

  private static String generatePayload() {
    return UUID.randomUUID().toString();
  }

  private static Map<String, String> generateSingleMetadata() {
    return Collections.singletonMap(UUID.randomUUID().toString(), UUID.randomUUID().toString());
  }

  private static final class Message {

    private final String id;

    private final String datacontenttype;

    private final String data;

    private final Map<String, String> metadata;

    private Message(String id, String datacontenttype, String data, Map<String, String> metadata) {
      this.id = id;
      this.datacontenttype = datacontenttype;
      this.data = data;
      this.metadata = metadata;
    }
  }

  private byte[] serialize(Message message) throws IOException {
    if (message == null) {
      return null;
    }

    try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      JsonGenerator generator = JSON_FACTORY.createGenerator(bos);
      generator.writeStartObject();
      if (message.id != null) {
        generator.writeStringField("id", message.id);
      }
      if (message.datacontenttype != null) {
        generator.writeStringField("datacontenttype", message.datacontenttype);
      }
      if (message.data != null) {
        generator.writeStringField("data", message.data);
      }
      generator.writeEndObject();
      generator.close();
      bos.flush();
      return bos.toByteArray();
    }
  }
}
