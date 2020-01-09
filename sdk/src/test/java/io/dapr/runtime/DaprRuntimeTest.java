/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.*;
import io.dapr.runtime.Dapr;
import io.dapr.runtime.DaprRuntime;
import io.dapr.runtime.TopicListener;
import io.dapr.utils.Constants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DaprRuntimeTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final String TYPE_PLAIN_TEXT = "plain/text";

  private static final String TOPIC_NAME = "mytopic";

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
    this.daprRuntime.subscribeToTopic(TOPIC_NAME, listener);

    verify(listener, never()).process(any(), any(), any(), any());

    Dapr.Message[] messages = new Dapr.Message[]{
      new Dapr.Message(
        generateMessageId(),
        TYPE_PLAIN_TEXT,
        generatePayload(),
        generateSingleMetadata()),
      new Dapr.Message(
        generateMessageId(),
        TYPE_PLAIN_TEXT,
        new byte[0],
        generateSingleMetadata()),
      new Dapr.Message(
        generateMessageId(),
        TYPE_PLAIN_TEXT,
        null,
        generateSingleMetadata()),
      new Dapr.Message(
        generateMessageId(),
        TYPE_PLAIN_TEXT,
        generatePayload(),
        null),
      new Dapr.Message(
        "",
        TYPE_PLAIN_TEXT,
        generatePayload(),
        generateSingleMetadata()),
      new Dapr.Message(
        null,
        TYPE_PLAIN_TEXT,
        generatePayload(),
        generateSingleMetadata()),
      new Dapr.Message(
        generateMessageId(),
        "",
        generatePayload(),
        generateSingleMetadata()),
      new Dapr.Message(
        generateMessageId(),
        null,
        generatePayload(),
        generateSingleMetadata())
    };

    DaprHttpStub daprHttp = mock(DaprHttpStub.class);
    DaprClient client = DaprClientTestBuilder.buildHttpClient(daprHttp);

    for (Dapr.Message message : messages) {
      when(daprHttp.invokeAPI(
              eq("POST"),
              eq(Constants.PUBLISH_PATH + "/" + TOPIC_NAME),
              eq(message.getData()),
              eq(null)))
              .thenAnswer(invocationOnMock -> {
                this.daprRuntime.handleInvocation(
                        TOPIC_NAME, OBJECT_MAPPER.writeValueAsBytes(message), message.getMetadata());
                return Mono.empty();
              });

      client.publishEvent(TOPIC_NAME, message.getData()).block();

      verify(listener, times(1))
        .process(eq(message.getId()), eq(message.getDatacontenttype()), eq(message.getData()), eq(message.getMetadata()));
    }

    verify(listener, times(messages.length)).process(any(), any(), any(), any());
  }

  @Test(expected = RuntimeException.class)
  public void subscribeCallbackException() throws Exception {
    Assert.assertNotNull(this.daprRuntime.getSubscribedTopics());
    Assert.assertTrue(this.daprRuntime.getSubscribedTopics().isEmpty());

    TopicListener listener = mock(TopicListener.class);
    when(listener.process(any(), any(), any(), any()))
            .thenReturn(Mono.error(new RuntimeException()));

    this.daprRuntime.subscribeToTopic(TOPIC_NAME, listener);

    Dapr.Message message = new Dapr.Message(
            generateMessageId(),
            TYPE_PLAIN_TEXT,
            generatePayload(),
            generateSingleMetadata());

    Mono<byte[]> result = this.daprRuntime
            .handleInvocation(TOPIC_NAME, OBJECT_MAPPER.writeValueAsBytes(message), message.getMetadata());

    verify(listener, times(1))
            .process(
                    eq(message.getId()),
                    eq(message.getDatacontenttype()),
                    eq(message.getData()),
                    eq(message.getMetadata()));

    result.block();
  }

  @Test(expected = RuntimeException.class)
  public void subscribeUnknownTopic() throws Exception {
    Assert.assertNotNull(this.daprRuntime.getSubscribedTopics());
    Assert.assertTrue(this.daprRuntime.getSubscribedTopics().isEmpty());

    TopicListener listener = mock(TopicListener.class);

    this.daprRuntime.subscribeToTopic(TOPIC_NAME, listener);

    Dapr.Message message = new Dapr.Message(
            generateMessageId(),
            TYPE_PLAIN_TEXT,
            generatePayload(),
            generateSingleMetadata());

    Mono<byte[]> result = this.daprRuntime
            .handleInvocation("UNKNOWN", OBJECT_MAPPER.writeValueAsBytes(message), message.getMetadata());

    verify(listener, never())
            .process(
                    eq(message.getId()),
                    eq(message.getDatacontenttype()),
                    eq(message.getData()),
                    eq(message.getMetadata()));

    result.block();
  }

  private static final String generateMessageId() {
    return UUID.randomUUID().toString();
  }

  private static final byte[] generatePayload() {
    return UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
  }

  private static final Map<String, String> generateSingleMetadata() {
    return Collections.singletonMap(UUID.randomUUID().toString(), UUID.randomUUID().toString());
  }
}
