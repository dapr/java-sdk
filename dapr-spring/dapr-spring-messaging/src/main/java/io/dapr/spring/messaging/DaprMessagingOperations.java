/*
 * Copyright 2024 The Dapr Authors
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

package io.dapr.spring.messaging;

import reactor.core.publisher.Mono;

public interface DaprMessagingOperations<T> {

  /**
   * Sends a message to the specified topic in a blocking manner.
   *
   * @param topic   the topic to send the message to or {@code null} to send to the
   *                default topic
   * @param message the message to send
   */
  void send(String topic, T message);

  /**
   * Create a {@link SendMessageBuilder builder} for configuring and sending a message.
   *
   * @param message the payload of the message
   * @return the builder to configure and send the message
   */
  SendMessageBuilder<T> newMessage(T message);

  /**
   * Builder that can be used to configure and send a message. Provides more options
   * than the basic send/sendAsync methods provided by {@link DaprMessagingOperations}.
   *
   * @param <T> the message payload type
   */
  interface SendMessageBuilder<T> {

    /**
     * Specify the topic to send the message to.
     *
     * @param topic the destination topic
     * @return the current builder with the destination topic specified
     */
    SendMessageBuilder<T> withTopic(String topic);

    /**
     * Send the message in a blocking manner using the configured specification.
     */
    void send();

    /**
     * Uses the configured specification to send the message in a non-blocking manner.
     *
     * @return a Mono that completes when the message has been sent
     */
    Mono<Void> sendAsync();
  }

}
