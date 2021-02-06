/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.runtime;

import io.dapr.client.domain.CloudEvent;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Processes a given topic's message delivery.
 */
public interface TopicListener {

  /**
   * Processes a given topic's message delivery.
   * @param message Message event to be processed.
   * @param metadata Headers (or metadata).
   * @return Empty response.
   * @throws Exception Any exception from user code.
   */
  Mono<Void> process(CloudEvent message, Map<String, String> metadata) throws Exception;

}
