/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.domain.CloudEvent;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Main interface to register and interface with local Dapr instance.
 *
 * This class DO NOT make I/O operations by itself, only via user-provided listeners.
 */
public final class Dapr implements DaprRuntime {

  /**
   * Serializes and deserializes internal objects.
   */
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * Singleton instance for this class.
   */
  private static volatile DaprRuntime instance;

  /**
   * Topics, methods and binding handles.
   */
  private final Map<String, Function<HandleRequest, Mono<byte[]>>> handlers = Collections.synchronizedMap(new HashMap<>());

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
  public String serializeSubscribedTopicList() throws IOException {
    return OBJECT_MAPPER.writeValueAsString(this.getSubscribedTopics());
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
  public void registerInputBinding(String name, MethodListener listener) {
    this.handlers.putIfAbsent(name, new MethodHandler(listener));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<byte[]> handleInvocation(String name, byte[] payload, Map<String, String> metadata) {
    if (name == null) {
      return Mono.error(new IllegalArgumentException("Handler's name cannot be null."));
    }

    Function<HandleRequest, Mono<byte[]>> handler = this.handlers.get(name);

    if (handler == null) {
      return Mono.error(new IllegalArgumentException("Did not find handler for : " + (name == null ? "" : name)));
    }

    try {
      HandleRequest request = new HandleRequest(name, payload, metadata);
      return  handler.apply(request);
    } catch (Exception e) {
      // Handling exception in user code by propagating up via Mono.
      return Mono.error(e);
    }
  }

  /**
   * Class used to pass-through the handler input.
   */
  private static final class HandleRequest {

    /**
     * Name of topic/method/binding being handled.
     */
    private final String name;

    /**
     * Payload received.
     */
    private final byte[] payload;

    /**
     * Metadata received.
     */
    private final Map<String, String> metadata;

    /**
     * Instantiates a new handle request.
     * @param name Name of topic/method/binding being handled.
     * @param payload Payload received.
     * @param metadata Metadata received.
     */
    private HandleRequest(String name, byte[] payload, Map<String, String> metadata) {
      this.name = name;
      this.payload = payload;
      this.metadata = metadata == null ? null : Collections.unmodifiableMap(metadata);
    }
  }

  /**
   * Internal class to handle a method call.
   */
  private static final class MethodHandler implements Function<HandleRequest, Mono<byte[]>> {

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
    public Mono<byte[]> apply(HandleRequest r) {
      try {
        return listener.process(r.payload, r.metadata);
      } catch (Exception e) {
        return Mono.error(e);
      }
    }
  }

  /**
   * Internal class to handle a topic message delivery.
   */
  private static final class TopicHandler implements Function<HandleRequest, Mono<byte[]>> {

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
    public Mono<byte[]> apply(HandleRequest r) {
      try {
        CloudEvent message = CloudEvent.deserialize(r.payload);
        if (message == null) {
          return Mono.empty();
        }

        return listener.process(message, r.metadata).then(Mono.empty());
      } catch (Exception e) {
        return Mono.error(e);
      }
    }
  }
}
