/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.runtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.exceptions.DaprException;
import io.dapr.utils.ObjectSerializer;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Dapr implements DaprRuntime {

  /**
   * Shared Json serializer/deserializer as per Jackson's documentation.
   */
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final Mono<byte[]> EMPTY_BYTES_ASYNC = Mono.just(new byte[0]);

  private static final byte[] EMPTY_BYTES = new byte[0];

  private static volatile DaprRuntime instance;

  private final ObjectSerializer serializer = new ObjectSerializer();

  private final Map<String, Function<HandleRequest, Mono<byte[]>>> handlers = Collections.synchronizedMap(new HashMap<>());

  private Dapr() {
  }

  /**
   * Returns an DaprRuntime object.
   *
   * @return DaprRuntime object.
   */
  public static DaprRuntime getInstance() {
    if (instance == null) {
      synchronized (Dapr.class) {
        if (instance == null) {
          instance = new Dapr();
        }
      }
    }

    return instance;
  }

  @Override
  public Collection<String> getSubscribedTopics() {
    return this.handlers
        .entrySet()
        .stream().filter(kv -> kv.getValue() instanceof TopicHandler)
        .map(kv -> kv.getKey())
        .collect(Collectors.toCollection(ArrayList::new));
  }

  @Override
  public void subscribeToTopic(String topic, TopicListener listener) {
    this.handlers.putIfAbsent(topic, new TopicHandler(listener));
  }

  @Override
  public void registerServiceMethod(String name, MethodListener listener) {
    this.handlers.putIfAbsent(name, new MethodHandler(listener));
  }

  @Override
  public Mono<byte[]> handleInvocation(String name, byte[] payload, Map<String, String> metadata) {
    Function<HandleRequest, Mono<byte[]>> handler = this.handlers.get(name);

    if (handler == null) {
      return Mono.error(new DaprException("INVALID_METHOD_OR_TOPIC", "Did not find handler for : " + (name == null ? "" : name)));
    }

    try {
      Map<String, Object> map = parse(payload);
      String messageId = map.getOrDefault("id", "").toString();
      String dataType = map.getOrDefault("datacontenttype", "").toString();
      byte[] data = this.serializer.deserialize(map.get("data"), byte[].class);
      return handler.apply(new HandleRequest(messageId, dataType, data, metadata)).switchIfEmpty(EMPTY_BYTES_ASYNC);
    } catch (Exception e) {
      // Handling exception in user code by propagating up via Mono.
      return Mono.error(e);
    }
  }

  private static final class HandleRequest {

    private final String messageId;

    private final String dataType;

    private final byte[] payload;

    private final Map<String, String> metadata;

    public HandleRequest(String messageId, String dataType, byte[] payload, Map<String, String> metadata) {
      this.messageId = messageId;
      this.dataType = dataType;
      this.payload = payload;
      this.metadata = Collections.unmodifiableMap(metadata);
    }
  }

  private static final class MethodHandler implements Function<HandleRequest, Mono<byte[]>> {

    private final MethodListener listener;

    private MethodHandler(MethodListener listener) {
      this.listener = listener;
    }

    @Override
    public Mono<byte[]> apply(HandleRequest r) {
      return listener.process(r.payload, r.metadata);
    }
  }

  private static final class TopicHandler implements Function<HandleRequest, Mono<byte[]>> {

    private final TopicListener listener;

    private TopicHandler(TopicListener listener) {
      this.listener = listener;
    }

    @Override
    public Mono<byte[]> apply(HandleRequest r) {
      return listener.process(r.messageId, r.dataType, r.payload, r.metadata).then(Mono.empty());
    }
  }

  public Map<String, Object> parse(final byte[] response) throws IOException {
    if (response == null) {
      return new HashMap<String, Object>();
    }

    return OBJECT_MAPPER.readValue(response, new TypeReference<Map<String, Object>>() {});
  }
}
