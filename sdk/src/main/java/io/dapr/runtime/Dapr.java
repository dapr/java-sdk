/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.runtime;

import io.dapr.client.domain.CloudEventEnvelope;
import io.dapr.exceptions.DaprException;
import io.dapr.utils.ObjectSerializer;
import reactor.core.publisher.Mono;

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
    this.handlers.putIfAbsent(topic, new TopicHandler(this.serializer, listener));
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
    Function<HandleRequest, Mono<byte[]>> handler = this.handlers.get(name);

    if (handler == null) {
      return Mono.error(new DaprException("INVALID_METHOD_OR_TOPIC", "Did not find handler for : " + (name == null ? "" : name)));
    }

    try {
      HandleRequest request = new HandleRequest(name, payload, metadata);
      Mono<byte[]> response = handler.apply(request);
      return response;
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
     * Serializer/deserializer.
     */
    private final ObjectSerializer serializer;

    /**
     * Instantiates a new topic handler.
     * @param serializer Useful for object serialization.
     * @param listener Callback to be executed on a given message.
     */
    private TopicHandler(ObjectSerializer serializer, TopicListener listener) {
      this.serializer = serializer;
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
        CloudEventEnvelope message = this.serializer.deserialize(r.payload, CloudEventEnvelope.class);
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
