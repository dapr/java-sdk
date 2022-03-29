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
