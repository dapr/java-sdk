/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.runtime;

import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;

public interface DaprRuntime {

  void subscribeToTopic(String topic, TopicListener listener);

  Collection<String> getSubscribedTopics();

  void registerServiceMethod(String name, MethodListener listener);

  Mono<byte[]> handleInvocation(String name, byte[] payload, Map<String, String> metadata);
}
