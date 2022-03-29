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
