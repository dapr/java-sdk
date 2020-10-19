/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.runtime;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientHttp;
import io.dapr.client.DaprClientTestBuilder;
import io.dapr.client.DaprHttpStub;
import io.dapr.client.domain.CloudEvent;
import io.dapr.client.domain.HttpExtension;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

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

  private final DaprRuntime daprRuntime = Dapr.getInstance();

  @Before
  public void setup() throws Exception {
    // Only for unit tests to simulate a new start of the app.
    Field field = this.daprRuntime.getClass().getDeclaredField("instance");
    field.setAccessible(true);
    field.set(null, null);
  }

  @Test
  public void pubSubHappyCase() throws Exception {
    Assert.assertNotNull(this.daprRuntime.getSubscribedTopics());
    Assert.assertTrue(this.daprRuntime.getSubscribedTopics().isEmpty());

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

    DaprHttpStub daprHttp = mock(DaprHttpStub.class);
    DaprClient client = DaprClientTestBuilder.buildHttpClient(daprHttp);
    DaprObjectSerializer serializer = new DefaultObjectSerializer();

    for (Message message : messages) {
      when(daprHttp.invokeApi(
          eq("POST"),
          eq(DaprClientHttp.PUBLISH_PATH + "/" + PUBSUB_NAME + "/" + TOPIC_NAME),
          any(),
          eq(serializer.serialize(message.data)),
          eq(null),
          any()))
          .thenAnswer(invocationOnMock -> this.daprRuntime.handleInvocation(
              TOPIC_NAME,
              this.serialize(message),
              message.metadata).then());

      client.publishEvent(PUBSUB_NAME, TOPIC_NAME, message.data).block();

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
    DaprClient client = DaprClientTestBuilder.buildHttpClient(daprHttp);

    DaprObjectSerializer serializer = new DefaultObjectSerializer();
    for (Message message : messages) {
      byte[] expectedResponse = serializer.serialize(message.id);
      when(listener.process(eq(serializer.serialize(message.data)), eq(message.metadata)))
          .then(x -> expectedResponse == null ? Mono.empty() : Mono.just(expectedResponse));

      when(daprHttp.invokeApi(
          eq("POST"),
          eq(DaprClientHttp.INVOKE_PATH + "/" + APP_ID + "/method/" + METHOD_NAME),
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
      Mono<byte[]> response = client.invokeService(APP_ID, METHOD_NAME, message.data, HttpExtension.POST,
          message.metadata, byte[].class);
      Assert.assertArrayEquals(expectedResponse, response.block());

      verify(listener, times(1))
          .process(eq(serializer.serialize(message.data)), eq(message.metadata));
    }

    verify(listener, times(messages.length)).process(any(), any());
  }

  @Test(expected = RuntimeException.class)
  public void subscribeCallbackException() throws Exception {
    Assert.assertNotNull(this.daprRuntime.getSubscribedTopics());
    Assert.assertTrue(this.daprRuntime.getSubscribedTopics().isEmpty());

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
    result.block();
  }

  @Test(expected = RuntimeException.class)
  public void subscribeUnknownTopic() throws Exception {
    Assert.assertNotNull(this.daprRuntime.getSubscribedTopics());
    Assert.assertTrue(this.daprRuntime.getSubscribedTopics().isEmpty());

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

    result.block();
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
