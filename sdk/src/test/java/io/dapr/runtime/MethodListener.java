/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
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
   * @throws Exception Any exception from user code.
   */
  Mono<byte[]> process(byte[] data, Map<String, String> metadata) throws Exception;

}
