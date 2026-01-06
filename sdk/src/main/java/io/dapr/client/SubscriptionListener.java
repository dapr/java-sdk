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

package io.dapr.client;

import io.dapr.client.domain.CloudEvent;
import reactor.core.publisher.Mono;

/**
 * Callback interface to receive events from a streaming subscription of events.
 * @param <T> Object type for deserialization.
 */
@Deprecated
public interface SubscriptionListener<T> {

  /**
   * Callback status response for acknowledging a message.
   */
  enum Status {
    SUCCESS,
    RETRY,
    DROP
  }

  /**
   * Processes an event from streaming subscription.
   * @param event Event received.
   * @return Acknowledgement status.
   */
  Mono<Status> onEvent(CloudEvent<T> event);

  /**
   * Processes an exception during streaming subscription.
   * @param exception Exception to be processed.
   */
  void onError(RuntimeException exception);
}
