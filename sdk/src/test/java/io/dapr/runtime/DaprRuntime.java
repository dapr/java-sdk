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

import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Common interface to configure and process callback API.
 *
 * Used for Integration Tests only, for now.
 */
public interface DaprRuntime {

  /**
   * Subscribes to a topic.
   * @param topic Topic name to be subscribed to.
   * @param listener Callback to be executed on a given message.
   */
  void subscribeToTopic(String topic, TopicListener listener);

  /**
   * Serializes the list of subscribed topics as a String.
   * @return Serialized list of subscribed topics.
   * @throws IOException If cannot serialize.
   */
  String serializeSubscribedTopicList() throws IOException;

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
   * Registers a method to be executed for an input binding.
   * @param name The name of the input binding.
   * @param listener The method to run when receiving a message on this binding.
   */
  void registerInputBinding(String name, MethodListener listener);


  /**
   * Handles a given topic message or method API call.
   * @param name Name of topic or method.
   * @param payload Input body.
   * @param metadata Headers (or metadata) for the call.
   * @return Response payload or empty.
   */
  Mono<byte[]> handleInvocation(String name, byte[] payload, Map<String, String> metadata);


}
