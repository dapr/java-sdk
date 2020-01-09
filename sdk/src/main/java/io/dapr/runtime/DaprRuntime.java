/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.runtime;

import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;

/**
 * Common interface to configure and process callback API.
 */
public interface DaprRuntime {

  /**
   * Subscribes to a topic.
   * @param topic Topic name to be subscribed to.
   * @param listener Callback to be executed on a given message.
   */
  void subscribeToTopic(String topic, TopicListener listener);

  /**
   * Returns the collection of subscribed topics.
   * @return Collection of subscribed topics.
   */
  Collection<String> getSubscribedTopics();

  /**
   * Registers a service method to be executed on an API call.
   * @param name Name of the service API's method.
   * @param listener Method to be executed on a given API call.
   */
  void registerServiceMethod(String name, MethodListener listener);

  /**
   * Handles a given topic message or method API call.
   * @param name Name of topic or method.
   * @param payload Input body.
   * @param metadata Headers (or metadata) for the call.
   * @return Response payload or empty.
   */
  Mono<byte[]> handleInvocation(String name, byte[] payload, Map<String, String> metadata);
}
