/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.runtime;

import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Processes a given topic's message delivery.
 */
public interface TopicListener {

  /**
   * Processes a given topic's message delivery.
   * @param messageId Message's identifier.
   * @param dataType Type of the input data.
   * @param data Input data.
   * @param metadata Headers (or metadata).
   * @return Empty response.
   */
  Mono<Void> process(String messageId, String dataType, byte[] data, Map<String, String> metadata);

}
