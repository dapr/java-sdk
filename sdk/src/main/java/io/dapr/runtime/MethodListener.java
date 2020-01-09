/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.runtime;

import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Processes a given API's method call.
 */
public interface MethodListener {

  /**
   * Processes a given API's method call.
   * @param data Raw input payload.
   * @param metadata Header (or metadata).
   * @return Raw response or empty.
   */
  Mono<byte[]> process(byte[] data, Map<String, String> metadata);

}
