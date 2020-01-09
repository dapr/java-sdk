/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.runtime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
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

/**
 * Main interface to register and interface with local Dapr instance.
 *
 * This class DO NOT make I/O operations by itself, only via user-provided listeners.
 */
public final class Dapr implements DaprRuntime {

  /**
   * Empty response.
   */
  private static final Mono<byte[]> EMPTY_BYTES_ASYNC = Mono.just(new byte[0]);

  /**
   * Singleton instance for this class.
   */
  private static volatile DaprRuntime instance;

  /**
   * Serializes and deserializes internal objects.
   */
  private final ObjectSerializer serializer = new ObjectSerializer();

  /**
   * Topics, methods and binding handles.
   */
  private final Map<String, Function<Message, Mono<byte[]>>> handlers = Collections.synchronizedMap(new HashMap<>());

  /**
   * Private constructor to keep it singleton.
   */
  private Dapr() {
  }

  /**
   * Returns the only DaprRuntime object.
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<String> getSubscribedTopics() {
    return this.handlers
        .entrySet()
        .stream().filter(kv -> kv.getValue() instanceof TopicHandler)
        .map(kv -> kv.getKey())
        .collect(Collectors.toCollection(ArrayList::new));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void subscribeToTopic(String topic, TopicListener listener) {
    this.handlers.putIfAbsent(topic, new TopicHandler(listener));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerServiceMethod(String name, MethodListener listener) {
    this.handlers.putIfAbsent(name, new MethodHandler(listener));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<byte[]> handleInvocation(String name, byte[] payload, Map<String, String> metadata) {
    Function<Message, Mono<byte[]>> handler = this.handlers.get(name);

    if (handler == null) {
      return Mono.error(new DaprException("INVALID_METHOD_OR_TOPIC", "Did not find handler for : " + (name == null ? "" : name)));
    }

    try {
      Message message = this.serializer.deserialize(payload, Message.class);
      if (message == null) {
        return EMPTY_BYTES_ASYNC;
      }
      message.setMetadata(metadata);
      return handler.apply(message).switchIfEmpty(EMPTY_BYTES_ASYNC);
    } catch (Exception e) {
      // Handling exception in user code by propagating up via Mono.
      return Mono.error(e);
    }
  }

  /**
   * Internal class to encapsulate a request message.
   */
  public static final class Message {

    /**
     * Identifier of the message being processed.
     */
    private String id;

    /**
     * Type of the input payload.
     */
    private String datacontenttype;

    /**
     * Raw input payload.
     */
    private byte[] data;

    /**
     * Headers (or metadata).
     */
    private Map<String, String> metadata;

    /**
     * Instantiates a new input request (useful for JSON deserialization).
     */
    public Message() {
    }

    /**
     * Instantiates a new input request.
     * @param id Identifier of the message being processed.
     * @param datacontenttype Type of the input payload.
     * @param data Input body.
     * @param metadata Headers (or metadata) for the call.
     */
    public Message(String id, String datacontenttype, byte[] data, Map<String, String> metadata) {
      this.id = id;
      this.datacontenttype = datacontenttype;
      this.data = data;
      this.metadata = metadata == null ? null : Collections.unmodifiableMap(metadata);
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public byte[] getData() {
      return data;
    }

    public void setData(byte[] data) {
      this.data = data;
    }

    public String getDatacontenttype() {
      return datacontenttype;
    }

    public void setDatacontenttype(String datacontenttype) {
      this.datacontenttype = datacontenttype;
    }

    public Map<String, String> getMetadata() {
      return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
      this.metadata = metadata;
    }
  }

  /**
   * Internal class to handle a method call.
   */
  private static final class MethodHandler implements Function<Message, Mono<byte[]>> {

    /**
     * User-provided listener.
     */
    private final MethodListener listener;

    /**
     * Instantiates a new method handler.
     * @param listener Method to be executed on a given API call.
     */
    private MethodHandler(MethodListener listener) {
      this.listener = listener;
    }

    /**
     * Executes the listener's method.
     * @param r Internal request object.
     * @return Raw output payload or empty.
     */
    @Override
    public Mono<byte[]> apply(Message r) {
      return listener.process(r.data, r.metadata);
    }
  }

  /**
   * Internal class to handle a topic message delivery.
   */
  private static final class TopicHandler implements Function<Message, Mono<byte[]>> {

    /**
     * User-provided listener.
     */
    private final TopicListener listener;

    /**
     * Instantiates a new topic handler.
     * @param listener Callback to be executed on a given message.
     */
    private TopicHandler(TopicListener listener) {
      this.listener = listener;
    }

    /**
     * Executes the listener's method.
     * @param r Internal request object.
     * @return Always empty response.
     */
    @Override
    public Mono<byte[]> apply(Message r) {
      return listener.process(r.id, r.datacontenttype, r.data, r.metadata).then(Mono.empty());
    }
  }
}
